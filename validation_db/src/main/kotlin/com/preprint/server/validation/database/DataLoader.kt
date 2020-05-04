package com.preprint.server.validation.database

import org.apache.logging.log4j.kotlin.logger
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object DataLoader {
    private val logger = logger()

    fun loadData(dbHandler: DBHandler) {
        val path = Config.config["semsch_path_to_files"].toString()
        val files = File(path).listFiles().filter {it.isFile && it.name.endsWith(".gz")}
        for ((i, file) in files.withIndex()) {
            logger.info("Begin extract records from $i archive out of ${files.size}")
            val records = processFile(file)
            logger.info("Begin storing ${records.size} records to the database")
            dbHandler.storeRecords(records)
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
        while (true) {
            val newLine = reader.readLine() ?: break
            SemanticScholarJsonParser.parse(newLine)?.let {records.add(it)}
        }
        return records
    }
}