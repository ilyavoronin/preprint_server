package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon

object SemanticScholarJsonParser {
    fun parse(jsonString : String) : UniversalData? {
        return Klaxon().parse<UniversalData>(jsonString)
    }
}