package com.preprint.server.arxivs3

import com.preprint.server.Config
import java.io.File

object ArxivS3PdfHandler {
    val path = Config.config["arxiv_pdf_path"].toString()
    val manifestFileName = "manifest.xml"
    fun beginBulkDownload() {
        val manifestPath = "$path/$manifestFileName"
        if (!File(manifestPath).exists()) {
            ArxivS3Downloader.downloadManifest(manifestPath)
        }
        
    }
}