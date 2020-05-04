package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon

object SemanticScholarJsonParser {
    fun parse(jsonString : String) : SemanticScholarData? {
        return Klaxon().parse<SemanticScholarData>(jsonString)
    }
}