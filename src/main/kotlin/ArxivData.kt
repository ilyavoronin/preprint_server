package testpdf

import java.util.*
import kotlin.math.abs

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
    var pdf = ""
    var refList = listOf<String>()
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
