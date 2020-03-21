package testpdf

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result;
import java.io.File

object ArxivBulkAPI {
    //only 1000 for now
    val requestURLprefix = "http://export.arxiv.org/oai2?"
    fun getArxivRecords(date : String) : List<ArxivData>? {
        val requestURL = requestURLprefix + "verb=ListRecords&from=$date&metadataPrefix=arXiv"
        val (request, response, result) = requestURL
            .httpGet()
            .responseString()
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                println(ex)
                null
            }
            is Result.Success -> {
                println("Success")
                val data = result.get()
                ArxivXMLParser.parse(data)
            }
        }
    }
}