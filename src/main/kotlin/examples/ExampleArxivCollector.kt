package examples

import com.preprint.server.arxiv.ArxivCollector
import com.preprint.server.neo4j.DatabaseHandler
import org.apache.logging.log4j.kotlin.logger

fun main() {
    val START_DATE = "2017-05-10"
    val dataBaseHandler = DatabaseHandler("localhost", "7687", "neo4j", "qwerty")
    ArxivCollector.collect(START_DATE, dataBaseHandler)
    dataBaseHandler.close()
}