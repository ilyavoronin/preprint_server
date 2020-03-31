package preprint.server.examples

import preprint.server.ref.CustomReferenceExtractor

import java.io.File
import kotlin.system.measureTimeMillis

fun main() {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")

    println(measureTimeMillis {
        for (fileName in test_files1) {
            val inputFile = File(prefix + fileName)
            val outputFile = File(prefix + "extractedPDFBox/" + fileName + ".txt")

            val pdf = inputFile.readBytes()

            val refs = CustomReferenceExtractor.extract(pdf)

            outputFile.writeText("")
            refs.forEach { outputFile.appendText(it + "\n") }
            println(fileName)
        }
    })
}