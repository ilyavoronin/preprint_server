package testpdf

import java.util.*

data class Author(val name : String, val affiliation : String? = null)

data class ArxivData(val identifier : String) {
    var id = ""
    var datestamp = ""
    val specs = mutableListOf<String>()
    var creationDate = ""
    var lastUpdateDate : String? = null
    val authors = mutableListOf<Author>()
    var title = ""
    var categories = listOf<String>()
    var comments : String? = null
    var mscClass : String? = null
    var journalRef : String? = null
    var doi : String? = null
    var license = ""
    var abstract = ""
}
