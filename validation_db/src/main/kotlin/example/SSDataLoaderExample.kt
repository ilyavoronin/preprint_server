package example

import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SSDataLoader

fun main() {
    val dbHandler = DBHandler()
    SSDataLoader.loadData(dbHandler)
    dbHandler.close()
}