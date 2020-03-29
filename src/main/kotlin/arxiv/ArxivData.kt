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
    var title = ""
    val authors = mutableListOf<Author>()
    var categories = mutableListOf<String>()
    var comments : String? = null
    var reportNo : String? = null
    var journalRef : String? = null
    var mscClass : String? = null
    var acmClass : String? = null
    var doi : String? = null
    var license = ""
    var abstract = ""

    override fun toString(): String {
        var res = ""
        res += "id : $id\n"

        res += "Creation date: $creationDate\n"

        if (lastUpdateDate != null) {
            res += "Update date: $lastUpdateDate\n"
        }

        res += "Title: $title\n"

        res += "Authors:\n"
        res += authors.foldIndexed("") { i, acc, author -> "  $acc${i + 1}) ${author.name}\n" }

        res += "Categories:\n"
        res += categories.foldIndexed("") { i, acc, category -> "  $acc${i + 1}) ${category}\n" }
        if (comments != null) {
            res += "Comments:\n$comments\n"
        }
        if (reportNo != null) {
            res += "Report number: $reportNo\n"
        }
        if (journalRef != null) {
            res += "Journal reference: $journalRef\n"
        }
        if (mscClass != null) {
            res += "MSC class: $mscClass\n"
        }
        if (acmClass != null) {
            res += "ACM class: $acmClass\n"
        }
        if (doi != null) {
            res += "DOI: $doi\n"
        }
        res += "License: $license\n"

        res += "PDF url: $pdfUrl\n"

        res += "References:\n"
        res += refList.foldIndexed("") { i, acc, reference -> "  $acc${i + 1}) ${reference}\n" }

        return res + "\n\n"
    }
}
