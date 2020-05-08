package com.preprint.server.validation

import com.preprint.server.algo.Algorithms
import com.preprint.server.data.Author
import com.preprint.server.data.Reference
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.UniversalData

object FastValidator : Validator, AutoCloseable {
    val dbHandler = DBHandler()

    override fun validate(ref: Reference) {
        val ids = mutableSetOf<Long>()
        if (!ref.title.isNullOrBlank()) {
            ids.addAll(dbHandler.getByTitle(ref.title!!.toLowerCase()))
        }

        if (!ref.journal.isNullOrBlank() && ref.firstPage != null) {
            ids.addAll(dbHandler.getByJNamePage(ref.journal!!, ref.firstPage!!))
        }

        if (!ref.volume.isNullOrBlank() && ref.firstPage != null) {
            if (ref.year != null) {
                ids.addAll(dbHandler.getByVolPageYear(ref.volume!!, ref.firstPage!!, ref.year!!))
            }
            if (ref.lastPage != null) {
                ids.addAll(dbHandler.getByFirsLastPageVolume(ref.firstPage!!, ref.lastPage!!, ref.volume!!))
            }
        }

        if (ref.authors.size >= 2) {
            val authString = dbHandler.getFirstAuthorLetters(ref.authors.map {it.name})
            if (!ref.volume.isNullOrBlank()) {
                ids.addAll(dbHandler.getByAuthorVolume(authString, ref.volume!!))
            }

            if (ref.year != null) {
                ids.addAll(dbHandler.getByAuthorYear(authString, ref.year!!))
            }

            if (ref.firstPage != null) {
                ids.addAll(dbHandler.getByAuthorPage(authString, ref.firstPage!!))
            }

            if (ref.authors.size >= 3) {
                ids.addAll(dbHandler.getByAuthors(authString))
            }
        }

        val records = dbHandler.mgetById(ids.toList()).filter { it != null }

        records.forEach {
            if (check(ref, it!!)) {
                accept(ref, it)
                return
            }
        }
    }

    private fun check(ref: Reference, record: UniversalData): Boolean {
        val refstr = ref.rawReference
        var score = 3
        if (record.year != null
                && record.year != ref.year
                && !refstr.contains(record.year.toString())
        ) {
            score -= 1
        }

        if (record.journalVolume != null
                && record.journalVolume != ref.volume
                && !refstr.contains(record.journalVolume!!)
        ) {
            score -= 1
        }

        if (record.firstPage != null
                && record.firstPage != ref.firstPage
                && !refstr.contains(record.firstPage!!.toString())
        ) {
            score -= 1
        }

        if (score < 2) {
            return false
        }

        if (record.lastPage != null) {
            if (record.lastPage == ref.lastPage) {
                score += 1
            }
        }

        if (record.journalName != null) {
            val t = Algorithms.findLCS(refstr, record.journalName!!).length
            if (t.toDouble() / record.journalName!!.length.toDouble() > 0.9) {
                score += 1
            }
        }

        if (record.issue != null) {
            if (record.issue == ref.issue) {
                score += 1
            }
        }

        if (record.title.isNullOrBlank()) {
            val t = Algorithms.findLCS(refstr, record.title!!).length
            if (t.toDouble() / record.title!!.length.toDouble() > 0.9) {
                score += 2
            }
        }
        return score >= 5
    }

    private fun accept(ref: Reference, record: UniversalData) {
        ref.title = record.title
        ref.authors = record.authors.map { Author(it.name) }
        ref.pmid = record.pmid
        ref.ssid = record.ssid
        ref.doi = record.doi
        ref.journal = record.journalName
        ref.firstPage = record.firstPage
        ref.lastPage = record.lastPage
        ref.volume = record.journalVolume
        ref.year = record.year
        ref.issue = record.issue
        ref.urls = record.pdfUrls
        ref.validated = true
        ref.isReference = true
    }

    override fun close() {
        dbHandler.close()
    }
}