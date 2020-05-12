package com.preprint.server.validation.database

import com.jsoniter.JsonIterator

object SSJsonParser {
    fun parse(json: String): UniversalData? {
        JsonIterator.deserialize(json, UniversalData::class.java)?.let {
            return it
        }
        return null
    }
}