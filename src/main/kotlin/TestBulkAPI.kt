package testpdf

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result;
import java.io.File

fun main() {
    val requestURL = "http://export.arxiv.org/oai2?verb=ListRecords&from=2020-03-19&metadataPrefix=arXiv"
    val outputFile = File("response.txt")
    val (request, response, result) = requestURL
        .httpGet()
        .responseString()
    when (result) {
        is Result.Failure -> {
            val ex = result.getException()
            println(ex)
        }
        is Result.Success -> {
            println("Success")
            val data = result.get()
            ArxivXMLParser.parse(data)
            outputFile.writeText(data)
        }
    }
}