package arxiv

import preprint.server.arxiv.ArxivAPI
import preprint.server.arxiv.ArxivData

object ArxivCollector {
    var resumptionToken = ""
    fun collect(startDate : String, resumptionToken_ : String = "") : List<ArxivData> {
        resumptionToken = resumptionToken_
        val arxivRecords = mutableListOf<ArxivData>()
        do {
            val (newArxivRecords, newResumptionToken) = ArxivAPI.getBulkArxivRecords(startDate, resumptionToken)
            resumptionToken = newResumptionToken
            if (newArxivRecords != null) {
                arxivRecords.addAll(newArxivRecords)
            }
        } while (resumptionToken != "")
        return arxivRecords
    }
}