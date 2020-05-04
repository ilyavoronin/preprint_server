package example

import com.preprint.server.validation.database.DBHandler

fun main() {
    val dbHandler = DBHandler()
    for (i in 0..100) {
        val list = dbHandler.getByVolPageYear("179", 466, 1966)
        val list2 = dbHandler.getByTitle("Ozonides of cyclic enol esters")
        val list3 = dbHandler.getByJNamePage("British heart journal", 239)
        println(list)
        println(list2)
        println(list3)
    }
    dbHandler.close()
}