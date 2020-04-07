package examples

import arxiv.ArxivCollector
import preprint.server.neo4j.DatabaseHandler
import java.io.File

const val START_DATE = "2020-04-06"
const val fileName = "files/arxivRecords.txt"

fun main() {
    val dataBaseHandler = DatabaseHandler("localhost", "7687", "neo4j", "qwerty")
    ArxivCollector.collect(START_DATE, dataBaseHandler)
    dataBaseHandler.close()
}