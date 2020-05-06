package com.preprint.server.validation.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.util.concurrent.atomic.AtomicInteger
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
        val gzipStream = GZIPInputStream(inputStream, 262144)
        val decoder = InputStreamReader(gzipStream, "utf-8")
        return BufferedReader(decoder, 262144)
    }

    private fun processFile(file : File) : List<SemanticScholarData> {
        val reader = getTextStream(file)
        val records = mutableListOf<SemanticScholarData>()
        val progress = AtomicInteger(0)
        runBlocking {
            while (true) {
                val newLine = reader.readLine() ?: break

                launch(Dispatchers.IO) {
                    SemanticScholarJsonParser.parse(newLine)?.let {
                        synchronized(records) {
                            records.add(it)
                            if (progress.incrementAndGet() % 100000 == 0) {
                                logger.info("Done $progress lines")
                            }
                        }
                    }
                }
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
                record.lastPage = (record.firstPage.toString().take(dif) +
                        record.lastPage.toString()).toIntOrNull()
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