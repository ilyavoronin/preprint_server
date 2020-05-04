package com.prepring.server.validation.database

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object DataLoader {
    fun loadData() {
        val path = Config.config["semsch_path_to_files"].toString()
        for (file in File(path).listFiles()) {
            if (!file.name.endsWith(".gz")) {
                continue
            }
            val records = processFile(file)
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