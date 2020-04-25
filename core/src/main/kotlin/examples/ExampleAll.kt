import com.preprint.server.arxiv.ArxivAPI
import com.preprint.server.pdf.PdfHandler
import com.preprint.server.ref.CustomReferenceExtractor

import java.io.File
import kotlin.system.measureTimeMillis


fun main() {
    val START_DATE = "2013-06-20"
    val NUMBER_OF_PDF_TO_DOWNLOAD = 10
    val FOLDER_PATH = "files/test/"
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")

    val arxivRecords = ArxivAPI.getBulkArxivRecords(START_DATE, "").first!!.subList(0, NUMBER_OF_PDF_TO_DOWNLOAD)
    println(measureTimeMillis {
        PdfHandler.getFullInfo(arxivRecords, FOLDER_PATH, CustomReferenceExtractor, listOf(),true)
    })
    for (record in arxivRecords) {
        File("${FOLDER_PATH}${record.id}.txt").writeText(record.toString())
    }
}