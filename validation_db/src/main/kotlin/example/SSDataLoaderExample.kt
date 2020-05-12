package example

import com.preprint.server.validation.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val dbHandler = DBHandler(Config.config["validation_db_path"].toString())
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            dbHandler.close()
        }
    })

    dbHandler.use {
        SSDataLoader.loadData(it, false)
        dbHandler.compactDb()
    }
}