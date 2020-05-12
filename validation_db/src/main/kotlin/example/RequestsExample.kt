package example

import com.preprint.server.validation.database.Config
import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler = DBHandler(Config.config["validation_db_path"].toString())

    val list0 = dbHandler.getByAuthorVolume("KV,AM,MR", "14")
    println(list0)

    val list2 = dbHandler.getByTitle("Can the Higgs be seen in rapidity gap events at the Tevatron or the LHC?")
    val list5 = dbHandler.getByAuthorVolume("CV,TSM,VSC,SM", "820")
    val list6 = dbHandler.getByAuthorPage("CV,TSM,VSC,SM", 419)
    println(list2)
    println(DBHandler.getFirstAuthorLetters(list2[0].authors.map {it.name}))
    println(list5)
    println(list6)
    dbHandler.close()
}