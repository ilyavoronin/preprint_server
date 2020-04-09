package com.preprint.server.arxiv

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.apache.logging.log4j.kotlin.logger
import java.lang.Thread.sleep

object ArxivAPI {

    val logger = logger()
    const val requestBulkUrlPrefix = "http://export.arxiv.org/oai2?"
    const val requestApiUrlPrefix = "http://export.arxiv.org/api/query"
    var recordsProcessed = 0

    //only 1000 records
    fun getBulkArxivRecords(startDate : String, resumptionToken : String, limit : Int = 100000) : Triple<List<ArxivData>?, String, Int> {
        if (resumptionToken == "") {
            recordsProcessed = 0
        }
        logger.info("Begin api request from $startDate")
        logger.info("Using resumption token: $resumptionToken")
        val requestURL = when(resumptionToken) {
                ""   -> requestBulkUrlPrefix +
                            "verb=ListRecords&from=$startDate&metadataPrefix=arXiv"
                else -> requestBulkUrlPrefix +
                            "verb=ListRecords&resumptionToken=$resumptionToken"
        }
        val (_, response, result) = requestURL.httpGet().timeoutRead(60000).responseString() //TODO handle timeout exception
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                if (response.statusCode == 503) {
                    logger.info("ArXiv OAI service is temporarily unavailable")
                    logger.info("Waiting 600 seconds")
                    sleep(600000)
                    getBulkArxivRecords(startDate, resumptionToken, limit)
                }
                else {
                    logger.error(ex)
                    logger.info("Failed: $ex")
                    Triple(null, resumptionToken, 0)
                }
            }
            is Result.Success -> {
                logger.info("Success")
                val data = result.get()
                val (arxivRecords, newResumptionToken, recordsTotal) = ArxivXMLParser.parseArxivRecords(data)
                if (resumptionToken == "") {
                    logger.info("Total records: ${recordsTotal}")
                }
                logger.info("Receive ${arxivRecords.size} records")
                val pdfLinks = getRecordsLinks(arxivRecords.map { arxivData -> arxivData.id })!!
                for ((arxivData, pdfLink) in arxivRecords.zip(pdfLinks)) {
                    arxivData.pdfUrl = pdfLink
                }
                Triple(arxivRecords.take(limit), newResumptionToken, recordsTotal)
            }
        }
    }

    fun getRecordsLinks(idList : List <String>) : List<String>? {
        logger.info("Begin api request to get pdf urls")
        val idString = idList.foldIndexed("") {i, acc, s ->
            if (i < idList.lastIndex)"$acc$s," else "$acc$s"
        }
        val (_, _, result) = requestApiUrlPrefix
            .httpPost(listOf("id_list" to idString, "max_results" to "1000"))
            .timeoutRead(60000)
            .responseString()
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                logger.error("Failed: $ex")
                null
            }
            is Result.Success -> {
                logger.info("Success: receive pdf urls")
                val data = result.get()
                ArxivXMLParser.getPdfLinks(data)
            }
        }
    }
}