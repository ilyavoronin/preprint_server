package com.preprint.server.data

import com.preprint.server.ref.GrobidEngine
import org.grobid.core.data.BibDataSet
import org.grobid.core.data.BiblioItem

class Reference() {
    var rawReference : String = ""
    var arxivId : String? = null
    var doi : String? = null
    var authors : List<Author>? = null
    var title : String? = null
    var journal : String? = null
    var issue : String? = null
    var pages : String? = null
    var volume : String? = null
    var year : String? = null
    var issn : String? = null
    var isReference = false
    var validated = false
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
        authors = p.fullAuthors?.map {author -> Author(author.toString())}
        title = p.title
        journal = p.journal
        issue = p.issue
        pages = p.pageRange
        volume = p.volumeBlock
        year = p.publicationDate
        issn = p.issn
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
        addField("  authors", authors?.joinToString { it.toString() })
        addField("  arxiv id", arxivId)
        addField("  doi", doi)
        addField("  journal", journal)
        addField("  volume", volume)
        addField("  year", year)
        addField("  pages", pages)
        res += "\n\n"
        return res
    }

    companion object {
        fun toReferences(refList: List<String>): List<Reference> {
            val p = GrobidEngine.processRawReferences(refList, 0)
            return refList.zip(p).map { (ref, bib) -> Reference(ref, bib) }
        }
    }
}