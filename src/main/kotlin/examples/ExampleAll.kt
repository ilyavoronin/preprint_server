package preprint.server.examples

import preprint.server.arxiv.ArxivAPI
import preprint.server.pdf.PdfHandler

import java.io.File


fun main() {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    val folderPath = "/home/ilya/projects/pdf_test/files/test/"
    val arxivRecords = ArxivAPI.getBulkArxivRecords("2020-03-20")!!.subList(0, 100)
    PdfHandler.getFullInfo(arxivRecords, folderPath)
    for (record in arxivRecords) {
        File("${folderPath}${record.id}.txt").writeText(record.toString())
    }
}