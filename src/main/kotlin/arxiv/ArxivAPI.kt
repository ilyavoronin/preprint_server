package preprint.server.arxiv

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result;

object ArxivAPI {
    //only 1000 for now
    const val requestBulkUrlPrefix = "http://export.arxiv.org/oai2?"
    const val requestApiUrlPrefix = "http://export.arxiv.org/api/query"

    fun getBulkArxivRecords(startDate : String, resumptionToken : String) : Pair<List<ArxivData>?, String> {
        println("$resumptionToken")
        val requestURL = when(resumptionToken) {
                ""   -> requestBulkUrlPrefix +
                            "verb=ListRecords&from=$startDate&metadataPrefix=arXiv"
                else -> requestBulkUrlPrefix +
                            "verb=ListRecords&resumptionToken=$resumptionToken"
        }
        val (_, _, result) = requestURL.httpGet().responseString()
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                println(ex)
                Pair(null, resumptionToken)
            }
            is Result.Success -> {
                println("Success")
                val data = result.get()
                val (arxivRecords, newResumptionToken) = ArxivXMLParser.parseArxivRecords(data)
                val pdfLinks = getRecordsLinks(arxivRecords.map { arxivData -> arxivData.id })!!
                for ((arxivData, pdfLink) in arxivRecords.zip(pdfLinks)) {
                    arxivData.pdfUrl = pdfLink
                }
                Pair(arxivRecords, newResumptionToken)
            }
        }
    }

    fun getRecordsLinks(idList : List <String>) : List<String>? {
        val idString = idList.foldIndexed("") {i, acc, s ->
            if (i < idList.lastIndex)"$acc$s," else "$acc$s"
        }
        val (_, _, result) = requestApiUrlPrefix
            .httpPost(listOf("id_list" to idString, "max_results" to "1000"))
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
                ArxivXMLParser.getPdfLinks(data)
            }
        }
    }
}