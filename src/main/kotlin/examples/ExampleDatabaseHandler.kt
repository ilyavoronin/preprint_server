package preprint.server.examples

import preprint.server.arxiv.ArxivData
import preprint.server.neo4j.DatabaseHandler

fun main() {
    val dbh = DatabaseHandler("localhost", "7687", "neo4j", "qwerty")
    val records = mutableListOf<ArxivData>()
    val r1 = ArxivData("1")
    r1.id = "1"
    r1.doi = "doi:1"
    r1.title = "Planets"
    records.add(r1)
    val r2 = ArxivData("2")
    r2.id = "2"
    r2.title = "Stars"
    records.add(r2)

    dbh.storeArxivData(records)
    dbh.close()
}

