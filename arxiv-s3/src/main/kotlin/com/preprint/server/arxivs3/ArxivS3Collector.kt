package com.preprint.server.arxivs3

import com.preprint.server.Config
import org.apache.logging.log4j.kotlin.logger
import java.io.File

object ArxivS3Collector {
    val logger = logger()
    val path = Config.config["arxiv_pdf_path"].toString()
    val manifestFileName = "manifest.xml"
    fun beginBulkDownload() {
        val manifestPath = "$path/$manifestFileName"
        if (!File(manifestPath).exists()) {
            ArxivS3Downloader.downloadManifest(manifestPath)
        }
        else {
            logger.info("manifest is already downloaded")
        }
        val fileNames = ManifestParser.parseFilenames(manifestPath)
        fileNames.forEach {filename ->
            val pdfPath = "$path/$filename"
            if (!File(pdfPath).exists()) {
                ArxivS3Downloader.download(filename, pdfPath)
            }
            else {
                logger.info("$filename is already downloaded")
            }
        }
    }
}