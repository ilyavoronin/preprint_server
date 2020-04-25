import org.allenai.scienceparse.Parser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis

fun main() {
    println(measureTimeMillis {
        for (pdfFilename in test_files) {
            val parser = Parser.getInstance()
            val pdf = File(prefix + pdfFilename).readBytes()
            val inputStream: InputStream = ByteArrayInputStream(pdf)
            val refs = parser.doParse(inputStream).references
            File("test.txt").writeText("")
            for (elem in refs) {
                File("test.txt").appendText(elem.toString() + "\n")
            }
        }
    })
}