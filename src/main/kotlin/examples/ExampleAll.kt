package preprint.server.examples

import preprint.server.arxiv.ArxivAPI
import preprint.server.pdf.PdfHandler

import java.io.File


const val START_DATE = "2020-03-20"
const val NUMBER_OF_PDF_TO_DOWNLOAD = 10
const val FOLDER_PATH = "files/test/"

fun main() {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")

    val arxivRecords = ArxivAPI.getBulkArxivRecords(START_DATE)!!.subList(0, NUMBER_OF_PDF_TO_DOWNLOAD)
    PdfHandler.getFullInfo(arxivRecords, FOLDER_PATH)
    for (record in arxivRecords) {
        File("${FOLDER_PATH}${record.id}.txt").writeText(record.toString())
    }
}