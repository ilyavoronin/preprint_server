package example

import com.prepring.server.validation.database.DBHandler
import com.prepring.server.validation.database.DataLoader

fun main() {
    val dbHandler = DBHandler()
    DataLoader.loadData(dbHandler)
    dbHandler.close()
}