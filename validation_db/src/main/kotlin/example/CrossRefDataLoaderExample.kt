package example

import com.preprint.server.validation.database.CrossRefDataLoader
import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SSDataLoader

fun main() {
    DBHandler().use {
        CrossRefDataLoader.loadData(it, 76000000)
    }
}