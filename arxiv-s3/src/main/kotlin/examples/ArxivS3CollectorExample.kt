package examples

import com.preprint.server.core.Config
import com.preprint.server.arxivs3.ArxivS3Collector
import com.preprint.server.core.neo4j.DatabaseHandler
import com.preprint.server.core.ref.CustomReferenceExtractor
import com.preprint.server.core.validation.ArxivValidator

fun main() {
    val downloadOnlyMode = true
    val dbHandler = if (!downloadOnlyMode) DatabaseHandler(
        Config.config["neo4j_url"].toString(),
        Config.config["neo4j_port"].toString(),
        Config.config["neo4j_user"].toString(),
        Config.config["neo4j_password"].toString()
    ) else null
    ArxivS3Collector.beginBulkDownload(
        dbHandler,
        CustomReferenceExtractor,
        listOf(ArxivValidator),
        16
    )
}