package preprint.server.neo4j

import neo4j.DBLabels
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

        driver.session().use {
            //create new or update publication node
            it.run("""
                    UNWIND ${"$"}publications as pubData
                    MERGE (pub:${DBLabels.PUBLICATION.str} {arxivId : pubData.arxivId}) 
                    ON CREATE SET pub += pubData
                    RETURN pub
                """.trimIndent(), publications
            )

            arxivRecords.forEach { record ->
                //create publication -> author connection and author -> affiliation connection
                record.authors.forEach {author ->
                    it.run("""
                        MERGE (auth:${DBLabels.AUTHOR.str} {name: "${author.name}"})
                        ${if (author.affiliation != null) {
                        """
                            MERGE (aff:${DBLabels.AFFILIATION.str} {name: "${author.affiliation}"})
                            MERGE (auth)-[:${DBLabels.WORKS.str}]->(aff)
                        """.trimIndent()
                        } else ""
                        }
                        WITH auth
                        MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: "${record.id}"})
                        MERGE (pub)-[:${DBLabels.AUTHORED.str}]->(auth)
                    """.trimIndent())
                }

                //create publication -> publication connections
                record.refList.forEach {ref ->
                    it.run("""
                        MATCH (pubFrom:${DBLabels.PUBLICATION.str} {arxivId: "${record.id}})
                        MATCH (pubTo:${DBLabels.PUBLICATION.str})
                        WHERE pubTo.arxivId == ${ref.arxivId} OR
                            pubTo.doi == ${ref.doi} OR pubTo.title == ${ref.title}
                        MERGE (pubFrom)-[:${DBLabels.CITES.str} {rawRef = ${ref.rawReference}}]->(pubTo)
                    """.trimIndent())
                }
            }

            //create publication author connections

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