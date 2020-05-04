package com.preprint.server.validation.database

data class SemanticScholarData(
    val authors: List<Author> = mutableListOf(),
    val doi: String? = null,
    val id: String? = null,
    val journalName: String? = null,
    var journalPages: String? = null,
    val journalVolume: String? = null,
    var title: String? = null,
    val year: Int? = null,
    var firstPage: Int? = null,
    var lastPage: Int? = null
) {
    data class Author(
            val name: String
    )
}