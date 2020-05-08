package example

import com.preprint.server.validation.database.CrossRefDataLoader
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SSDataLoader

fun main() {
    val dbHandler = DBHandler()
    CrossRefDataLoader.loadData(dbHandler)
    dbHandler.close()
}