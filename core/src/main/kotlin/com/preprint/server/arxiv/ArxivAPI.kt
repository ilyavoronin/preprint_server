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
    var sleepTime : Long = 600000
    val timeout = 60000

    //only 1000 records
    fun getBulkArxivRecords(startDate : String, resumptionToken : String, limit : Int = 100000) : Triple<List<ArxivData>, String, Int> {
        logger.info("Begin api request from $startDate")
        logger.info("Using resumption token: $resumptionToken")
        val requestURL = when(resumptionToken) {
                ""   -> requestBulkUrlPrefix +
                            "verb=ListRecords&from=$startDate&metadataPrefix=arXiv"
                else -> requestBulkUrlPrefix +
                            "verb=ListRecords&resumptionToken=$resumptionToken"
        }
        val (_, response, result) = try {
            requestURL.httpGet().timeoutRead(timeout).responseString()
        } catch (e: Exception) {
            throw ApiRequestFailedException(e.message)
        }
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                if (response.statusCode == 503) {
                    logger.info("ArXiv OAI service is temporarily unavailable")
                    logger.info("Waiting 600 seconds")
                    sleep(sleepTime)
                    getBulkArxivRecords(startDate, resumptionToken, limit)
                }
                else {
                    logger.error(ex)
                    logger.info("Failed: $ex")
                    throw ApiRequestFailedException(ex.message)
                }
            }
            is Result.Success -> {
                logger.info("Success")
                val data = result.get()
                val (arxivRecords, newResumptionToken, recordsTotal) = ArxivXMLDomParser.parseArxivRecords(data)
                if (resumptionToken == "") {
                    logger.info("Total records: ${recordsTotal}")
                }
                logger.info("Receive ${arxivRecords.size} records")
                val pdfLinks = try {
                    getRecordsLinks(arxivRecords.map { arxivData -> arxivData.id })
                } catch (e : ApiRequestFailedException) {
                    //try one more time
                    getRecordsLinks(arxivRecords.map { arxivData -> arxivData.id })
                }
                for ((arxivData, pdfLink) in arxivRecords.zip(pdfLinks)) {
                    arxivData.pdfUrl = pdfLink
                }
                Triple(arxivRecords.take(limit), newResumptionToken, recordsTotal)
            }
        }
    }

    fun getRecordsLinks(idList : List <String>) : List<String> {
        val metadata = getArxivMetadata(idList)
        val records = ArxivXMLDomParser.getPdfLinks(metadata)
        if (records.size != idList.size) {
            throw ApiRequestFailedException(
                ("The number of records received from arxiv api(${records.size})" +
                        "differs from the number of ids(${idList.size})")
            )
        }
        return records
    }

    fun getArxivRecords(idList : List <String>) : List<ArxivData> {
        val metadata = getArxivMetadata(idList)
        val records = ArxivXMLSaxParser.parse(metadata)
        if (records.size != idList.size) {
            throw ApiRequestFailedException(
                ("The number of records received from arxiv api(${records.size})" +
                        "differs from the number of ids(${idList.size})")
            )
        }
        records.forEachIndexed {i, record -> record.id = idList[i]}
        return records
    }

    fun getArxivMetadata(idList : List <String>) : String {
        logger.info("Begin api request to get arxiv metadata for ${idList.size} records")
        val idString = idList.foldIndexed("") {i, acc, s ->
            if (i < idList.lastIndex)"$acc$s," else "$acc$s"
        }
        val (_, _, result) = requestApiUrlPrefix
            .httpPost(listOf("id_list" to idString, "max_results" to idList.size.toString()))
            .timeoutRead(timeout)
            .responseString()
        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                logger.error("Failed: $ex")
                throw ApiRequestFailedException(ex.message)
            }
            is Result.Success -> {
                logger.info("Success: receive metadata")
                result.get()
            }
        }
    }

    class ApiRequestFailedException(message : String? = null) : Throwable(message)
}