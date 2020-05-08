package example

import com.preprint.server.validation.database.DBHandler
import com.preprint.server.validation.database.SSDataLoader

fun main() {
    DBHandler().use {
        SSDataLoader.loadData(it)
    }
}