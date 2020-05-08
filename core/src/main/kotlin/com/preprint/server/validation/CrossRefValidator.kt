package com.preprint.server.validation

import com.preprint.server.algo.Algorithms
import com.preprint.server.crossref.CRData
import com.preprint.server.crossref.CrossRefApi
import com.preprint.server.data.Reference
import org.apache.logging.log4j.kotlin.logger

object CrossRefValidator : Validator {
    val logger = logger()

    private val distThreshold = 0.05

    override fun validate(ref : Reference) {
        val records = CrossRefApi.findRecord(ref.rawReference)
        for (record in records) {
            if (checkSim(ref, record)) {
                ref.validated = true
                ref.title = record.title
                ref.doi = record.doi
                ref.firstPage = record.journal?.firstPage
                ref.lastPage = record.journal?.lastPage
                ref.issue = record.journal?.number
                ref.volume = record.journal?.volume
                ref.year = record.journal?.year
                ref.issn = record.journal?.issn
                ref.journal = record.journal?.shortTitle ?: record.journal?.fullTitle
                ref.authors = record.authors
                break
            }
        }
    }

    private fun checkSim(ref : Reference, record : CRData) : Boolean {
        if (ref.title != null && ref.title != "") {
            val dist = Algorithms.findLvnstnDist(ref.title!!, record.title)
            val d = dist.toDouble() / ref.title!!.length.toDouble()
            return d < distThreshold
        }
        else {
            val lcs = Algorithms.findLCS(ref.rawReference, record.title).length
            if (lcs.toDouble() / record.title.length > 1 - distThreshold) {
                return true
            }

            var score = 0;
            val j = record.journal ?: return false
            if (ref.issue != null && j.number != null && ref.issue == j.number) {
                score += 1
            }
            if (ref.firstPage != null && j.firstPage != null && ref.firstPage == ref.lastPage) {
                if (ref.lastPage != null && j.lastPage != null) {
                    if (ref.lastPage != j.lastPage) {
                        score += 1
                    }
                }
                else {
                    score += 1
                }
            }
            if (ref.volume != null && j.volume != null && ref.volume == j.volume) {
                score += 1
            }

            if (!record.authors.isNullOrEmpty() &&
                    record.authors.all {it.secondName != null && ref.rawReference.contains(it.secondName)}) {
                score += 1
            }

            if (j.year != null && ref.year != null && j.year == ref.year) {
                score += 1
            }
            return score >= 2
        }
    }
}