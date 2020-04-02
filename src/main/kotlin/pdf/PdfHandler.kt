package preprint.server.pdf

import preprint.server.data.Data

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result;
import org.apache.logging.log4j.kotlin.logger
import preprint.server.ref.ReferenceExtractor
import java.io.File
import java.lang.Exception
import java.lang.Thread.sleep

object PdfHandler {
    val logger = logger()
    private const val SLEEP_TIME : Long = 0
    fun getFullInfo(recordList : List <Data>, outputPath : String, refExtractor : ReferenceExtractor) {
        logger.info("Begin download of ${recordList.size} pdf")
        for ((i, record) in recordList.withIndex()) {
            logger.info("downloading $i: ${record.id}")
            logger.info("pdf url: ${record.pdfUrl}")

            if (record.pdfUrl == "") {
                logger.error("Failed to download: pdf url is empty")
                File(outputPath + "failed.txt").appendText("${record.id}\n")
                continue
            }

            val pdf = downloadPdf(record.pdfUrl) ?: return
            File("$outputPath${record.id}.pdf").writeBytes(pdf)

            record.refList =  try {
                refExtractor.extract(pdf).toMutableList()
            } catch (e : Exception) {
                logger.error(e)
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
                logger.error(ex)
                null
            }
            is Result.Success -> {
                logger.info("Success: downloaded")
                result.get()
            }
        }
    }
}