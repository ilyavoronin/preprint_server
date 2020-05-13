package examples

import com.preprint.server.arxivs3.ArxivS3Downloader

fun main() {
    //download manifest
    val path = ArxivS3Config.config["arxiv_pdf_path"].toString()
    ArxivS3Downloader.downloadManifest(path + "/manifest.xml")
}