package com.preprint.server.crossref

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.preprint.server.Config
import com.preprint.server.arxiv.ArxivAPI
import com.preprint.server.arxiv.ArxivXMLParser
import com.preprint.server.data.PubData
import java.net.URLEncoder

object CrossRefApi {
    const val prefix = "https://api.crossref.org"
    val email = Config.config["email"].toString()
    val maxRecordsNumber = 3

    fun findRecord(ref : String) : List<CRData> {
        val url = "$prefix/works?query=${URLEncoder.encode(ref, "utf-8")}&rows=$maxRecordsNumber&mailto=$email"
        val (_, response, result) = url.httpGet().responseString()
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

    class ApiRequestFailedException(override val message : String?) : Throwable(message)
}