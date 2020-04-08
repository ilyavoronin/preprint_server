package com.preprint.server.arxiv

import com.preprint.server.neo4j.DatabaseHandler
import com.preprint.server.pdf.PdfHandler
import com.preprint.server.ref.CustomReferenceExtractor

import org.apache.logging.log4j.kotlin.logger

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