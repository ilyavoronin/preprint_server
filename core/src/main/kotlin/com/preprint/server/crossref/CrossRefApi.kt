package com.preprint.server.crossref

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.preprint.server.Config
import com.preprint.server.arxiv.ArxivAPI
import com.preprint.server.utils.RequestLimiter
import org.apache.logging.log4j.kotlin.logger
import java.net.URLEncoder

object CrossRefApi {
    val logger = logger()
    const val prefix = "https://api.crossref.org"
    val email = Config.config["email"].toString()
    val maxRecordsNumber = 5
    val reqLimiter = RequestLimiter(49, 2100)

    fun findRecord(ref : String) : List<CRData> {
        reqLimiter.waitForRequest()
        val url = "$prefix/works?query=${URLEncoder.encode(ref, "utf-8")}&rows=$maxRecordsNumber&mailto=$email"
        val (_, response, result) = try {
            url.httpGet().timeoutRead(10000).responseString()
        } catch (e : Exception) {
            logger.error("Failed to validate: ${e.message}")
            throw ApiRequestFailedException(e.message)
        }
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
            return Pair(50, 2100.toLong())
        }
        else {
            return Pair(newLimit[0].toInt() - 1, newInterval[0].dropLast(1).toLong() * 1000 * 2 + 100)
        }
    }

    class ApiRequestFailedException(override val message : String?) : Throwable(message)
}