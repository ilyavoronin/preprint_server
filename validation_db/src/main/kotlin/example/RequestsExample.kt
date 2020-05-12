package example

import com.preprint.server.validation.database.Config
import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler = DBHandler(Config.config["validation_db_path"].toString())


    val list2 = dbHandler.getByTitle("Can the Higgs be seen in rapidity gap events at the Tevatron or the LHC?")
    println(list2)
    dbHandler.close()
}