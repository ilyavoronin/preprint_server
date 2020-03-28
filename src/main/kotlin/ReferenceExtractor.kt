package testpdf

import java.io.File
import java.lang.Integer.max
import kotlin.math.abs

object ReferenceExtractor {
    data class Line(val indent : Int, var str : String, val pn : Int)

    fun extract(textWithMarks : String, pageWidth : Double) : List <String> {
        var lines = getLines(textWithMarks)
        lines = removeEmptyLines(lines)
        lines = removePageHeaders(lines)
        //lines = removePageStrings(lines)
        val ind = findRefBegin(lines)
        println(ind)
        if (ind == -1) {
            return listOf()
        }
        lines = lines.drop(ind)
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

    //return the index of the first line of references
    fun findRefBegin(lines: List<Line>) : Int {
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
        val numberList1 = mutableListOf<String>()
        val numberList2 = mutableListOf<String>()
        val numberList3 = mutableListOf<String>()
        for (a in 50 downTo 1) {
            numberList1 += "[$a]"
            numberList2 += "$a."
            numberList3 += a.toString()
        }

        fun findSequenceFromEnd(list : List<String>) : Int {
            var lastIndex = lines.lastIndex
            var lastPage = lines.last().pn
            for (s in list) {
                while (lastIndex > -1) {
                    if (lastPage - lines[lastIndex].pn > 1) {
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
        return -1
    }

    fun removePageHeaders(lines : List<Line>) : List<Line> {
        //find longest common substring
        fun findLGS(s1 : String, s2 : String) : String {
            if (s1 == s2) {
                return s1
            }
            val dp = Array(s1.length) {IntArray(s2.length)}
            for (i in 0 until s1.length + 1) {
                for (j in 0 until s2.length + 1) {
                    dp[i][j] = 0
                }
            }
            for (i in 1 until s1.length + 1) {
                for (j in 1 until s2.length + 1) {
                    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
                    if (s1[i - 1] == s2[j - 1]) {
                        dp[i][j] == max(dp[i][j], dp[i - 1][j - 1])
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
            //we capture 'lines' from outer function

            val c = 0.75
            var state : Int = 0
            //current longest substring for the last lines
            var curMaxString = ""
            var runLength = 0
            for ((i, index) in listIndices.withIndex()) {
                if (state == 1) {
                    val newString = findLGS(curMaxString, lines[index].str)
                    if (newString.length == curMaxString.length) {
                        runLength += 1
                        continue
                    }
                    else {
                        if (runLength >= 3) {
                            for (j in i - 1 downTo i - runLength + 1) {
                                lines[listIndices[j]].str = ""
                            }
                        }
                        state = 0
                        runLength = 0
                    }
                }
                if (i == listIndices.lastIndex) {
                    break
                }
                curMaxString = findLGS(lines[index].str, lines[listIndices[i + 1]].str)
                if (curMaxString.length > lines[index].str.length * c &&
                    curMaxString.length > lines[listIndices[i + 1]].str.length * c) {
                    //the we assume that this strings contain header
                    state = 1
                    runLength = 2
                }
                else {
                    curMaxString = ""
                }
            }
        }

        val firstLineInd = getFirstLineIndices(lines)
        val lastLineInd = getLastLineIndices(lines)
        removeHeaders(firstLineInd)
        removeHeaders(lastLineInd)
        return lines.filter { it.str != "" }
    }

    fun getFirstLineIndices(lines : List<Line>) : List<Int> {
        return lines.mapIndexed { i, line ->
            if (line.indent == PdfMarks.PageStart.num && i + 1 < lines.size) {
                i + 1
            }
            else {
                -1
            }
        }.filter{ it != -1 }
    }

    fun getLastLineIndices(lines : List<Line>) : List<Int> {
        return lines.mapIndexed { i, line ->
            if (line.indent == PdfMarks.PageEnd.num && i - 1 >= 0) {
                i - 1
            }
            else {
                -1
            }
        }.filter{ it != -1 }
    }

    fun removeEmptyLines(lines : List<Line>) = lines.filter {it.str != ""}
}