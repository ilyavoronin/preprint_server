package preprint.server.pdf

import preprint.server.data.Data
import preprint.server.ref.PDFBoldTextStripper
import preprint.server.ref.CustomReferenceExtractor

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result;
import org.apache.pdfbox.pdmodel.PDDocument
import preprint.server.ref.ReferenceExtractor
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.Thread.sleep

object PdfHandler {
    private const val SLEEP_TIME : Long = 0
    fun getFullInfo(recordList : List <Data>, outputPath : String, refExtractor : ReferenceExtractor) {
        for ((i, record) in recordList.withIndex()) {
            println("downloading $i: ${record.id}")
            println(record.pdfUrl)

            if (record.pdfUrl == "") {
                File(outputPath + "failed.txt").appendText("${record.id}\n")
                continue
            }

            val pdf = downloadPdf(record.pdfUrl) ?: return
            File("$outputPath${record.id}.pdf").writeBytes(pdf)

            record.refList =  try {
                refExtractor.extract(pdf).toMutableList()
            } catch (e : Exception) {
                println(e.message)
                File(outputPath + "failed.txt").appendText("${record.id}\n")
                continue
            }

            sleep(SLEEP_TIME)
        }
    }

    fun downloadPdf(url : String) : ByteArray? {
        val (_, _, result) = url
            .httpGet()
            .response()
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                println(ex)
                null
            }
            is Result.Success -> {
                println("Success")
                result.get()
            }
        }
    }
}