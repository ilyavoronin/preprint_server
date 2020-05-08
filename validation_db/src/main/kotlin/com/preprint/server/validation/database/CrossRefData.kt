package com.preprint.server.validation.database

import com.beust.klaxon.Json
import com.jsoniter.annotation.JsonProperty

data class CrossRefData(
    val DOI: String? = null,
    val author: MutableList<Author> = mutableListOf(),
    @field:JsonProperty("container-title")
    val container_title: MutableList<String> = mutableListOf(),
    val issue: String? = null,
    val issued: Issued? = null,
    val link: MutableList<Link> = mutableListOf(),
    val page: String? = null,
    @field:JsonProperty("short-container-title")
    val short_container_title: MutableList<String> = mutableListOf(),
    val title: MutableList<String> = mutableListOf(),
    val volume: String? = null
) {
    data class Author(
        val affiliation: MutableList<Any> = mutableListOf(),
        val family: String? = null,
        val given: String? = null,
        val sequence: String? = null
    )

    data class Issued(
        @field:JsonProperty("date-parts")
        val date_parts: MutableList<List<Int>> = mutableListOf()
    )

    data class Link(
        val URL: String? = null
    )
}