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

    fun findRecord(ref : String) {
        val url = "$prefix/works?query=${URLEncoder.encode(ref, "utf-8")}&rows=3&mailto=$email"
        val (_, response, result) = url.httpGet().responseString()
        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                ArxivAPI.logger.error(ex)
                ArxivAPI.logger.info("Failed: $ex")
                throw ApiRequestFailedException(ex.message)
            }
            is Result.Success -> {
                println(result.value)
            }
        }
    }

    class ApiRequestFailedException(override val message : String?) : Throwable(message)
}