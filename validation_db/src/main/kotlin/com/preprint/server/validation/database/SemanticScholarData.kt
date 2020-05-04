package com.preprint.server.validation.database

data class SemanticScholarData(
    val authors: List<Author> = mutableListOf(),
    val doi: String? = null,
    val id: String? = null,
    val journalName: String? = null,
    val journalPages: String? = null,
    val journalVolume: String? = null,
    val title: String? = null,
    val year: Int? = null
) {
    data class Author(
            val name: String
    )
}