import com.preprint.server.ref.CustomReferenceExtractor

import java.io.File
import kotlin.system.measureTimeMillis

fun main() {
    println(measureTimeMillis {
        for (fileName in test_files1) {
            val inputFile = File(prefix + fileName)
            val outputFile = File(prefix + "extractedPDFBox/" + fileName + ".txt")

            val pdf = inputFile.readBytes()

            val refs = CustomReferenceExtractor.getReferences(pdf)

            outputFile.writeText("")
            refs.forEach { outputFile.appendText(it.toString() + "\n") }
            println(fileName)
        }
    })
}