package com.preprint.server.arxiv

import com.preprint.server.data.Author
import com.preprint.server.data.PubData
import com.preprint.server.ref.Reference

data class ArxivData(
    val identifier : String,
    var datestamp: String = "",
    var id : String = "",
    override var abstract : String = "",
    var creationDate : String = "",
    override var title : String = "",
    override var year : String? = null,
    var lastUpdateDate : String? = null,
    override val authors : MutableList<Author> = mutableListOf(),
    var categories : MutableList<String> = mutableListOf(),
    var comments : String? = null,
    var reportNo : String? = null,
    override var journalRef : String? = null,
    var mscClass : String? = null,
    var acmClass : String? = null,
    override var doi : String? = null,
    var license : String? = null,
    override var refList : MutableList<Reference> = mutableListOf(),
    override var pdfUrl : String = ""
): PubData
