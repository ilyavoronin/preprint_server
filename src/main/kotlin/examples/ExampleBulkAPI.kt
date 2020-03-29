package preprint.server.examples

import preprint.server.arxiv.ArxivXMLParser

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result;
import java.io.File

fun main() {
    val requestURL = "http://export.arxiv.org/oai2?verb=ListRecords&from=2020-03-20&metadataPrefix=arXiv"
    val outputXMLFile = File("files/response.xml")
    val outputFile = File("files/metadata.txt")
    outputFile.writeText("")
    val (request, response, result) = requestURL
        .httpGet()
        .responseString()
    when (result) {
        is Result.Failure -> {
            val ex = result.getException()
            println(ex)
            return
        }
        is Result.Success -> {
            println("Success")
            val data = result.get()
            outputXMLFile.writeText(data)
            for (elem in ArxivXMLParser.parseArxivRecords(data)) {
                outputFile.appendText(elem.toString())
            }
        }
    }
}