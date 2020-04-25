package com.preprint.server.arxiv

import com.preprint.server.neo4j.DatabaseHandler
import com.preprint.server.pdf.PdfHandler
import com.preprint.server.ref.CustomReferenceExtractor
import com.preprint.server.validation.ArxivValidator
import com.preprint.server.validation.CrossRefValidator

import org.apache.logging.log4j.kotlin.logger
import java.lang.Thread.sleep

object ArxivCollector {
    val logger = logger()
    var resumptionToken = ""
    const val limit = 100
    var sleepTime : Long = 600000
    fun collect(
        startDate : String,
        dbHandler : DatabaseHandler,
        resumptionToken_ : String = ""
    ) {
        var recordsProcessed = 0
        logger.info("Begin collecting arxiv metadata from $startDate with resumption token:$resumptionToken")
        resumptionToken = resumptionToken_
        do {
            try {
                val (newArxivRecords, newResumptionToken, recordsTotal) =
                    ArxivAPI.getBulkArxivRecords(startDate, resumptionToken, limit)

                resumptionToken = newResumptionToken

                PdfHandler.getFullInfo(
                    newArxivRecords,
                    "files/",
                    CustomReferenceExtractor,
                    listOf(CrossRefValidator, ArxivValidator),
                    false
                )

                dbHandler.storeArxivData(newArxivRecords)

                recordsProcessed += newArxivRecords.size
                logger.info("Records processed ${recordsProcessed} out of $recordsTotal")
            } catch (e: ArxivAPI.ApiRequestFailedException) {
                sleep(sleepTime)
                continue
            }
        } while (resumptionToken != "")
    }
}