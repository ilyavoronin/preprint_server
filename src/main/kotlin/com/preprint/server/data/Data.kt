package com.preprint.server.data

import com.preprint.server.ref.Reference

interface Data {
    var id : String
    var pdfUrl : String
    var refList : MutableList<Reference>
}