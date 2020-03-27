package testpdf

import java.io.File
import kotlin.math.abs

object ReferenceExtractor {
    data class Line(val indent : Int, val str : String, val pn : Int)
    fun extract(textWithMarks : String, pageWidth : Double) : List <String> {
        var lines = getLines(textWithMarks)
        lines = removePageStrings(lines)
        return listOf()
    }

    //get indent from each line
    private fun getLines(text : String) : List<Line> {
        val indentRegex = """${PdfMarks.IntBeg.str}\d{1,3}${PdfMarks.IntEnd.str}""".toRegex()
        var pageNumber = 0
        return text.split('\n').map {line ->
            if (line == PdfMarks.PageStart.str) {
                pageNumber += 1
            }
            val matchRes = """^$indentRegex""".toRegex().find(line)
            if (matchRes != null) {
                val indent = matchRes.value.drop(PdfMarks.IntBeg.str.length).dropLast(PdfMarks.IntEnd.str.length).toInt()
                return@map Line(indent, line.replace(indentRegex, ""), pageNumber)
            }
            else {
                if (line == PdfMarks.PageStart.str) {
                    return@map Line(PdfMarks.PageStart.num, "", pageNumber)
                }
                if (line == PdfMarks.PageEnd.str) {
                    return@map Line(PdfMarks.PageEnd.num, "", pageNumber)
                }
            }
            Line(-1, "@", pageNumber)
        }.filter { line -> line.indent != -1 || line.str != "@" }
    }

    private fun removePageStrings(lines : List<Line>) : List<Line> {
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
                    return lines
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
        var pagePattern = 0;
        if (pageNumberPos.size < 6) {
            when {
                (pageNumberPos.all {it == 0}) -> pagePattern = 0
                (pageNumberPos.all {it == 1}) -> pagePattern = 1
                else                          -> return lines //we haven't got enough information
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
            return lines
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
            else if (i + 1 < lines.lastIndex && lines[i + 1].indent == PdfMarks.PageEnd.num) {
                !deleter(false, line)
            } else {
                true
            }
        }
    }
}