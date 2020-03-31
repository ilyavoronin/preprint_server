package preprint.server.examples

import preprint.server.arxiv.ArxivAPI
import preprint.server.pdf.PdfHandler
import preprint.server.ref.CermineReferenceExtractor
import preprint.server.ref.CustomReferenceExtractor
import preprint.server.ref.GrobidReferenceExtractor

import java.io.File
import kotlin.system.measureTimeMillis


const val START_DATE = "2020-03-20"
const val NUMBER_OF_PDF_TO_DOWNLOAD = 100
const val FOLDER_PATH = "files/testcu/"

fun main() {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")

    val arxivRecords = ArxivAPI.getBulkArxivRecords(START_DATE, "").first!!.subList(0, NUMBER_OF_PDF_TO_DOWNLOAD)
    println(measureTimeMillis {
        PdfHandler.getFullInfo(arxivRecords, FOLDER_PATH, CustomReferenceExtractor)
    })
    for (record in arxivRecords) {
        File("${FOLDER_PATH}${record.id}.txt").writeText(record.toString())
    }
}