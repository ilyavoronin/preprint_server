package com.preprint.server.pdf

import com.preprint.server.data.Data
import com.preprint.server.ref.ReferenceExtractor

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.lang.Exception
import java.lang.Thread.sleep

object PdfHandler {
    val logger = logger()
    private const val SLEEP_TIME : Long = 0
    fun getFullInfo(
        recordList : List <Data>,
        outputPath : String,
        refExtractor : ReferenceExtractor?,
        savePdf : Boolean
    ) {
        logger.info("Begin download of ${recordList.size} pdf")
        for ((i, record) in recordList.withIndex()) {
            logger.info("downloading $i: ${record.id}")
            logger.info("pdf url: ${record.pdfUrl}")

            if (record.pdfUrl == "") {
                logger.error("Failed to download: pdf url is empty")
                File(outputPath + "failed.txt").appendText("${record.id}\n")
                continue
            }

            // Load file from local storage or download if missing
            // Assuming that the link always looks like "http://arxiv.org/pdf/{record.id}v{version}"
            val pdfName = record.pdfUrl.split('/').last()
            val pdfFile = File("$outputPath${pdfName}.pdf")
            val pdf = if (pdfFile.exists()) {
                pdfFile.readBytes()
            } else {
                downloadPdf(record.pdfUrl) ?: return
            }

            // Save if new file was downloaded and savePdf is true
            if (pdfFile.exists()) {
                logger.info("PDF file for record ${record.id} has been downloaded earlier")
            } else if (savePdf) {
                pdfFile.writeBytes(pdf)
            }

            refExtractor?.let {
                record.refList = try {
                    it.extract(pdf).toMutableList()
                } catch (e: Exception) {
                    logger.error(e)
                    File(outputPath + "failed.txt").appendText("${record.id}\n")
                    return
                }
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