package com.preprint.server.data

import com.preprint.server.ref.GrobidEngine
import org.grobid.core.data.BiblioItem

data class JournalRef(
    var rawRef : String,
    var name: String? = null,
    var volume : String? = null,
    var pages : String? = null,
    var year : String? = null,
    var number : String? = null
) {
    constructor(rawRef: String, parse : Boolean) : this(rawRef) {
        getFullJournalInfo(this)
    }

    constructor(bib : BiblioItem, rawRef: String) : this(rawRef) {
        name = bib.journal
        pages = bib.pageRange
        volume = bib.volumeBlock
        year = bib.publicationDate
        number = bib.issue
    }

    companion object {
        fun getFullJournalInfo(journal : JournalRef) {
            val bibitem = GrobidEngine.processRawReference(journal.rawRef, 0)
            journal.name = bibitem.journal
            journal.pages = bibitem.pageRange
            journal.volume = bibitem.volumeBlock
            journal.year = bibitem.publicationDate
            journal.number = bibitem.issue
        }
    }
}