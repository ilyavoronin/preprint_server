package examples

import com.preprint.server.Config
import com.preprint.server.arxivs3.S3Downloader

fun main() {
    val path = Config.config["arxiv_pdf_path"].toString()
    S3Downloader.downloadManifest(path + "/manifest.xml")
}