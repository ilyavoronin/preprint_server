package testpdf

import java.util.*

data class Author(val name : String, val affiliation : String? = null)

data class ArxivData(val id : String,
                     val datestamp : Date,
                     val specs : List<Int>,
                     val creationDate : Date,
                     val lastUpdateDate : Date,
                     val authors : List<Author>,
                     val title : String,
                     val categories : String,
                     val comments : String,
                     val mscClass : String,
                     val journalRef : String,
                     val doi : String,
                     val license : String,
                     val abstract : String

)
