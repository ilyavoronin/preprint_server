package com.preprint.server.ref

import com.preprint.server.data.Reference
import com.preprint.server.ref.custom.*
import org.apache.logging.log4j.kotlin.logger
import org.apache.pdfbox.pdmodel.PDDocument
import kotlin.math.roundToInt

object CustomReferenceExtractor : ReferenceExtractor {

    private val logger = logger()

    override fun extractUnverifiedReferences(pdf : ByteArray) : List <Reference> {
        logger.info("Begin reference extraction")

        val doc = PDDocument.load(pdf)
        val textWithMarks = synchronized(PDFRefTextStripper) {
             PDFRefTextStripper.getMarkedText(doc)
        }
        val pageWidth = doc.pages[0].mediaBox.width.toDouble()
        val isTwoColumns = PDFRefTextStripper.isTwoColumns

        var lines = getLines(textWithMarks)
        lines = removeEmptyLines(lines)
        lines = removeLeadingSpaces(lines)
        while (true) {
            val oldSize = lines.size
            lines = GarbageDeleter.removePageNumbers(lines)
            lines = GarbageDeleter.removePageHeaders(lines)
            if (lines.size == oldSize) {
                break
            }
        }
        val ind = findRefBegin(lines)
        if (ind == -1) {
            return listOf()
        }
        lines = lines.drop(ind)
        //remove pageStart and page end marks
        lines = lines.filter {line -> line.indent >= 0}
        var refList = Reference.toReferences(
            parseReferences(
                lines,
                isTwoColumns,
                pageWidth.roundToInt()
            ).map {it.trimIndent()}.filter { it.isNotEmpty() }
        )
        val isReferences = refList.all {it.isReference}
        if (refList.isEmpty() || !isReferences) {
            if (!isReferences) {
                logger.debug("Drop because grobid can't identify parsed string as reference")
            }
            logger.debug("done by GROBID")
            refList = GrobidReferenceExtractor.getReferences(pdf)
        }
        else {
            logger.debug("done by CUSTOM")
        }
        logger.debug(refList.mapIndexed { i, ref -> "  ${i + 1}) $ref"}.joinToString(prefix = "\n", separator = "\n"))
        return refList
    }

    //get indent from each line
    fun getLines(text : String) : List<Line> {
        val indentRegex = """${PdfMarks.IntBeg.str}\d{1,3}${PdfMarks.IntEnd.str}""".toRegex()
        var pageNumber = 0
        return text.split('\n').map {line ->
            if (line == PdfMarks.PageStart.str) {
                pageNumber += 1
            }
            val matchResBeg = """^$indentRegex""".toRegex().find(line)
            val matchResEnd = """$indentRegex$""".toRegex().find(line)
            if (matchResBeg != null && matchResEnd != null) {
                val indent = matchResBeg.value.drop(PdfMarks.IntBeg.str.length).dropLast(
                    PdfMarks.IntEnd.str.length).toInt()
                val lastPos = matchResEnd.value.drop(PdfMarks.IntBeg.str.length).dropLast(
                    PdfMarks.IntEnd.str.length).toInt()
                return@map Line(
                    indent,
                    lastPos,
                    line.replace(indentRegex, ""),
                    pageNumber
                )
            }
            else {
                if (line == PdfMarks.PageStart.str) {
                    return@map Line(
                        PdfMarks.PageStart.num,
                        0,
                        line,
                        pageNumber
                    )
                }
                if (line == PdfMarks.PageEnd.str) {
                    return@map Line(
                        PdfMarks.PageEnd.num,
                        0,
                        line,
                        pageNumber
                    )
                }
            }
            Line(-1, 0, "@", pageNumber)
        }.filter { line -> line.indent != -1 || line.str != "@" }
    }

    //return the index of the first line of references
    private fun findRefBegin(lines: List<Line>) : Int {
        val i1 = lines.indexOfLast { line ->
            line.str.contains("${PdfMarks.RareFont}References")
                    || line.str.contains("${PdfMarks.RareFont}REFERENCES")
        }
        if (i1 != -1) {
            return i1 + 1
        }

        val i2 = lines.indexOfLast { line ->
            line.str.contains("References") || line.str.contains("REFERENCES")
        }
        if (i2 != -1) {
            return i2 + 1
        }

        //search for [1], [2], [3], ... [n]
        //or search for 1., 2., 3., ... n.
        //or search for 1, 2, 3, ... n
        //where n from 50 to 10 with step 10
        val numberList1 = mutableListOf<Regex>()
        val numberList2 = mutableListOf<Regex>()
        val numberList3 = mutableListOf<Regex>()
        for (a in 50 downTo 1) {
            numberList1 += """\[$a]""".toRegex()
            numberList2 += """$a\.[^\d]""".toRegex()
            numberList3 += """$a[^\d]""".toRegex()
        }

        fun findSequenceFromEnd(list : List<Regex>) : Int {
            var lastIndex = lines.lastIndex
            var lastPage = -1
            for (s in list) {
                while (lastIndex > -1) {
                    if (lastPage != -1 && lastPage - lines[lastIndex].pn > 1) {
                        return -1
                    }
                    if (lines[lastIndex].str.contains("^$s".toRegex())) {
                        lastPage = lines[lastIndex].pn
                        break
                    }
                    lastIndex--
                }
            }
            return lastIndex
        }

        for (i in 50 downTo 10 step 10) {
            val i3 = findSequenceFromEnd(numberList1.subList(50 - i, 50))
            if (i3 != -1) {
                return i3
            }
        }
        for (i in 50 downTo 10 step 10) {
            val i4 = findSequenceFromEnd(numberList2.subList(50 - i, 50))
            if (i4 != -1) {
                return i2
            }
            val i5 = findSequenceFromEnd(numberList3.subList(50 - i, 50))
            if (i5 != -1) {
                return i5
            }
        }

        val i6 = lines.indexOfLast { line -> line.str.contains("references", ignoreCase = true) }
        return i6 + 1
    }

    private fun parseReferences(lines : List<Line>, isTwoColumn : Boolean, pageWidth : Int) : List<String> {
        //find type of references
        var type : ReferenceType? = null
        for (refType in ReferenceType.values()) {
            if (refType.firstRegex.containsMatchIn(lines[0].str)) {
                type = refType
                break
            }
        }
        val refList = mutableListOf<String>()

        //futher if we return empty list
        // means that we want grobid to parse this document later

        logger.debug("References type: $type")
        logger.debug("Has two columns: $isTwoColumn")
        if (type == null) {
            return listOf()
        }
        return ReferenceParser.parse(
            lines,
            type,
            isTwoColumn,
            pageWidth
        ).also {
            logger.info("Parsed ${it.size} references")
        }
    }

    private fun removeEmptyLines(lines : List<Line>) = lines.filter {!it.str.matches("""\s*""".toRegex())}
    private fun removeLeadingSpaces(lines : List<Line>) = lines.map { line -> line.str = line.str.trim(); line}
}