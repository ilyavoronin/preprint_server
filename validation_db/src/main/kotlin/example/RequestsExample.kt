package example

import com.preprint.server.validation.database.Config
import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler = DBHandler(Config.config["validation_db_path"].toString())

    val list0 = dbHandler.getByTitle("Cooling of White Dwarfs")
    println(list0)

    val list2 = dbHandler.getByTitle("Ozonides of cyclic enol esters")
    val list5 = dbHandler.getByAuthorVolume("CV,TSM,VSC,SM", "820")
    val list6 = dbHandler.getByAuthorPage("CV,TSM,VSC,SM", 419)
    println(list2)
    println(list5)
    println(list6)
    dbHandler.close()
}