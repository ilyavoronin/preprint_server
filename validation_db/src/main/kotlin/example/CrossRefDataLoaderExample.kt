package example

import ValidationDBConfig
import com.preprint.server.validation.database.CrossRefDataLoader
import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler =
        DBHandler(ValidationDBConfig.config["validation_db_path"].toString())

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            dbHandler.close()
        }
    })
    dbHandler.use {
        CrossRefDataLoader.loadData(it, false)
        dbHandler.compactDb()
    }
}