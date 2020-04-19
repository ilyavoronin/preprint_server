package com.preprint.server.data

import com.preprint.server.ref.Reference

interface PubData {
    var id : String
    var abstract : String
    var title : String
    val authors : MutableList<Author>
    var doi : String?
    var refList : MutableList<Reference>
    var pdfUrl : String
    var journal : JournalRef?
}