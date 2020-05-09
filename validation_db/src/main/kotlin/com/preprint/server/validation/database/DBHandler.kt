package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon
import org.apache.logging.log4j.kotlin.logger
import java.io.File

class DBHandler(dbFolderPath: String): AutoCloseable {
    private val logger = logger()
    private val databases = mutableListOf<SingleDBHandler>()
    private val recordsCnt = mutableListOf<Long>()
    val maxRecordsPerDb = Config.config["validation_db_max_records_per_db"].toString().toLong()

    val dbFolderFile = File(dbFolderPath)
    init {
        if (!dbFolderFile.mkdir() && dbFolderFile.listFiles().isNotEmpty()) {
            logger.info("Database already exists")
            val dbInfo = Klaxon().parse<DBInfo>(File(dbFolderFile, "STRUCTURE"))!!
            databases.addAll(dbInfo.dbPaths.map { SingleDBHandler(File(it)) })
            recordsCnt.addAll(dbInfo.dbRecordsCnt)
        }
        else {
            logger.info("Database was initialized")
        }
    }

    fun storeRecords(records: List<UniversalData>) {
        if (databases.isEmpty() || recordsCnt.last() + records.size > maxRecordsPerDb) {
            logger.info("Creating new database")
            createNewDb()
        }
        recordsCnt[recordsCnt.lastIndex] = recordsCnt.last() + records.size
        writeInfo()
        databases.last().storeRecords(records)
    }

    fun getByTitle(title: String) : List<UniversalData> =
            databases.flatMap { it.mgetById(it.getByTitle(title).toList())}.filterNotNull()

    fun getByJNamePage(jname: String, firstPage: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByJNamePage(jname, firstPage).toList()
                )
            }.filterNotNull()

    fun getByVolPageYear(volume: String, firstPage: Int, year: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByVolPageYear(volume, firstPage, year).toList()
                )
            }.filterNotNull()

    fun getByAuthorVolume(authors: String, volume: String): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthorVolume(authors, volume).toList()
                )
            }.filterNotNull()

    fun getByAuthorPage(authors: String, firstPage: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthorPage(authors, firstPage).toList()
                )
            }.filterNotNull()


    fun getByAuthorYear(authors: String, year: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthorYear(authors, year).toList()
                )
            }.filterNotNull()

    fun getByAuthors(authors: String): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthors(authors).toList()
                )
            }.filterNotNull()

    fun getByFirsLastPageVolume(fpage: Int, lpage: Int, vol: String): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByFirsLastPageVolume(fpage, lpage, vol).toList()
                )
            }.filterNotNull()

    private fun createNewDb() {
        databases.add(SingleDBHandler(File(dbFolderFile, "db${databases.size}")))
        recordsCnt.add(0)
    }

    fun getFirstAuthorLetters(authors: List<String>) : String {
        return authors.joinToString(separator = ",") { name ->
            val words = name.split("""\s""".toRegex()).filter {!it.isBlank()}.sorted()
            words.joinToString(separator = "") { it[0].toString() }
        }
    }

    private fun writeInfo() {
        File(dbFolderFile, "STRUCTURE").writeText(
                Klaxon().toJsonString(DBInfo(databases.map {it.dbFolderPath.absolutePath}, recordsCnt))
        )
    }

    override fun close() {
        databases.forEach {
            it.close()
        }
    }

    data class DBInfo(val dbPaths: List<String>, val dbRecordsCnt: List<Long>)
}