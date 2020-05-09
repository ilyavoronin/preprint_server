package com.preprint.server.validation.database

import kotlinx.coroutines.Dispatchers
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

    fun loadData(dbHandler: DBHandler, startFrom: Long = 0) {
        var recordProcessed = 0
        for (i in 0 until startFrom) {
            val line = textStream.readLine()
            recordProcessed += 1
            if (recordProcessed % 1_000_000 == 0) {
                logger.info("Processed $recordProcessed records")
                val recordToCheck = CrossRefJsonParser.parse(line)
                logger.info("Record to check: $recordToCheck")
                if (!recordToCheck?.title.isNullOrBlank()
                        && dbHandler.getByTitle(recordToCheck!!.title!!).isEmpty()
                ) {
                    logger.error("Warning: this record wasn't found in the title database")
                }
            }
        }
        while (true) {
            logger.info("Begin parsing next ${bulkRecodsNumber} records")
            val (records, isEOF) = getNextRecords()
            logger.info("Begin storing ${records.size} records to the database")
            var tries = 0
            while (true) {
                tries += 1
                try {
                    dbHandler.storeRecords(records)
                    break
                } catch (e: Exception) {
                    logger.info("${e.message}")
                    if (tries >= 3) {
                        logger.error("Failed to store records")
                        break
                    }
                }
            }
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
                CrossRefJsonParser.parse(line)?.let {
                    records.add(it)
                }
            }
        }
        return Pair(records, isEOF)
    }
}