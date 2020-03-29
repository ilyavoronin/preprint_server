package preprint.server.arxiv

import preprint.server.data.Data

data class ArxivData(val identifier : String) : Data {
    data class Author(val name : String, val affiliation : String? = null)

    override var id = ""
    override var refList = mutableListOf<String>()
    override var pdfUrl = ""

    var datestamp = ""
    val specs = mutableListOf<String>()
    var creationDate = ""
    var lastUpdateDate : String? = null
    val authors = mutableListOf<Author>()
    var title = ""
    var categories = mutableListOf<String>()
    var comments : String? = null
    var mscClass : String? = null
    var journalRef : String? = null
    var doi : String? = null
    var license = ""
    var abstract = ""
    override fun toString(): String {
        return """id: $id
Creation date: $creationDate
Authors:
${authors.foldIndexed(""){i, acc, author -> "$acc${i + 1}) ${author.name}\n"}}
Abstract:
$abstract
References:
${refList.foldIndexed("") { i, acc, ref -> "$acc${i + 1}) $ref\n" }}
        """
    }
}
