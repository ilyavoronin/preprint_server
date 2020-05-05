package example

import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.DataLoader

fun main() {
    val dbHandler = DBHandler()
    DataLoader.loadData(dbHandler, 6)
    dbHandler.close()
}