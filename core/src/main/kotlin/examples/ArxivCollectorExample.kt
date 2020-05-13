package examples

import Config
import com.preprint.server.core.arxiv.ArxivCollector
import com.preprint.server.core.neo4j.DatabaseHandler
import com.preprint.server.core.validation.ArxivValidator
import com.preprint.server.core.validation.CrossRefValidator

fun main() {
    val START_DATE = "2010-05-11"
    val dataBaseHandler = DatabaseHandler(
        Config.config["neo4j_url"].toString(),
        Config.config["neo4j_port"].toString(),
        Config.config["neo4j_user"].toString(),
        Config.config["neo4j_password"].toString()
    )
    ArxivCollector.collect(START_DATE, dataBaseHandler, listOf(CrossRefValidator, ArxivValidator))
    dataBaseHandler.close()
}