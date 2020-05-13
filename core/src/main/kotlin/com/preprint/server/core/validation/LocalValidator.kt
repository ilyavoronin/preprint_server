package com.preprint.server.core.validation

import com.preprint.server.core.algo.Algorithms
import com.preprint.server.core.data.Author
import com.preprint.server.core.data.Reference
import com.preprint.server.validation.database.Config
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.UniversalData
import org.apache.logging.log4j.kotlin.logger
import java.lang.Math.abs

object LocalValidator : Validator, AutoCloseable {
    val logger = logger()
    val dbHandler =
        DBHandler(Config.config["validation_db_path"].toString())

    override fun validate(ref: Reference) {
        val records = mutableSetOf<UniversalData>()
        if (!ref.title.isNullOrBlank()) {
            records.addAll(dbHandler.getByTitle(ref.title!!))
            if (records.size == 1) {
                if (ref.title!!.length > 20 && checkByTitle(ref, records.first())) {
                    accept(ref, records.first())
                    return
                }
            }
        }

        if (!ref.volume.isNullOrBlank() && ref.firstPage != null && ref.authors.isNotEmpty()) {
            val auth = DBHandler.getFirstAuthorLetters(ref.authors.map {it.name})
            if (ref.year != null) {
                records.addAll(dbHandler.getByAuthVolPageYear(auth, ref.volume!!, ref.firstPage!!, ref.year!!))
            }
            if (ref.lastPage != null) {
                records.addAll(dbHandler.getByAuthFirsLastPageVolume(auth, ref.firstPage!!, ref.lastPage!!, ref.volume!!))
            }
        }


        if (ref.authors.size >= 2 && ref.year != null) {
            val authString = DBHandler.getFirstAuthorLetters(ref.authors.map {it.name})

            if (!ref.volume.isNullOrBlank()) {
                records.addAll(dbHandler.getByAuthorVolume(authString, ref.volume!!, ref.year!!))
            }

            if (ref.firstPage != null) {
                records.addAll(dbHandler.getByAuthorPage(authString, ref.firstPage!!, ref.year!!))
            }
        }


        records.forEach {
            if (check(ref, it)) {
                accept(ref, it)
                return
            }
        }
    }

    private fun check(ref: Reference, record: UniversalData): Boolean {
        val refstr = ref.rawReference
        var score = 4
        if (ref.year != null && record.year != null && abs(record.year!! - ref.year!!) > 1) {
            return false
        }

        if (record.year != null
                && record.year != ref.year
        ) {
            score -= 1
        }

        if (record.journalVolume != null
                && record.journalVolume != ref.volume
        ) {
            score -= 1
        }

        if (record.firstPage != null
                && record.firstPage != ref.firstPage
        ) {
            score -= 1
        }

        //checking authors
        if (!record.authors.all {author ->
                containsAuthor(author.name, refstr)
            }
        ) {
            score -= 2
        }

        println("score: $score")

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
            println(t)
            println(record.journalName)
            if (t.toDouble() / record.journalName!!.length.toDouble() > 0.8) {
                score += 1
            }
        }

        if (record.issue != null) {
            if (record.issue == ref.issue) {
                score += 1
            }
        }

        if (!record.title.isNullOrBlank()) {
            val t = Algorithms.findLCS(refstr, record.title!!).length
            if (t.toDouble() / record.title!!.length.toDouble() > 0.9) {
                score += 2
            }
        }
        if (refstr.contains(record.firstPage.toString())) {
            score += 1
        }
        return score >= 4
    }

    fun checkByTitle(ref: Reference, record: UniversalData): Boolean {
        if (
            record.authors.all { containsAuthor(it.name, ref.rawReference)}
            && ref.rawReference.contains(record.year.toString())
        ) {
            return true
        }

        if (ref.title?.trim() == record.title?.trim()) {
            return true
        }

        return false
    }

    private fun containsAuthor(author: String, refstr: String): Boolean {
        val authorParts = author.split("""\s""".toRegex()).filter { it.isNotBlank() }
        val longestPart = authorParts.maxBy { it.length }
        if (longestPart != null) {
            return Algorithms.findLCS(refstr, longestPart).length + 1 >= longestPart.length
                    || (authorParts.last().all {it.isLetter()} && authorParts.last().length > 1 &&
                    Algorithms.findLCS(refstr, authorParts.last()).length + 1 >= authorParts.last().length)
        }
        else {
            return false
        }
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