package example

import ValidationDBConfig
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SSDataLoader

fun main() {
    val dbHandler =
        DBHandler(ValidationDBConfig.config["validation_db_path"].toString())
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            dbHandler.close()
        }
    })

    dbHandler.use {
        SSDataLoader.loadData(it, true, true)
        dbHandler.compactDb()
    }
}