package com.preprint.server.validation.database

import com.jsoniter.JsonIterator
import java.lang.Exception

object CrossRefJsonParser {
    fun parse(json: String): UniversalData? {
        JsonIterator.deserialize(json, CrossRefData::class.java)?.let {
            return toUniversalData(it)
        }
        return null
    }

    private fun toUniversalData(record: CrossRefData): UniversalData {
        val authors = record.author.map {
            UniversalData.Author(it.given + " " + it.family)
        }
        val journal = record.short_container_title.getOrElse(
                0,
                {record.container_title.getOrNull(0)}
        )
        val spages = record.page?.split("-")
        var firstPage: Int? = null
        var lastPage: Int? = null
        if (spages != null) {
            if (spages.size >= 1) {
                firstPage = spages[0].toIntOrNull()
            }
            if (spages.size >= 2) {
                lastPage = spages[1].toIntOrNull()
            }
        }
        val dateParts = record.issued?.date_parts?.getOrNull(0)
        var year: Int? = null
        if (!dateParts.isNullOrEmpty()) {
            try {
                dateParts.forEach {
                    if (it.toString().length == 4) {
                        year = it
                    }
                }
            } catch (e: Exception) {
                year = null
            }
        }
        return UniversalData(
                authors = authors,
                doi = record.DOI,
                journalName = journal,
                journalPages = record.page,
                firstPage = firstPage,
                lastPage = lastPage,
                journalVolume = record.volume,
                title = record.title.getOrNull(0),
                year = year,
                issue = record.issue,
                pdfUrls = record.link.map {link -> link.URL ?: ""}.filter {it.isNotBlank()}.toMutableList()
        )
    }
}