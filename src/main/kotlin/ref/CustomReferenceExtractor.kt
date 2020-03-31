package preprint.server.ref

import com.google.common.math.DoubleMath.roundToInt
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import java.lang.Integer.max
import kotlin.math.round
import kotlin.math.roundToInt

object CustomReferenceExtractor : ReferenceExtractor {
    data class Line(val indent : Int, val lastPos : Int, var str : String, val pn : Int)

    override fun extract(pdf : ByteArray) : List <String> {
        val doc = PDDocument.load(pdf)
        val textWithMarks = PDFRefTextStripper.getMarkedText(doc)
        val pageWidth = doc.pages[0].mediaBox.width.toDouble()
        val isTwoColumns = PDFRefTextStripper.isTwoColumns

        var lines = getLines(textWithMarks)
        lines = removeEmptyLines(lines)
        lines = removeLeadingSpaces(lines)
        while (true) {
            val oldSize = lines.size
            lines = removePageNumbers(lines)
            lines = removePageHeaders(lines)
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
        File("test.txt").writeText(lines.joinToString(separator = "\n") {line -> line.str})
        return parseReferences(lines, isTwoColumns, pageWidth.roundToInt())
    }

    //get indent from each line
    private fun getLines(text : String) : List<Line> {
        val indentRegex = """${PdfMarks.IntBeg.str}\d{1,3}${PdfMarks.IntEnd.str}""".toRegex()
        var pageNumber = 0
        return text.split('\n').map {line ->
            if (line == PdfMarks.PageStart.str) {
                pageNumber += 1
            }
            val matchResBeg = """^$indentRegex""".toRegex().find(line)
            val matchResEnd = """$indentRegex$""".toRegex().find(line)
            if (matchResBeg != null && matchResEnd != null) {
                val indent = matchResBeg.value.drop(PdfMarks.IntBeg.str.length).dropLast(PdfMarks.IntEnd.str.length).toInt()
                val lastPos = matchResEnd.value.drop(PdfMarks.IntBeg.str.length).dropLast(PdfMarks.IntEnd.str.length).toInt()
                return@map Line(indent, lastPos, line.replace(indentRegex, ""), pageNumber)
            }
            else {
                if (line == PdfMarks.PageStart.str) {
                    return@map Line(PdfMarks.PageStart.num, 0, line, pageNumber)
                }
                if (line == PdfMarks.PageEnd.str) {
                    return@map Line(PdfMarks.PageEnd.num, 0, line, pageNumber)
                }
            }
            Line(-1, 0,"@", pageNumber)
        }.filter { line -> line.indent != -1 || line.str != "@" }
    }

    private fun removePageNumbers(lines : List<Line>) : List<Line> {
        //find out where page numbers are located(bottom or top or alternate)

        val pageNumberPos = mutableListOf<Int>()
        for ((i, line) in lines.withIndex()) {
            if (line.pn == 1) {
                continue
            }
            if (line.indent == PdfMarks.PageStart.num) {
                if (i + 1 <= lines.lastIndex && lines[i + 1].str.contains(line.pn.toString())) {
                    pageNumberPos.add(0)
                }
            }
            if (i + 1 <= lines.lastIndex && lines[i + 1].indent == PdfMarks.PageEnd.num) {
                if (line.str.contains(line.pn.toString())) {
                    if (pageNumberPos.size + 1 == line.pn) {
                        //the this page has page number in the first and in the last line
                        //mark this page with 2
                        pageNumberPos[pageNumberPos.lastIndex] = 2
                    }
                    else {
                        //this page has page number in the last line
                        pageNumberPos.add(1)
                    }
                }
                if (pageNumberPos.size + 1 < line.pn) {
                    //then this page doesn't contain page number
                    //and we will assume that all pages doesn't contain page number
                    return removePageNumbersSimple(lines)
                }
            }

            //we only want to scan first 6 pages
            if (line.pn == 8) {
                break
            }
        }
        //0 -- page number in the first line
        //1 -- page number in the second line
        //2 -- in the odd pages in the first line, in the even on the last
        //3 -- in the even pages in the last line, in the odd on the first
        //4 -- in the first and in the last line
        val pagePattern : Int
        if (pageNumberPos.size < 6) {
            when {
                (pageNumberPos.all {it == 0}) -> pagePattern = 0
                (pageNumberPos.all {it == 1}) -> pagePattern = 1
                else                          -> pagePattern = -1 //we haven't got enough information
            }
        }
        else {
            when {
                (pageNumberPos.all {it == 2}) -> pagePattern = 4

                (pageNumberPos.all {it == 0 || it == 2})      -> pagePattern = 0

                (pageNumberPos.all {it == 1 || it == 2})      -> pagePattern = 1

                (pageNumberPos.withIndex().all
                {(i, p) -> (i % 2 == 0 && (p == 0 || p == 2))
                        || i % 2 == 1 && (p == 1 || p == 2)}) -> pagePattern = 2

                (pageNumberPos.withIndex().all
                {(i, p) -> (i % 2 == 0 && (p == 1 || p == 2))
                        || i % 2 == 1 && (p == 0 || p == 2)}) -> pagePattern = 3

                else                                          -> pagePattern = -1
            }
        }

        if (pagePattern == -1) {
            return removePageNumbersSimple(lines)
        }

        //(is first or last line, line, page number) -> should we delete this line or not
        val deleter : (Boolean, Line) -> Boolean = when(pagePattern) {
            0 -> {
                isFirst, line -> isFirst && (line.str.contains(line.pn.toString()))
            }
            1 -> {
                isFirst, line -> !isFirst && (line.str.contains(line.pn.toString()))
            }
            2 -> {
                isFirst, line -> isFirst && line.pn % 2 == 1 || !isFirst && line.pn % 2 == 0
            }
            3 -> {
                isFirst, line -> isFirst && line.pn % 2 == 0 || !isFirst && line.pn % 1 == 0
            }
            else -> {
                _, _ -> true
            }
        }

        return lines.filterIndexed{i, line ->
            if (i - 1 > 0 && lines[i - 1].indent == PdfMarks.PageStart.num) {
                !deleter(true, line)
            }
            else if (i + 1 <= lines.lastIndex && lines[i + 1].indent == PdfMarks.PageEnd.num) {
                !deleter(false, line)
            } else {
                true
            }
        }
    }

    private fun removePageNumbersSimple(lines : List<Line>) : List<Line> {
        val firstLineIndices = getFirstLineIndices(lines)
        val lastLineIndices = getLastLineIndices(lines)

        //drop first page
        firstLineIndices.drop(1)
        lastLineIndices.drop(1)

        fun isDigits(indices : List<Int>) : Boolean {
            return indices.all{ind -> lines[ind].str.all {it.isDigit()}}
        }

        return when {
            isDigits(firstLineIndices) -> lines.filterIndexed {i, line ->
                !(i + 1 <= lines.lastIndex && lines[i + 1].indent == PdfMarks.PageEnd.num)
            }
            isDigits(lastLineIndices) -> lines.filterIndexed {i, line ->
                !(i + 1 <= lines.lastIndex && lines[i + 1].indent == PdfMarks.PageEnd.num)
            }
            else -> lines
        }
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

    private fun removePageHeaders(lines : List<Line>) : List<Line> {
        //find longest common substring
        fun findLGS(s1 : String, s2 : String) : String {
            if (s1 == s2) {
                return s1
            }
            val dp = Array(s1.length + 1) {IntArray(s2.length + 1)}
            for (i in 0 until s1.length + 1) {
                for (j in 0 until s2.length + 1) {
                    dp[i][j] = 0
                }
            }
            for (i in 1 until s1.length + 1) {
                for (j in 1 until s2.length + 1) {
                    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
                    if (s1[i - 1] == s2[j - 1]) {
                        dp[i][j] = max(dp[i][j], dp[i - 1][j - 1] + 1)
                    }
                }
            }
            var li : Int = s1.length
            var lj : Int = s2.length
            var res = ""
            while (li > 0 && lj > 0) {
                if (dp[li][lj] == dp[li - 1][lj]) {
                    li -= 1
                    continue
                }
                if (dp[li][lj] == dp[li][lj - 1]) {
                    lj -= 1
                    continue
                }
                res += s1[li - 1]
                li -= 1
                lj -= 1
            }
            return res.reversed()
        }

        //make all lines with indices from the list, that contains headers, empty
        fun removeHeaders(listIndices : List<Int>) {
            //capture 'lines' from outer function

            var state = 0
            //current longest substring for the last lines
            var curMaxString = ""
            var runLength = 0
            for (i in 1 until listIndices.size) {
                val cur = listIndices[i]
                val prev = listIndices[i - 1]
                if (state == 0) {
                    curMaxString = findLGS(lines[prev].str, lines[cur].str)
                    if (curMaxString.length > lines[cur].str.length * 0.75 &&
                        curMaxString.length > lines[prev].str.length * 0.75
                    ) {
                        //then we assume that this strings contain header
                        state = 1
                        runLength = 2
                    } else {
                        curMaxString = ""
                    }
                    continue
                }
                if (state == 1) {
                    val newString = findLGS(curMaxString, lines[cur].str)
                    if (newString.length == curMaxString.length
                            || curMaxString.length > 15 && curMaxString.length - newString.length < 4) {
                        runLength += 1
                        if (i == listIndices.lastIndex) {
                            if (runLength >= 3) {
                                for (j in i downTo (i - runLength + 1)) {
                                    lines[listIndices[j]].str = ""
                                }
                            }
                        }
                        continue
                    }
                    if (runLength >= 3) {
                        for (j in i - 1 downTo (i - runLength)) {
                            lines[listIndices[j]].str = ""
                        }
                    }
                    state = 0
                    runLength = 0
                }
            }
        }

        val firstLineInd = getFirstLineIndices(lines)
        val lastLineInd = getLastLineIndices(lines)
        val evenFirstLineInd = firstLineInd.filterIndexed{i, _ -> i % 2 == 0}
        val oddFirstLineInd = firstLineInd.filterIndexed {i, _ -> i % 2 == 1}
        val evenLastLineInd = lastLineInd.filterIndexed{i, _ -> i % 2 == 0}
        val oddLastLineInd = lastLineInd.filterIndexed {i, _ -> i % 2 == 1}
        removeHeaders(firstLineInd)
        removeHeaders(lastLineInd)
        removeHeaders(evenFirstLineInd)
        removeHeaders(evenLastLineInd)
        removeHeaders(oddFirstLineInd)
        removeHeaders(oddLastLineInd)
        return lines.filter { it.str != "" }
    }

    private fun parseReferences(lines : List<Line>, isTwoColumn : Boolean, pageWidth : Int) : List<String> {
        //find type of references
        var type : ReferenceType = ReferenceType.A
        for (refType in ReferenceType.values()) {
            if (refType.firstRegex.containsMatchIn(lines[0].str)) {
                type = refType
                break
            }
        }
        val refList = mutableListOf<String>()
        val typeRegex = type.regex

        //futher if we return empty list
        // means that we want grobid to parse this document later

        //[1], [2], ...
        println(type)
        println("isTwoColumn: $isTwoColumn")
        if (type == ReferenceType.A && isTwoColumn == false) {
            //analyze this text
            var canUseSecondIndentPattern = true
            var i = 0
            var secondLineIndent = -1
            var curRefNum = 1
            val firstLineIndices = mutableListOf<Int>()
            var maxWidth = 0
            while (i < lines.size) {
                firstLineIndices.add(i)
                //find next reference
                maxWidth = max(maxWidth, lines[i].lastPos - lines[i].indent)
                var j = i + 1
                while (j < lines.size) {
                    val match = typeRegex.find(lines[j].str)
                    val value = match?.value?.drop(1)?.dropLast(1)?.toInt()
                    if (value != null && value == curRefNum + 1) {
                        break
                    }
                    else {
                        if (value == curRefNum) {
                            return listOf()
                        }
                    }
                    j += 1
                }
                if (j != lines.size) {
                    for (k in i + 1 until j) {
                        if (secondLineIndent == -1) {
                            secondLineIndent = lines[k].indent
                        }
                        if (secondLineIndent != lines[k].indent) {
                            canUseSecondIndentPattern = false
                        }
                    }
                }
                else {
                    //this was the last reference
                    break
                }
                curRefNum += 1
                i = j
            }

            fun addLineToReference(ref : String, line : String) : String {
                return if (ref.length > 2 && ref.last() == '-') {
                    if (ref[ref.lastIndex - 1].isLowerCase()) {
                        ref.dropLast(1) + line
                    }
                    else {
                        ref + line
                    }
                }
                else {
                    if (ref == "") {
                        line
                    }
                    else {
                        ref + " " + line
                    }
                }
            }

            //parse references
            for ((j, lineInd) in firstLineIndices.withIndex()) {
                var curRef = ""
                if (j != firstLineIndices.lastIndex) {
                    val nextLineInd = firstLineIndices[j + 1]
                    for (k in lineInd until nextLineInd) {
                        curRef = addLineToReference(curRef, lines[k].str)

                        if ((curRef.last() == '.' || nextLineInd - lineInd > 5)
                                && ((k != lineInd && lines[k].lastPos < lines[k - 1].lastPos * 0.9)
                                    || (lines[k].lastPos - lines[k].indent) < maxWidth * 0.7)) {

                            //then this is the end of reference
                            break
                        }
                    }
                }
                else {
                    //this is the last reference and we should find it's end
                    for (k in lineInd until lines.size) {
                        curRef = addLineToReference(curRef, lines[k].str)
                        if (canUseSecondIndentPattern && k < lines.lastIndex &&
                            lines[k + 1].indent != secondLineIndent) {
                            //we find the end of reference
                            break
                        }
                        if (k > lineInd) {
                            if (lines[k].lastPos < lines[k - 1].lastPos * 0.9) {
                                //we find the end of reference
                                break
                            }
                        }
                        else {
                            if (lines[k].lastPos - lines[k].indent < 0.9 * maxWidth) {
                                //we find the end of reference
                                break
                            }
                        }
                    }
                }
                refList.add(curRef)
            }
        }

        if (type == ReferenceType.A && isTwoColumn) {
            //analyze this text
            var canUseSecondIndentPattern = true
            var i = 0
            var secondLineIndentLeft = -1
            var secondLineIndentRight = -1

            //current page side(0 -- left, 1 -- right)
            fun getSide(line : Line) : Int {
                return if (lines[0].lastPos < pageWidth * 0.7) 0 else 1
            }
            var curRefNum = 1
            val firstLineIndices = mutableListOf<Int>()
            var maxWidth = 0
            while (i < lines.size) {
                firstLineIndices.add(i)
                //find next reference
                maxWidth = max(maxWidth, lines[i].lastPos - lines[i].indent)
                var j = i + 1
                while (j < lines.size) {
                    val match = typeRegex.find(lines[j].str)
                    val value = match?.value?.drop(1)?.dropLast(1)?.toInt()
                    if (value != null && value == curRefNum + 1) {
                        break
                    }
                    else {
                        if (value == curRefNum) {
                            return listOf()
                        }
                    }
                    j += 1
                }
                if (j != lines.size) {
                    for (k in i + 1 until j) {
                        val curSide = getSide(lines[k])
                        if (curSide == 0) {
                            if (secondLineIndentLeft == -1) {
                                secondLineIndentLeft = lines[k].indent
                            }
                            if (secondLineIndentLeft != lines[k].indent) {
                                canUseSecondIndentPattern = false
                            }
                        }
                        else {
                            if (secondLineIndentRight == -1) {
                                secondLineIndentRight = lines[k].indent
                            }
                            if (secondLineIndentRight != lines[k].indent) {
                                canUseSecondIndentPattern = false
                            }
                        }
                    }
                }
                else {
                    //this was the last reference
                    break
                }
                curRefNum += 1
                i = j
            }

            fun addLineToReference(ref : String, line : String) : String {
                return if (ref.length > 2 && ref.last() == '-') {
                    if (ref[ref.lastIndex - 1].isLowerCase()) {
                        ref.dropLast(1) + line
                    }
                    else {
                        ref + line
                    }
                }
                else {
                    if (ref == "") {
                        line
                    }
                    else {
                        ref + " " + line
                    }
                }
            }

            //parse references
            for ((j, lineInd) in firstLineIndices.withIndex()) {
                var curRef = ""
                if (j != firstLineIndices.lastIndex) {
                    val nextLineInd = firstLineIndices[j + 1]
                    var prevSide = 0
                    for (k in lineInd until nextLineInd) {
                        curRef = addLineToReference(curRef, lines[k].str)
                        val curSide = getSide(lines[k])
                        if ((curRef.last() == '.' || nextLineInd - lineInd > 10)
                            && ((k != lineInd && curSide == prevSide && lines[k].lastPos < lines[k - 1].lastPos * 0.9)
                                    || (lines[k].lastPos - lines[k].indent) < maxWidth * 0.7)) {

                            //then this is the end of reference
                            break
                        }
                    }
                }
                else {
                    //this is the last reference and we should find it's end
                    var prevSide = 0
                    for (k in lineInd until lines.size) {
                        curRef = addLineToReference(curRef, lines[k].str)
                        val curSide = getSide(lines[k])
                        if (canUseSecondIndentPattern && k < lines.lastIndex) {
                            if (curSide == 0 && lines[k + 1].indent != secondLineIndentLeft
                                    || curSide == 1 && lines[k + 1].indent != secondLineIndentRight) {

                                //we find the end of reference
                                break
                            }
                        }
                        if (k > lineInd) {
                            if (prevSide == curSide && lines[k].lastPos < lines[k - 1].lastPos * 0.9) {
                                //we find the end of reference
                                break
                            }
                        }
                        else {
                            if (lines[k].lastPos - lines[k].indent < 0.9 * maxWidth) {
                                //we find the end of reference
                                break
                            }
                        }
                        prevSide = curSide
                    }
                }
                refList.add(curRef)
            }
        }
        return refList
    }

    private fun getFirstLineIndices(lines : List<Line>) : List<Int> {
        return lines.mapIndexed { i, line ->
            if (line.indent == PdfMarks.PageStart.num && i + 1 < lines.size) {
                i + 1
            }
            else {
                -1
            }
        }.filter{ it != -1 }
    }

    private fun getLastLineIndices(lines : List<Line>) : List<Int> {
        return lines.mapIndexed { i, line ->
            if (line.indent == PdfMarks.PageEnd.num && i - 1 >= 0) {
                i - 1
            }
            else {
                -1
            }
        }.filter{ it != -1 }
    }

    private fun removeEmptyLines(lines : List<Line>) = lines.filter {!it.str.matches("""\s*""".toRegex())}
    private fun removeLeadingSpaces(lines : List<Line>) = lines.map {line -> line.str = line.str.trim(); line}
}