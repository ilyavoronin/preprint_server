package com.preprint.server.data

import com.preprint.server.ref.Reference

interface PubData {
    var id : String
    var pdfUrl : String
    var refList : MutableList<Reference>
}