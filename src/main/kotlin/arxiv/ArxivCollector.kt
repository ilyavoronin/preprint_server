package arxiv

import preprint.server.arxiv.ArxivAPI
import preprint.server.arxiv.ArxivData

import org.apache.logging.log4j.kotlin.logger
import preprint.server.neo4j.DatabaseHandler
import preprint.server.pdf.PdfHandler
import preprint.server.ref.CustomReferenceExtractor

object ArxivCollector {
    val logger = logger()
    var resumptionToken = ""
    fun collect(
        startDate : String,
        dbHandler : DatabaseHandler,
        resumptionToken_ : String = ""
    ) {
        logger.info("Begin collecting arxiv metadata from $startDate with resumption token:$resumptionToken")
        resumptionToken = resumptionToken_
        do {
            val (newArxivRecords, newResumptionToken) = ArxivAPI.getBulkArxivRecords(startDate, resumptionToken)
            resumptionToken = newResumptionToken
            if (newArxivRecords != null) {
                PdfHandler.getFullInfo(newArxivRecords, "files/", CustomReferenceExtractor, false)
                dbHandler.storeArxivData(newArxivRecords)
            }
        } while (resumptionToken != "")
    }
}