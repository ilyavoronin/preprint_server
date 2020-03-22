package testpdf

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import java.io.File
import com.github.kittinunf.result.Result;

fun main() {
    val folderPath = "/home/ilya/projects/pdf_test/files/test/"
    val arxivRecords = ArxivAPI.getBulkArxivRecords("2020-03-20")!!.subList(0, 100)
    PdfHandler.getFullInfo(arxivRecords, folderPath)
    for (record in arxivRecords) {
        File("${folderPath}${record.id}.txt").writeText(record.toString())
    }
}