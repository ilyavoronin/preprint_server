package com.preprint.server.validation.database

data class UniversalData(
    val authors: List<Author> = mutableListOf(),
    val pmid: String? = null,
    val doi: String? = null,
    val ssid: String? = null,
    val journalName: String? = null,
    var journalPages: String? = null,
    var journalVolume: String? = null,
    var title: String? = null,
    val year: Int? = null,
    var firstPage: Int? = null,
    var lastPage: Int? = null,
    var issue: String? = null,
    val pdfUrls: MutableList<String> = mutableListOf()
) {
    data class Author(
        val name: String
    )
}