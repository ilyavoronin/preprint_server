package com.preprint.server.crossref

import com.preprint.server.data.Author
import com.preprint.server.data.JournalRef
import com.preprint.server.data.PubData
import com.preprint.server.ref.Reference

data class CRData(
    override var id: String = "",
    override var title: String = "",
    override var doi: String? = null,
    override var journal: JournalRef? = null,
    override var refList: MutableList<Reference> = mutableListOf<Reference>(),
    override var pdfUrl: String = "",
    override val authors: MutableList<Author> = mutableListOf<Author>(),
    override var abstract: String = ""
) : PubData