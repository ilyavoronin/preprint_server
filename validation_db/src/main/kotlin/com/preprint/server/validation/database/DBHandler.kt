package com.preprint.server.validation.database

import ValidationDBConfig
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.io.File

class DBHandler(dbFolderPath: String): AutoCloseable {
    private val logger = logger()
    private val databases = mutableListOf<SingleDBHandler>()
    private val recordsCnt = mutableListOf<Long>()
    val maxRecordsPerDb = ValidationDBConfig.config["validation_db_max_records_per_db"].toString().toLong()

    val dbFolderFile = File(dbFolderPath)
    init {
        if (!dbFolderFile.mkdir() && dbFolderFile.listFiles().isNotEmpty()) {
            logger.info("Database already exists")
            val dbInfo = Klaxon().parse<DBInfo>(File(dbFolderFile, "STRUCTURE"))!!
            databases.addAll(dbInfo.dbPaths.map {
                SingleDBHandler(
                    File(it)
                )
            })
            recordsCnt.addAll(dbInfo.dbRecordsCnt)
        }
        else {
            logger.info("Database was initialized")
        }
    }

    fun storeRecords(records_: List<UniversalData>, checkDuplicates: Boolean) {
        val records = if (checkDuplicates) filterExisting(records_)
                      else records_
        if (checkDuplicates) {
            logger.info("Records left after filtering: ${records.size}")
        }
        if (databases.isEmpty() || recordsCnt.last() + records.size > maxRecordsPerDb) {
            logger.info("Creating new database")
            createNewDb()
        }
        recordsCnt[recordsCnt.lastIndex] = recordsCnt.last() + records.size
        writeInfo()
        databases.last().storeRecords(records)
    }

    private fun filterExisting(records_: List<UniversalData>): List<UniversalData> {
        val exists = BooleanArray(records_.size, {false})
        val records = records_.sortedBy { it.doi }
        databases.forEach {db ->
                val jobs = records.chunked(20000).map {
                        db.doiDb.multiGetAsList(it.map { it.doi?.toByteArray() })
                }
                val list = jobs.flatMap { it }
                list.forEachIndexed { i, ba -> if (ba != null) exists[i] = true }
        }
        return records.filterIndexed {i, _ -> !exists[i]}
    }

    private fun <T> mergeLists(list1: List< List< List <T>>>): List<List<T>> {
        return list1.fold(List(list1[0].size, { listOf<T>()})) {acc, list ->
            acc.zip(list).map {(l1, l2) -> l1 + l2}
        }
    }

    fun getByTitle(title: String) : List<UniversalData> {
        return databases.flatMap { it.mgetById(it.getByTitle(title).toList()) }.filterNotNull()
    }

    fun mgetByTitle(
        titleList: List<UniversalData>
    ): List<List<UniversalData>> = runBlocking(Dispatchers.IO) {
            val jobs = databases.map { db ->
                async {
                    db.mgetByTitle(titleList)
                }
            }
            mergeLists(jobs.map {it.await()})
    }

    fun getByAuthVolPageYear(auth: String, volume: String, firstPage: Int, year: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthVolPageYear(auth, volume, firstPage, year).toList()
                )
            }.filterNotNull()

    fun mgetByAuthVolPageYear(list: List<UniversalData>) = runBlocking(Dispatchers.IO) {
        val jobs = databases.map { db ->
            async {
                db.mgetByAuthVolPageYear(list)
            }
        }
        mergeLists(jobs.map {it.await()})
    }

    fun mgetByAuthPageYear(list: List<UniversalData>) = runBlocking(Dispatchers.IO) {
        val jobs = databases.map { db ->
            async {
                db.mgetByAuthPage(list)
            }
        }
        mergeLists(jobs.map {it.await()})
    }

    fun mgetByAuthVolumeYear(list: List<UniversalData>) = runBlocking(Dispatchers.IO) {
        val jobs = databases.map { db ->
            async {
                db.mgetByAuthVolume(list)
            }
        }
        mergeLists(jobs.map {it.await()})
    }

    fun mgetByAuthFLPageVolume(list: List<UniversalData>) = runBlocking(Dispatchers.IO) {
        val jobs = databases.map { db ->
            async {
                db.mgetByAuthFLVolume(list)
            }
        }
        mergeLists(jobs.map {it.await()})
    }

    fun getByAuthorVolume(authors: String, volume: String, year: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthorVolume(authors, volume, year).toList()
                )
            }.filterNotNull()

    fun getByAuthorPage(authors: String, firstPage: Int, year: Int): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByAuthorPage(authors, firstPage, year).toList()
                )
            }.filterNotNull()

    fun getByAuthFirsLastPageVolume(auth: String, fpage: Int, lpage: Int, vol: String): List<UniversalData> =
            databases.flatMap {
                it.mgetById(
                        it.getByFirsLastPageVolume(auth, fpage, lpage, vol).toList()
                )
            }.filterNotNull()

    private fun createNewDb() {
        if (databases.isNotEmpty()) databases.last().compactDb(true)
        databases.add(
            SingleDBHandler(
                File(
                    dbFolderFile,
                    "db${databases.size}"
                )
            )
        )
        recordsCnt.add(0)
    }


    private fun writeInfo() {
        File(dbFolderFile, "STRUCTURE").writeText(
                Klaxon().toJsonString(
                    DBInfo(
                        databases.map { it.dbFolderPath.absolutePath },
                        recordsCnt
                    )
                )
        )
    }

    fun compactDb() {
        databases.forEach {
            it.compactDb(true)
        }
    }

    override fun close() {
        databases.forEach {
            it.close()
        }
    }

    data class DBInfo(val dbPaths: List<String>, val dbRecordsCnt: List<Long>)

    companion object {
        fun getFirstAuthorLetters(authors: List<String>): String {
            return authors.joinToString(separator = ",") { name ->
                val words = name.split("""(\s|\.)""".toRegex()).filter { !it.isBlank() }.sorted()
                words.joinToString(separator = "") { it[0].toString() }
            }
        }
    }
}