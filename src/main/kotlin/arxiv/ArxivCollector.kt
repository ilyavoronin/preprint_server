package arxiv

import preprint.server.arxiv.ArxivAPI
import preprint.server.arxiv.ArxivData

import org.apache.logging.log4j.kotlin.logger

object ArxivCollector {
    val logger = logger()
    var resumptionToken = ""
    fun collect(startDate : String, resumptionToken_ : String = "") : List<ArxivData> {
        logger.info("Begin collecting arxiv metadata from $startDate with resumption token:$resumptionToken")
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