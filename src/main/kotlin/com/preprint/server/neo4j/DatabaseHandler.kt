package com.preprint.server.neo4j

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import com.preprint.server.arxiv.ArxivData
import org.apache.logging.log4j.kotlin.logger
import java.io.Closeable

class DatabaseHandler(
    url : String,
    port : String,
    user : String,
    password : String
) : Closeable {

    private val driver = GraphDatabase.driver("bolt://$url:$port", AuthTokens.basic(user, password))
    private val logger = logger()

    fun storeArxivData(arxivRecords : List<ArxivData>) {
        logger.info("Begin storing ${arxivRecords.size} records to the database")

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
            logger.info("Publication nodes created")

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
                logger.info("Publication->author connections created")

                //create publication -> publication connections
                record.refList.forEach {ref ->
                    val res = it.run("""
                        MATCH (pubFrom:${DBLabels.PUBLICATION.str} {arxivId: "${record.id}"})
                        MATCH (pubTo:${DBLabels.PUBLICATION.str})
                        WHERE pubTo.arxivId = "${ref.arxivId}" OR pubTo.doi = "${ref.doi}" OR pubTo.title = "${ref.title}"
                        MERGE (pubFrom)-[c:${DBLabels.CITES.str} {rawRef: "${ref.rawReference}"}]->(pubTo)
                        RETURN pubTo
                    """.trimIndent())
                    if (res.list().size == 0) {
                        //then the cited publication doesn't exist in database
                        //crete missing publication -> publication connection
                        it.run("""
                            MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: "${record.id}"})
                            MERGE (mpub:${DBLabels.MISSING_PUBLICATION.str} {title: "${ref.title}"})
                            MERGE (mpub)-[c:${DBLabels.CITED_BY.str}]->(pub)
                            SET c.rawRef = "${ref.rawReference}"
                            ${if (ref.arxivId != null) """mpub.arxivId = "${ref.arxivId}"""" else ""}
                            ${if (ref.doi != null) """mpub.doi = "${ref.doi}"""" else ""}
                        """.trimIndent())
                    }
                }
                logger.info("Publication->publication connections created")

                //create publication -> journal connections
                if (record.journalRef != null) {
                    it.run("""
                       MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: "${record.id}"})
                       MERGE (j:${DBLabels.JOURNAL.str} {title: "${record.journalRef}"})
                       MERGE (pub)-[jref:${DBLabels.PUBLISHED_IN}]->(j)
                    """.trimIndent())
                }
                logger.info("Publication->journal connections created")
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