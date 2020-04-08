package com.preprint.server.ref

import org.grobid.core.data.BibDataSet
import org.grobid.core.data.BiblioItem

class Reference() {
    var rawReference : String = ""
    var arxivId : String? = null
    var doi : String? = null
    var booktitle : String? = null
    var address : String? = null
    var authors : List<String>? = null
    var title : String? = null
    var isbn10 : String? = null
    var isbn13 : String? = null
    var journal : String? = null
    var pubnum : String? = null
    var pages : String? = null
    var volume : String? = null
    var year : String? = null
    var isReference = false
    constructor(ref : String, shouldParse : Boolean = false) : this() {
        rawReference = ref
        if (shouldParse) {
            val p = GrobidEngine.processRawReference(rawReference, 1)
            setBib(p)
        }
    }
    constructor(bibData : BibDataSet) : this() {
        val p = bibData.resBib
        rawReference = bibData.rawBib.replace("\n", "")
        setBib(p)
    }
    constructor(rawRef : String, bib : BiblioItem) : this() {
        rawReference = rawRef
        setBib(bib)
    }
    private fun setBib(p : BiblioItem) {
        arxivId = p.arXivId
        doi = p.doi
        booktitle = p.bookTitle
        address = p.address
        authors = p.fullAuthors?.map {author -> author.toString() ?: ""}
        title = p.title
        isbn10 = p.isbN10
        isbn13 = p.isbN13
        journal = p.journal
        pubnum = p.pubnum
        pages = p.pageRange
        volume = p.volumeBlock
        year = p.publicationDate
        isReference = !p.rejectAsReference()
    }

    override fun toString() : String {
        var res = "record\n"
        fun addField(field: String, value : String?) {
            if (value != null) {
                res += "$field: $value\n"
            }
        }
        addField("raw:", rawReference)
        addField("  title", title)
        addField("  book title", booktitle)
        addField("  authors", authors?.joinToString { it })
        addField("  arxiv id", arxivId)
        addField("  doi", doi)
        addField("  journal", journal)
        addField("  volume", volume)
        addField("  year", year)
        addField("  pages", pages)
        addField( " adress", address)
        addField("  isbn10", isbn10)
        addField("  isbn13", isbn13)
        addField("  pubnum", pubnum)
        res += "\n\n"
        return res
    }

    companion object {
        fun toReferences(refList: List<String>): List<Reference> {
            val p = GrobidEngine.processRawReferences(refList, 1)
            return refList.zip(p).map { (ref, bib) -> Reference(ref, bib) }
        }
    }
}