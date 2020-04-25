package com.preprint.server.validation

import com.preprint.server.algo.LCS
import com.preprint.server.algo.LvnstDist
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
                ref.pages = record.journal?.pages
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
            val dist = LvnstDist.findDist(ref.title!!, record.title)
            val d = dist.toDouble() / ref.title!!.length.toDouble()
            return d < distThreshold
        }
        else {
            val lcs = LCS.find(ref.rawReference, record.title).length
            if (lcs.toDouble() / record.title.length > 1 - distThreshold) {
                return true
            }

            var score = 0;
            val j = record.journal ?: return false
            if (ref.issue != null && j.number != null && ref.issue == j.number) {
                score += 1
            }
            val pages1 = ref.pages?.split("--")
            val pages2 = j.pages?.split("-")
            if (pages1 != null && pages2 != null && pages1[0] == pages2[0]) {
                if (pages1.size > 1 && pages2.size > 1) {
                    if (pages1[1] == pages2[1]) {
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