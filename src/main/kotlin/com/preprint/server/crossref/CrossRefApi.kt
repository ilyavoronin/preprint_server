package com.preprint.server.crossref

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.preprint.server.Config
import com.preprint.server.arxiv.ArxivAPI
import com.preprint.server.arxiv.ArxivXMLParser
import com.preprint.server.data.PubData
import com.preprint.server.utils.RequestLimiter
import org.apache.logging.log4j.kotlin.logger
import java.net.CacheResponse
import java.net.URLEncoder

object CrossRefApi {
    const val prefix = "https://api.crossref.org"
    val email = Config.config["email"].toString()
    val maxRecordsNumber = 5
    val reqLimiter = RequestLimiter(49, 2100)

    fun findRecord(ref : String) : List<CRData> {
        reqLimiter.waitForRequest()
        val url = "$prefix/works?query=${URLEncoder.encode(ref, "utf-8")}&rows=$maxRecordsNumber&mailto=$email"
        val (_, response, result) = url.httpGet().responseString()
        val (newLimit, newInterval) = getNewInterval(response)
        reqLimiter.set(newLimit, newInterval)
        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                ArxivAPI.logger.error(ex)
                ArxivAPI.logger.info("Failed: $ex")
                throw ApiRequestFailedException(ex.message)
            }
            is Result.Success -> {
                val records = CrossrefJsonParser.parse(result.value)
                return records
            }
        }
    }

    fun getNewInterval(response: Response) : Pair<Int, Long> {
        val newLimit =  response.headers.get("X-Rate-Limit-Limit").toList()
        val newInterval = response.headers.get("X-Rate-Limit-Interval").toList()
        if (newLimit.isEmpty() || newInterval.isEmpty()) {
            return Pair(50, 2000.toLong())
        }
        else {
            return Pair(newLimit[0].toInt(), newInterval[0].dropLast(1).toLong() * 1000 * 2 + 50)
        }
    }

    class ApiRequestFailedException(override val message : String?) : Throwable(message)
}