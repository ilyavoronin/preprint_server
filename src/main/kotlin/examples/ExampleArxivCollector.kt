package examples

import com.preprint.server.arxiv.ArxivCollector
import com.preprint.server.neo4j.DatabaseHandler

fun main() {
    val START_DATE = "2020-04-06"
    val dataBaseHandler = DatabaseHandler("localhost", "7687", "neo4j", "qwerty")
    ArxivCollector.collect(START_DATE, dataBaseHandler)
    dataBaseHandler.close()
}