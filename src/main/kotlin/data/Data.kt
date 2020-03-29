package preprint.server.data

interface Data {
    var id : String
    var pdfUrl : String
    var refList : MutableList<String>
}