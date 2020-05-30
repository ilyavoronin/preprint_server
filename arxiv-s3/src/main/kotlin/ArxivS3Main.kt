import com.preprint.server.arxivs3.ArxivS3Collector
import com.preprint.server.core.neo4j.DatabaseHandler
import com.preprint.server.core.ref.CustomReferenceExtractor
import com.preprint.server.core.ref.GrobidReferenceExtractor
import com.preprint.server.core.ref.ReferenceExtractor
import com.preprint.server.core.validation.ArxivValidator
import com.preprint.server.core.validation.CrossRefValidator
import com.preprint.server.core.validation.LocalValidator
import joptsimple.OptionParser
import java.io.IOException

fun main(args: Array<String>) {
    val optionParser = OptionParser()
    optionParser.accepts("download-only")
    optionParser.accepts("ref-extractor").withRequiredArg()
    optionParser.accepts("validators").withRequiredArg()
    optionParser.accepts("mpd").withRequiredArg()
    optionParser.accepts("threads").withRequiredArg()

    val options = optionParser.parse(*args)

    val downloadOnlyMode = options.has("download-only")


    var refExtractor: ReferenceExtractor = CustomReferenceExtractor
    if (options.has("ref-extractor")) {
        when (options.valueOf("ref-extractor").toString()) {
            "g" -> refExtractor = GrobidReferenceExtractor
            "c" -> refExtractor = CustomReferenceExtractor
            else -> throw IOException("Wrong argument for --ref-extractor")
        }
    }

    var validators = mutableListOf(LocalValidator, ArxivValidator)
    if (options.has("validators")) {
        validators = mutableListOf()
        options.valueOf("validators").toString().forEach { c ->
            when (c) {
                'l' -> validators.add(LocalValidator)
                'c' -> validators.add(CrossRefValidator)
                'a' -> validators.add(ArxivValidator)
                else -> throw IOException("Wrong argument for --validators")
            }
        }
    }

    val maxParallelDownload = if (options.has("mpd")) {
        options.valueOf("mpd").toString().toInt()
    } else {
        10
    }

    val maxThreads = if (options.has("threads")) {
        options.valueOf("threads").toString().toInt()
    } else {
        -1
    }


    val dbHandler = if (!downloadOnlyMode) DatabaseHandler(
        ArxivS3Config.config["neo4j_url"].toString(),
        ArxivS3Config.config["neo4j_port"].toString(),
        ArxivS3Config.config["neo4j_user"].toString(),
        ArxivS3Config.config["neo4j_password"].toString()
    ) else null

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            dbHandler?.close()
            LocalValidator.close()
        }
    })

    println("ArxivS3Collector will be launched with the following parametrs:")
    println("download only mode: $downloadOnlyMode")
    println("reference extractor: ${refExtractor.javaClass.name}")
    println("validators: ${validators.joinToString(separator = ", ") { it.javaClass.name }}")
    println("max parallel downloads: $maxParallelDownload")
    println("max threads: $maxThreads")

    ArxivS3Collector.beginBulkDownload(
        dbHandler,
        refExtractor,
        validators,
        maxParallelDownload,
        maxThreads
    )
}