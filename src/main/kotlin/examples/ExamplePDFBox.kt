package preprint.server.examples

import preprint.server.ref.CustomReferenceExtractor

import java.io.File
import kotlin.system.measureTimeMillis

fun main() {
    println(measureTimeMillis {
        for (fileName in test_files) {
            val inputFile = File(prefix + fileName)
            val outputFile = File(prefix + "extractedPDFBox/" + fileName + ".txt")

            val pdf = inputFile.readBytes()

            val refs = CustomReferenceExtractor.extract(pdf)

            outputFile.writeText("")
            refs.forEach { outputFile.appendText(it.toString() + "\n") }
            println(fileName)
        }
    })
}