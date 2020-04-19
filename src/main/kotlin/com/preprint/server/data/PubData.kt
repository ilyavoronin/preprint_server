package com.preprint.server.data

import com.preprint.server.ref.Reference

interface PubData {
    var abstract : String
    var year : String?
    var title : String
    val authors : MutableList<Author>
    var journalRef : String?
    var doi : String?
    var refList : MutableList<Reference>
    var pdfUrl : String
}