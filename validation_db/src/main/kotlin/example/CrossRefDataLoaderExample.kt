package example

import com.preprint.server.validation.database.Config
import com.preprint.server.validation.database.CrossRefDataLoader
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SingleDBHandler

fun main() {
    val dbHandler = DBHandler(Config.config["validation_db_path"].toString())

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            dbHandler.close()
        }
    })
    dbHandler.use {
        CrossRefDataLoader.loadData(it, false,54900000)
    }
}