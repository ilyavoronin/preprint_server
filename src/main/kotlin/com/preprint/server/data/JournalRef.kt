package com.preprint.server.data

import com.preprint.server.ref.GrobidEngine

data class JournalRef(
    var rawRef : String,
    var name: String? = null,
    var volume : String? = null,
    var pages : String? = null,
    var year : String? = null
) {
    constructor(rawRef: String, parse : Boolean) : this(rawRef) {
        GrobidEngine.getFullJournalInfo(this)
    }
}