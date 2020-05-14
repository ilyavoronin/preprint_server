package example

import ValidationDBConfig
import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler =
        DBHandler(ValidationDBConfig.config["validation_db_path"].toString())


    val list2 = dbHandler.getByTitle("Few-nucleon forces and systems in chiral effective field theory")
    println(list2)
    val list3 = dbHandler.getByAuthVolPageYear("EE","43", 57, 2008)
    println(list3)
    dbHandler.close()
}