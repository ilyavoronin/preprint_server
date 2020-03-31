package preprint.server.data

import preprint.server.ref.Reference

interface Data {
    var id : String
    var pdfUrl : String
    var refList : MutableList<Reference>
}