package example

import com.preprint.server.validation.database.Config
import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler =
        DBHandler(Config.config["validation_db_path"].toString())


    val list2 = dbHandler.getByTitle("Prospects for new physics observations in diffractive processes at the LHC and Tevatron")
    println(list2)
    val list3 = dbHandler.getByAuthorVolume("AKV,ADM,GMR","23", 2002)
    println(list3)
    dbHandler.close()
}