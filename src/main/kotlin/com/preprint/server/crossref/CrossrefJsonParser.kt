package com.preprint.server.crossref

import com.beust.klaxon.Klaxon
import com.preprint.server.data.Author
import com.preprint.server.data.JournalRef

object CrossrefJsonParser {
    fun parse(json : String) : List<CRData> {
        val parsedJson = Klaxon().parse<CrossRefJsonData>(json)
        val items = parsedJson?.message?.items
        if (items != null) {
            val records = mutableListOf<CRData>()
            for (record in items) {
                val crRecord = CRData()
                record.DOI?.let {crRecord.doi = it}
                record.title?.let {crRecord.title = it[0]}
                crRecord.authors.addAll(record.author?.map {auth ->
                    Author(auth.family + " " + auth.given, firstName = auth.given, secondName = auth.family)
                } ?: listOf())
                record.URL?.let {crRecord.pdfUrl = it}
                if (record.container_title != null) {
                    val journal = JournalRef("")
                    record.short_container_title?.let {journal.shortTitle = it[0]}
                    journal.fullTitle = record.container_title[0]
                    record.volume?.let {journal.volume = it}
                    record.page?.let {journal.pages = it}
                    record.issue?.let {journal.number = it}
                    record.ISSN?.let {journal.issn = it[0]}
                    crRecord.journal = journal
                }
                records.add(crRecord)
            }
            return records
        }
        return listOf()
    }
}