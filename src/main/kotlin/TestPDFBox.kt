package testpdf

import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import kotlin.math.abs
import kotlin.system.measureTimeMillis

fun extractReferences(text : String, pageWidth : Double) : List <String> {
    //find where references begin
    var ind = text.lastIndexOfAny(listOf("References\\\$\\", "REFERENCES\\\$\\"), text.lastIndex, ignoreCase = false)
    if (ind == -1) {
        ind = text.lastIndexOfAny(listOf("References", "REFERENCES"), text.lastIndex, ignoreCase = false)
        if (ind == -1) {
            val indOfFirstRef = text.lastIndexOfAny(listOf("[1]"), text.lastIndex, ignoreCase = false)
            if (indOfFirstRef == -1) {
                return listOf()
            }
            ind = text.lastIndexOf("@d", indOfFirstRef)
            ind = text.lastIndexOf("@d", ind - 1) - 1
        }
    }
    //removing mark for bold text
    var refs =  text.substring(ind).replace("\\\$\\", "");
    //find the end of references
    val indBeg = refs.indexOf('\n') + 1
    var indEnd = refs.indexOfAny(listOf("\n\n", "\\%\\"), indBeg) // \\%\\ indicates a big space between lines
    if (indEnd == -1) {
        indEnd = refs.lastIndex
    }
    refs = refs.substring(indBeg, indEnd)
    println(refs)
    //remove page number and empty lines
    val indentRegex = """@d[0-9.]+@d""".toRegex()
    val regexpPageNumber = ("""($indentRegex)?\s*(\d{1,3})?\s*""").toRegex()
    val linesWithInd = refs.lines().filter {line ->
        !line.matches(regexpPageNumber) && !line.contains("\\%p")
    }.map {line ->
        val match = "^$indentRegex".toRegex().find(line) ?: return@map Pair(0.0, line)
        Pair(match.value.drop(2).dropLast(2).toDouble(), line.drop(match.range.last + 1))
    }

    val refList = mutableListOf<String>()

    var firstIndent = linesWithInd[0].first
    var otherIndent = -1.0
    for ((indent, _) in linesWithInd) {
        if (abs(indent - firstIndent) > 2) {
            otherIndent = indent
            break
        }
    }

    fun isFirstRefLine(line : String, firsLine : String) : Boolean {
        if (firsLine[0] == '[') {
            if (line[0] != '[') {
                return false
            }
        }
        if (firsLine[0].isDigit()) {
            if (!line[0].isDigit()) {
                return false
            }
        }
        if (firsLine[0].isLetter()) {
            if (!line[0].isLetter()) {
                return false
            }
        }
        return true
    }

    //parse references
    if (otherIndent == -1.0) {
        otherIndent = firstIndent
    }
    var curRef = ""
    var lastRefLineIndex = 0
    println("$firstIndent $otherIndent")
    for((i, pair) in linesWithInd.withIndex()) {
        val (indent, line) = pair
        if (i == 0) {
            curRef = line
            continue
        }
        if (abs(indent - firstIndent) < 2 && isFirstRefLine(line, linesWithInd[0].second)) {
            refList.add(curRef.replace(indentRegex, ""))
            curRef = line
            lastRefLineIndex = i
        } else {
            println("$indent $otherIndent")
            if (abs(indent - otherIndent) < 2) {
                if (curRef.isNotEmpty() && curRef.last() == '-') {
                    curRef = curRef.dropLast(1)
                    curRef += line
                } else {
                    curRef += " $line"
                }
            } else {
                val patternChanged = ((indent < firstIndent && abs(indent - firstIndent) < 10)
                        || indent > pageWidth / 2 || firstIndent > pageWidth / 2 && indent < pageWidth / 5)
                if (patternChanged && isFirstRefLine(line, linesWithInd[0].second)) {
                    if (indent > pageWidth / 2 || firstIndent > pageWidth / 2 && indent < pageWidth / 5) {
                        otherIndent = indent + otherIndent - firstIndent
                    }
                    firstIndent = indent
                    refList.add(curRef.replace(indentRegex, ""))
                    curRef = line
                    lastRefLineIndex = i
                } else {
                    println(i)
                    //pattern has ended
                    break
                }
            }
        }
    }
    if (curRef.isNotEmpty()) {
        refList.add(curRef.replace(indentRegex, ""))
    }
    return refList
    refs = linesWithInd.joinToString(separator = "\n", prefix = "\n") {(intend, string) -> string.replace(indentRegex, "")}

    //parse references if previous method didn't work
    /*val regexpFirstType = """\n\[[\w-]+][\s\S]*?\n\[""".toRegex()
    if (regexpFirstType.find(refs) != null) {
        var i = 0
        while (true) {
            val match = regexpFirstType.find(refs, i) ?: break
            i = match.range.last - 1
            val ref = match.value.lines().dropLast(1).drop(1).joinToString(separator = " ")
            refList += ref
        }
        refList += refs.substring(i + 1).lines()
            .dropLastWhile{it.matches("""\s*""".toRegex())}.joinToString(separator = " ")
        return refList
    }

     */
    return refList
}

fun main() {

    println(measureTimeMillis {
        for (fileName in test_files) {
            val pdStripper = PDFBoldTextStripper()

            val inputFile = File(prefix + fileName)
            val outputFile = File(prefix + "extractedPDFBox/" + fileName + ".txt")
            val doc = PDDocument.load(inputFile)
            val pageWidth = doc.pages[0].mediaBox.width.toDouble()
            val text = pdStripper.getText(doc)
            val refs = extractReferences(text, pageWidth)
            outputFile.writeText("")
            refs.forEach { outputFile.appendText(it + "\n") }
            println(fileName)
            doc.close()
        }
    })
}