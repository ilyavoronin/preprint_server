import com.preprint.server.validation.database.CrossRefDataLoader
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SSDataLoader
import joptsimple.OptionParser
import java.io.IOException

fun main(args: Array<String>) {
    val optionParser = OptionParser()
    optionParser.accepts("with-doi")
    optionParser.accepts("filter-duplicates")
    optionParser.accepts("start-from")
    val options = optionParser.parse(*args)

    val dbHandler =
        DBHandler(ValidationDBConfig.config["validation_db_path"].toString())

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            dbHandler.close()
        }
    })

    dbHandler.use {
        when (args[0]) {
            "crossref" -> {
                val checkDuplicates = if (options.has("filter-duplicates")) {
                    options.valueOf("filter-duplicates").toString().toBoolean()
                } else {
                    false
                }

                val startFrom = if (options.has("start-from")) {
                    options.valueOf("start-from").toString().toLong()
                } else {
                    0
                }
                CrossRefDataLoader.loadData(it, checkDuplicates, startFrom)
            }

            "semsch" -> {
                val checkDuplicates = if (options.has("filter-duplicates")) {
                    options.valueOf("filter-duplicates").toString().toBoolean()
                } else {
                    true
                }

                val onlyWithDoi = if (options.has("with-doi")) {
                    options.valueOf("with-doi").toString().toBoolean()
                } else {
                    true
                }
                SSDataLoader.loadData(it, checkDuplicates, onlyWithDoi)
            }
            else -> throw IOException("Illegal argument ${args[0]}")
        }
    }
}