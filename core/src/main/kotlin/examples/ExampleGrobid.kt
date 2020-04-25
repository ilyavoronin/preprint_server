import org.grobid.core.factory.GrobidFactory
import org.grobid.core.main.GrobidHomeFinder
import org.grobid.core.utilities.GrobidProperties
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis


fun main() {
    val homePath = "/home/ilya/staff/grobid/grobid-home"
    val grobidHomeFinder = GrobidHomeFinder(Arrays.asList(homePath))
    GrobidProperties.getInstance(grobidHomeFinder)
    val engine = GrobidFactory.getInstance().createEngine()

    println(measureTimeMillis {
        for (pdfFilename in test_files) {
            val pdf = File(prefix + pdfFilename)
            val refs = engine.processReferences(pdf, 0)

            val outputFile =File(prefix + "extractedGrobid/$pdfFilename.txt")
            outputFile.writeText("")
            for ((i, elem) in refs.withIndex()) {
                outputFile.appendText("${i + 1}) " + elem.resBib.toBibTeX() + "\n")
            }
            println(pdfFilename)
        }
    })
}