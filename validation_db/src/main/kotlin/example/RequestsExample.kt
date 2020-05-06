package example

import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler = DBHandler()
    var ids = dbHandler.getByTitle("Analysis of Ethernet AVB for automotive networks using Network Calculus")
    ids.forEach {
        println(dbHandler.getById(it))
    }

    println()
    ids = dbHandler.getByJNamePage("British heart journal", 239)
    ids.forEach {
        println(dbHandler.getById(it))
    }

    val list = dbHandler.getByVolPageYear("179", 466, 1966)
    val list2 = dbHandler.getByTitle("Ozonides of cyclic enol esters")
    val list3 = dbHandler.getByJNamePage("British heart journal", 239)
    val list4 = dbHandler.getByAuthorYear("CV,TSM,VSC,SM", 2015)
    val list5 = dbHandler.getByAuthorVolume("CV,TSM,VSC,SM", "820")
    val list6 = dbHandler.getByAuthorPage("CV,TSM,VSC,SM", 419)
    println(list)
    println(list2)
    println(list3)
    println(list4)
    println(list5)
    println(list6)
    dbHandler.close()
}