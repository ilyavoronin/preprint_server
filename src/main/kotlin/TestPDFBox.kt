package testpdf

import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import kotlin.system.measureTimeMillis

fun main() {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    println(measureTimeMillis {
        for (fileName in test_files1) {
            val inputFile = File(prefix + fileName)
            val outputFile = File(prefix + "extractedPDFBox/" + fileName + ".txt")
            val doc = PDDocument.load(inputFile)
            val pageWidth = doc.pages[0].mediaBox.width.toDouble()

            val pdStripper = PDFRefTextStripper()
            val text = pdStripper.getText(doc)
            val refs = ReferenceExtractor.extract(text, pageWidth)
            outputFile.writeText("")
            refs.forEach { outputFile.appendText(it + "\n") }
            println(fileName)
            doc.close()
        }
    })
}