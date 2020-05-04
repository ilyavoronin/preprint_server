package com.preprint.server.crossref

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.preprint.server.Config
import com.preprint.server.utils.RequestLimiter
import org.apache.logging.log4j.kotlin.logger
import java.net.URLEncoder


/**
 * Used for accessing CrossRef API
 */
object CrossRefApi {
    val logger = logger()
    const val prefix = "https://api.crossref.org"
    val email = Config.config["email"].toString()

    //the number of records that each request returns
    var maxRecordsNumber = 5

    val reqLimiter = RequestLimiter(49, 2100)


    /**
     * Makes request to CrossRef API to find the given record
     * and returns `maxRecordsNumber` most suitable results
     */
    fun findRecord(ref: String): List<CRData> {

        //if there was made too many request, will wait until can make another one
        reqLimiter.waitForRequest()

        val url = "$prefix/works?query=${URLEncoder.encode(ref, "utf-8")}&rows=$maxRecordsNumber&mailto=$email"
        val (_, response, result) = try {
            url.httpGet().timeoutRead(10000).responseString()
        } catch (e : Exception) {
            throw ApiRequestFailedException(e.message)
        }
        val (newLimit, newInterval) = getNewInterval(response)
        reqLimiter.set(newLimit, newInterval)

        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                if (response.statusCode == 414) {
                    //this means that url is too long, and most likely
                    //reference was parsed wrong
                    return listOf()
                }
                else {
                    throw ApiRequestFailedException(ex.message)
                }
            }
            is Result.Success -> {
                val records = CrossrefJsonParser.parse(result.value)
                return records
            }
        }
    }

    private fun getNewInterval(response: Response): Pair<Int, Long> {
        val newLimit =  response.headers.get("X-Rate-Limit-Limit").toList()
        val newInterval = response.headers.get("X-Rate-Limit-Interval").toList()
        if (newLimit.isEmpty() || newInterval.isEmpty()) {
            return Pair(50, 2100.toLong())
        }
        else {
            return Pair(newLimit[0].toInt() - 1, newInterval[0].dropLast(1).toLong() * 1000 * 2 + 100)
        }
    }

    class ApiRequestFailedException(override val message: String?) : Exception(message)
}