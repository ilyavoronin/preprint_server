package preprint.server.examples

import pl.edu.icm.cermine.ContentExtractor
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.system.measureTimeMillis


fun main() {
    println(measureTimeMillis {
        for (pdfFilename in test_files1) {
            val extractor = ContentExtractor()
            val inputStream: InputStream = FileInputStream(prefix + pdfFilename)
            extractor.setPDF(inputStream)

            val file = File(prefix + "extracted_cermine/${pdfFilename}.txt")
            val references = extractor.references

            file.writeText("#### REFERENCES ####\n")
            for (ref in references) {
                file.appendText(ref.text + "\n")
            }
            println("$pdfFilename done")
        }
    })
}