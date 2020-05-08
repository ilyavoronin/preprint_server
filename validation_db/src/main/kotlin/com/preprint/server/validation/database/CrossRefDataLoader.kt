package com.preprint.server.validation.database

import com.jsoniter.JsonIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.logging.log4j.kotlin.logger
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Exception

object CrossRefDataLoader {
    private val logger = logger()
    val filePath = Config.config["crossref_path_to_file"].toString()
    val textStream = getTextStream(File(filePath))
    val bulkRecodsNumber = 1_000_000

    fun loadData(dbHandler: DBHandler) {
        var recordProcessed = 0
        while (true) {
            logger.info("Begin parsing next ${bulkRecodsNumber} records")
            val (records, isEOF) = getNextRecords()
            logger.info("Begin storing ${records.size} records to the database")
            dbHandler.storeRecords(records)
            logger.info(dbHandler.stats)
            recordProcessed += records.size
            logger.info("Records processed $recordProcessed")
            if (isEOF) {
                break
            }
        }
    }

    private fun getTextStream(file: File): BufferedReader {
        val inputStream = FileInputStream(file)
        val gzipStream = XZCompressorInputStream(inputStream)
        val decoder = InputStreamReader(gzipStream, "utf-8")
        return BufferedReader(decoder, 262144)
    }

    private fun getNextRecords(): Pair<List<UniversalData>, Boolean> {
        val records = mutableListOf<UniversalData>()
        var isEOF = false
        runBlocking(Dispatchers.IO) {
            for (i in 0 until bulkRecodsNumber) {
                val line = textStream.readLine()
                if (line == null) {
                    isEOF = true
                    break
                }
                JsonIterator.deserialize(line.toByteArray(), CrossRefData::class.java)?.let {
                    records.add(toUniversalData(it))
                }
            }
        }
        return Pair(records, isEOF)
    }

    private fun toUniversalData(record: CrossRefData): UniversalData {
        val authors = record.author.map {
            UniversalData.Author(it.given + " " + it.family)
        }
        val journal = record.short_container_title.getOrElse(
            0,
            {record.container_title.getOrNull(0)}
        )
        val spages = record.page?.split("-")
        var firstPage: Int? = null
        var lastPage: Int? = null
        if (spages != null) {
            if (spages.size >= 1) {
                firstPage = spages[0].toIntOrNull()
            }
            if (spages.size >= 2) {
                lastPage = spages[1].toIntOrNull()
            }
        }
        val dateParts = record.issued?.date_parts?.getOrNull(0)
        var year: Int? = null
        if (!dateParts.isNullOrEmpty()) {
            try {
                dateParts.forEach {
                    if (it.toString().length == 4) {
                        year = it
                    }
                }
            } catch (e: Exception) {
                year = null
            }
        }
        return UniversalData(
            authors = authors,
            doi = record.DOI,
            journalName = journal,
            journalPages = record.page,
            firstPage = firstPage,
            lastPage = lastPage,
            journalVolume = record.volume,
            title = record.title.getOrNull(0),
            year = year,
            issue = record.issue,
            pdfUrls = record.link.map {link -> link.URL ?: ""}.filter {it.isNotBlank()}.toMutableList()
        )
    }
}