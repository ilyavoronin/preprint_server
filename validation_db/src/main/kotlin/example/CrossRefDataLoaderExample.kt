package example

import com.preprint.server.validation.database.CrossRefDataLoader
import com.preprint.server.validation.database.DBHandler

fun main() {
    DBHandler().use {
        CrossRefDataLoader.loadData(it)
    }
}