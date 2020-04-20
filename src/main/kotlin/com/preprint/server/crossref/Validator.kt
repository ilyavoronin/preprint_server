package com.preprint.server.crossref

import com.preprint.server.algo.LvnstDist
import com.preprint.server.data.Reference

object Validator {
    private val distThreshold = 0.05
    fun validate(refList : List<Reference>) {
        for (ref in refList) {
            validate(ref)
        }
    }

    fun validate(ref : Reference) {
        val records = CrossRefApi.findRecord(ref.rawReference)
        for (record in records) {
            println(record)
            if (checkSim(ref, record)) {
                ref.validated = true
                ref.title = record.title
                ref.doi = record.doi
                ref.pages = record.journal?.pages
                ref.issue = record.journal?.number
                ref.volume = record.journal?.volume
                ref.year = record.journal?.year
                break
            }
        }
    }

    private fun checkSim(ref : Reference, record : CRData) : Boolean {
        if (ref.volume != null && record.journal?.volume != null) {
            if (ref.volume != record.journal?.volume) {
                return false
            }
        }
        if (ref.title != null && ref.title != "") {
            val dist = LvnstDist.findDist(ref.title!!, record.title)
            val d = dist.toDouble() / ref.title!!.length.toDouble()
            return d < distThreshold
        }
        else {
            val j = record.journal
            if (ref.volume == null || j?.volume == null || ref.pages == null || j.pages == null) {
                return false
            }
            if (ref.issue != null && j.number != null) {
                if (ref.issue != j.number) {
                    return false
                }
            }
            val pages1 = ref.pages!!.split("--")
            val pages2 = ref.pages!!.split("-")
            if (pages1[0] != pages2[0]) {
                return false
            }
            if (pages1.size > 1 && pages2.size > 1) {
                if (pages1[1] != pages2[1]) {
                    return false
                }
            }
        }
        return true
    }
}