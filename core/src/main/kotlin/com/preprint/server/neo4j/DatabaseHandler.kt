package com.preprint.server.neo4j

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import com.preprint.server.arxiv.ArxivData
import com.preprint.server.data.Author
import com.preprint.server.data.JournalRef
import com.preprint.server.data.Reference
import org.apache.logging.log4j.kotlin.logger
import org.neo4j.driver.Session
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
            val pubIds = it.run("""
                    UNWIND ${"$"}publications as pubData
                    MERGE (pub:${DBLabels.PUBLICATION.str} {arxivId : pubData.arxivId}) 
                    ON CREATE SET pub += pubData, pub:${DBLabels.ARXIV_LBL.str}
                    ON MATCH SET pub:${DBLabels.ARXIV_LBL.str}
                    RETURN id(pub)
                """.trimIndent(), publications
            ).list().map {it.get("id(pub)").asLong()}
            logger.info("Publication nodes created")

            pubIds.zip(arxivRecords).forEach { (id, record) ->
                createAuthorConnections(it, record.authors, id)

                createCitationsConnections(it, record)

                if (record.journal != null) {
                    createJournalPublicationConnections(it, record.journal, id)
                }
            }
            logger.info("All connections created")
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

    private fun refDataToMap(ref : Reference) : Map<String, String> {
        val res = mutableMapOf<String, String>()
        if (!ref.title.isNullOrEmpty()) {
            res += "title" to ref.title!!
        }
        if (!ref.arxivId.isNullOrEmpty()) {
            res += "arxivId" to ref.arxivId!!
        }
        if (!ref.doi.isNullOrEmpty()) {
            res += "doi" to ref.doi!!
        }
        if (!ref.year.isNullOrEmpty()) {
            res += "pubYear" to ref.year!!
        }
        return res
    }

    private fun journalDataToMap(ref : Reference) : Map<String, String> {
        val res = mutableMapOf<String, String>()
        if (!ref.journal.isNullOrEmpty()) {
            res += "jornal" to ref.journal!!
        }
        if (!ref.volume.isNullOrEmpty()) {
            res += "volume" to ref.volume!!
        }
        if (!ref.issue.isNullOrEmpty()) {
            res += "issue" to ref.issue!!
        }
        if (!ref.year.isNullOrEmpty()) {
            res += "pubYear" to ref.year!!
        }
        return res
    }

    fun parm(paramName : String) : String {
        return "\$$paramName"
    }

    //create publication -> author connection and author -> affiliation connection
    private fun createAuthorConnections(session : Session, authors : List<Author>, id : Long) {
        authors.forEach {author ->
            val params = mapOf(
                "name" to author.name,
                "aff" to author.affiliation,
                "pubId" to id
            )
            val createAffiliationQuery =
                if (author.affiliation != null) {
                    """
                        MERGE (aff:${DBLabels.AFFILIATION.str} {name: ${parm("aff")}})
                        MERGE (auth)-[:${DBLabels.WORKS.str}]->(aff)
                    """.trimIndent()
                } else ""

            session.run("""
                        MERGE (auth:${DBLabels.AUTHOR.str} {name: ${parm("name")}})
                        $createAffiliationQuery
                        WITH auth
                        MATCH (pub:${DBLabels.PUBLICATION.str})
                        WHERE id(pub) = ${parm("pubId")}
                        MERGE (pub)-[:${DBLabels.AUTHORED.str}]->(auth)
                    """.trimIndent(), params)
        }
    }

    //create publication -> publication connections and create MissingPublication nodes
    private fun createCitationsConnections(session : Session, record : ArxivData) {
        record.refList.forEach {ref ->
            val params = mapOf(
                "rid" to record.id,
                "arxId" to ref.arxivId,
                "rdoi" to ref.doi,
                "rtit" to ref.title,
                "rRef" to ref.rawReference,
                "cdata" to refDataToMap(ref),
                "jdata" to journalDataToMap(ref)
            )
            val res = session.run("""
                        MATCH (pubFrom:${DBLabels.PUBLICATION.str} {arxivId: ${parm("rid")}})
                        MATCH (pubTo:${DBLabels.PUBLICATION.str})
                        WHERE pubTo <> pubFrom AND (pubTo.arxivId = ${parm("arxId")} OR
                            pubTo.doi = ${parm("rdoi")} OR pubTo.title = ${parm("rtit")})
                        SET pubTo += ${parm("cdata")}
                        MERGE (pubFrom)-[c:${DBLabels.CITES.str} {rawRef: ${parm("rRef")}}]->(pubTo)
                        RETURN pubTo
                    """.trimIndent(), params)

            if (res.list().isEmpty()) {
                if (!ref.validated) {
                    if (!ref.title.isNullOrEmpty()) {
                        //then the cited publication doesn't exist in database
                        //crete missing publication -> publication connection
                        session.run(
                            """
                            MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: ${parm("rid")}})
                            MERGE (mpub:${DBLabels.MISSING_PUBLICATION.str} {title: ${parm("rtit")}})
                            MERGE (mpub)-[c:${DBLabels.CITED_BY.str}]->(pub)
                            SET c.rawRef = ${parm("rRef")}, 
                                mpub += ${parm("cdata")},
                                mpub += ${parm("jdata")}
                        """.trimIndent(), params
                        )
                    }
                    else {
                        val searchByArxivIdQuery =
                            if (ref.arxivId != null) """,mpub.arxivId = ${parm("arxId")}""" else ""
                        val searchByDoiQuery = if (ref.doi != null) """,mpub.doi = ${parm("rdoi")}""" else ""
                        session.run(
                            """
                            MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: ${parm("rid")}})
                            CREATE (mpub:${DBLabels.MISSING_PUBLICATION.str})
                            MERGE (mpub)-[c:${DBLabels.CITED_BY.str}]->(pub)
                            SET c.rawRef = ${parm("rRef")}, 
                                mpub += ${parm("cdata")},
                                mpub += ${parm("jdata")}
                        """.trimIndent(), params
                        )
                    }
                }
                else {
                    val params = mapOf("cdata" to refDataToMap(ref),
                                       "arxivId" to record.id,
                                       "rawRef" to ref.rawReference)
                    val matchString =
                        if (!ref.doi.isNullOrEmpty()) "doi: ${parm("cdata.doi")}"
                        else "title: ${parm("cdata.title")}"
                    val id = session.run(
                        """
                            MATCH (pub:${DBLabels.PUBLICATION.str} {arxivId: ${parm("arxivId")}})
                            MERGE (cpub:${DBLabels.PUBLICATION.str} {${matchString}})
                            ON CREATE SET cpub += ${parm("cdata")}
                            ON MATCH SET cpub += ${parm("cdata")}
                            MERGE (pub)-[cites:${DBLabels.CITES.str}]->(cpub)
                            SET cites.rawRef = ${parm("rawRef")}
                            RETURN id(cpub)
                        """.trimIndent(), params
                    ).list().map {it.get("id(cpub)").asLong()}.get(0)
                    ref.authors?.let {createAuthorConnections(session, it, id)}
                    val journal = JournalRef(rawTitle = ref.journal, volume = ref.volume, pages = ref.pages,
                                             number = ref.issue, issn = ref.issn, rawRef = "")
                    createJournalPublicationConnections(session, journal, id)
                }
            }
        }
    }

    //create publication -> journal connections
    private fun createJournalPublicationConnections(session: Session, journal : JournalRef?, id : Long) {
        if (journal?.rawTitle != null) {
            val params = mapOf(
                "pubId" to id,
                "rjrl" to journal.rawTitle,
                "vol" to journal.volume,
                "pages" to journal.pages,
                "no" to journal.number,
                "rr" to journal.rawRef
            )
            session.run("""
                       MATCH (pub:${DBLabels.PUBLICATION.str})
                       WHERE id(pub) = ${parm("pubId")}
                       MERGE (j:${DBLabels.JOURNAL.str} {title: ${parm("rjrl")}})
                       MERGE (pub)-[jref:${DBLabels.PUBLISHED_IN}]->(j)
                       ON CREATE SET jref.volume = ${parm("vol")}, jref.pages = ${parm("pages")},
                           jref.number = ${parm("no")}, jref.rawRef = ${parm("rr")}
                    """.trimIndent(), params)
        }
    }

    override fun close() {
        driver.close()
    }
}