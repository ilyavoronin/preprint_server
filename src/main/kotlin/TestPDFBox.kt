package testpdf

import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import kotlin.system.measureTimeMillis

fun extractReferences(text : String) : String {
    //find where references begin
    var ind = text.lastIndexOf("References\\\$\\", text.lastIndex, ignoreCase = true)
    if (ind == -1) {
        ind = text.lastIndexOf("References", ignoreCase = true)
        if (ind == -1) {
            return "";
        }
    }
    //removing mark for bold text
    var refs =  text.substring(ind).replace("\\\$\\", "");

    //find the end of references
    val indBeg = refs.indexOf('\n') + 1
    var indEnd = refs.indexOfAny(listOf("\n\n", "\\%\\"), indBeg)
    if (indEnd == -1) {
        indEnd = refs.lastIndex
    }
    refs = refs.substring(indBeg, indEnd)

    //remove page number and empty lines
    val regexpPageNumber = """\s*(\d{1,3})?\s*""".toRegex()
    val lines = refs.lines().filter {line ->
        !line.matches(regexpPageNumber)
    }
    refs = lines.joinToString(separator = "\n")

    return refs
}

fun main() {

    println(measureTimeMillis {
        for (fileName in test_files) {
            val pdStripper = PDFBoldTextStripper()

            val inputFile = File(prefix + fileName)
            val outputFile = File(prefix + "extractedPDFBox/" + fileName + ".txt")
            val doc = PDDocument.load(inputFile)

            val text = pdStripper.getText(doc)
            val refs = extractReferences(text)
            outputFile.writeText(refs)
            println(fileName)
        }
    })
}