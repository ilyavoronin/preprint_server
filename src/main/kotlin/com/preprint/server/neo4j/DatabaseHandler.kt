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

        driver.session().use {
            //create new or update publication node

            val publications = mapOf("publications" to arxivRecords.map {arxivDataToMap(it)})
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
                    val params = mapOf(
                        "name" to author.name,
                        "aff" to author.affiliation,
                        "recordId" to record.id
                    )
                    it.run("""
                        MERGE (auth:${DBLabels.AUTHOR.str} {name: ${parm("name")}})
                        ${if (author.affiliation != null) {
                        """
                            MERGE (aff:${DBLabels.AFFILIATION.str} {name: ${parm("aff")}})
                            MERGE (auth)-[:${DBLabels.WORKS.str}]->(aff)
                        """.trimIndent()
                        } else ""
                        }
                        WITH auth
                        MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: ${parm("recordId")}})
                        MERGE (pub)-[:${DBLabels.AUTHORED.str}]->(auth)
                    """.trimIndent(), params)
                }
                logger.info("Publication->author connections created")

                //create publication -> publication connections
                record.refList.forEach {ref ->
                    val params = mapOf(
                        "rid" to record.id,
                        "arxId" to ref.arxivId,
                        "rdoi" to ref.doi,
                        "rtit" to ref.title,
                        "rRef" to ref.rawReference
                    )
                    val res = it.run("""
                        MATCH (pubFrom:${DBLabels.PUBLICATION.str} {arxivId: ${parm("rid")}})
                        MATCH (pubTo:${DBLabels.PUBLICATION.str})
                        WHERE pubTo.arxivId = ${parm("arxId")} OR
                            pubTo.doi = ${parm("rdoi")} OR pubTo.title = ${parm("rtit")}
                        MERGE (pubFrom)-[c:${DBLabels.CITES.str} {rawRef: ${parm("rRef")}}]->(pubTo)
                        RETURN pubTo
                    """.trimIndent(), params)

                    if (res.list().size == 0) {
                        //then the cited publication doesn't exist in database
                        //crete missing publication -> publication connection
                        it.run("""
                            MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: ${parm("rid")}})
                            MERGE (mpub:${DBLabels.MISSING_PUBLICATION.str} {title: ${parm("rtit")}})
                            MERGE (mpub)-[c:${DBLabels.CITED_BY.str}]->(pub)
                            SET c.rawRef = ${parm("rRef")}
                                ${if (ref.arxivId != null) """,mpub.arxivId = ${parm("arxId")}""" else ""}
                                ${if (ref.doi != null) """,mpub.doi = ${parm("rdoi")}""" else ""}
                        """.trimIndent(), params)
                    }
                }
                logger.info("Publication->publication connections created")

                //create publication -> journal connections
                val params = mapOf(
                    "arxId" to record.id,
                    "rjrl" to record.journalRef
                )
                if (record.journalRef != null) {
                    it.run("""
                       MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: ${parm("arxId")}})
                       MERGE (j:${DBLabels.JOURNAL.str} {title: ${parm("rjrl")}})
                       MERGE (pub)-[jref:${DBLabels.PUBLISHED_IN}]->(j)
                    """.trimIndent(), params)
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

    fun parm(paramName : String) : String {
        return "\$$paramName"
    }

    override fun close() {
        driver.close()
    }
}