package preprint.server.neo4j

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import preprint.server.arxiv.ArxivData
import java.io.Closeable

class DatabaseHandler(
    url : String,
    port : String,
    user : String,
    password : String
) : Closeable {

    private val driver = GraphDatabase.driver("bolt://$url:$port", AuthTokens.basic(user, password))

    fun storeArxivData(arxivRecords : List<ArxivData>) {

        val publications = mapOf("publications" to arxivRecords.map {arxivDataToMap(it)})

        //create new or update publication node
        driver.session().use {
            it.run("""
                    UNWIND ${"$"}publications as pubData
                    MERGE (pub:Publication {title : pubData.arxivId}) 
                    ON CREATE SET pub += pubData
                    RETURN pub
                """.trimIndent(), publications)
        }
    }

    private fun arxivDataToMap(record : ArxivData) : Map<String, String> {
        val res = mutableMapOf<String, String>()
        res += "title" to record.title
        res += "arxivId" to record.id
        if (record.doi != null) {
            res += "doi" to record.doi!!
        }
        if (record.mscClass != null) {
            res += "mscClass" to record.mscClass!!
        }
        if (record.acmClass != null) {
            res += "acmClass" to record.acmClass!!
        }
        return res
    }

    override fun close() {
        driver.close()
    }
}