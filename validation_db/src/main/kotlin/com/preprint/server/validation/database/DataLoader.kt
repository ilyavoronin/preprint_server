package com.preprint.server.validation.database

import org.apache.logging.log4j.kotlin.logger
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object DataLoader {
    private val logger = logger()
    val path = Config.config["semsch_path_to_files"].toString()
    private val cntPath = File(path, "START.txt")
    private var startFrom: Int

    init {
        if (cntPath.exists()) {
            startFrom = cntPath.readText().toInt()
        }
        else {
            startFrom = 1
        }
    }

    fun loadData(dbHandler: DBHandler, start: Int? = null) {
        if (start != null) {
            startFrom = start
        }
        val files = File(path).listFiles().filter {it.isFile && it.name.endsWith(".gz")}
        for ((i, file) in files.withIndex()) {
            if (i + 1 < startFrom) {
                continue
            }
            logger.info("Begin extract records from ${i + 1} archive out of ${files.size}")
            val records = processFile(file).filter { validate(it)}
            logger.info("Begin storing ${records.size} records to the database")
            records.forEach { format(it) }
            dbHandler.storeRecords(records)
            logger.info(dbHandler.stats)

            cntPath.writeText((i + 2).toString())
        }
    }

    private fun getTextStream(file : File) : BufferedReader {
        val inputStream = FileInputStream(file)
        val gzipStream = GZIPInputStream(inputStream)
        val decoder = InputStreamReader(gzipStream, "utf-8")
        return BufferedReader(decoder)
    }

    private fun processFile(file : File) : List<SemanticScholarData> {
        val reader = getTextStream(file)
        val records = mutableListOf<SemanticScholarData>()
        var progress = 0
        while (true) {
            val newLine = reader.readLine() ?: break
            SemanticScholarJsonParser.parse(newLine)?.let {records.add(it)}

            progress += 1
            if (progress % 100000 == 0) {
                logger.info("Done $progress lines")
            }
        }
        return records
    }

    private fun format(record: SemanticScholarData) {
        record.journalPages = record.journalPages?.replace("""\s+""".toRegex(), "")
        val pages = record.journalPages?.split("-") ?: listOf()
        if (pages.size == 1) {
            record.firstPage = pages[0].toIntOrNull()
        }
        if (pages.size == 2) {
            record.firstPage = pages[0].toIntOrNull()
            record.lastPage = pages[1].toIntOrNull()
            if (record.lastPage != null
                    && record.firstPage != null
                    && record.lastPage.toString().length < record.firstPage.toString().length) {

                val dif = record.firstPage.toString().length - record.lastPage.toString().length
                val n1 = record.firstPage.toString().length
                record.lastPage = (record.firstPage.toString().take(n1 - dif) +
                        record.lastPage.toString().take(dif)).toIntOrNull()
            }
        }
        record.title = record.title?.replace("\n", " ")
    }

    private fun validate(record: SemanticScholarData): Boolean {
        if (record.title.isNullOrBlank() || record.title!!.contains("[Not Available].")) {
            return false
        }
        return true
    }
}