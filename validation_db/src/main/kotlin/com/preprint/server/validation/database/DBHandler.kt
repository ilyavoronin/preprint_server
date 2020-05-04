package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon
import org.apache.logging.log4j.kotlin.logger
import org.rocksdb.Options
import org.rocksdb.RocksDB
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DBHandler : AutoCloseable {
    private var currentId = AtomicLong(0)
    private val dbFolderPath = File(Config.config["semsch_path_to_db"].toString())
    private val mainDbPath = File(dbFolderPath, "main")
    private val titleDbPath = File(dbFolderPath, "title")
    private val jpageDbPath = File(dbFolderPath, "jpage")
    private val volPageYearDbPath = File(dbFolderPath, "volpy")
    private val authorYDbPath = File(dbFolderPath, "authory")
    private val authorVDbPath = File(dbFolderPath, "authorv")
    private val authorPDbPath = File(dbFolderPath, "authorp")
    private val mainDb: RocksDB
    private val titleDb: RocksDB
    private val jpageDb: RocksDB
    private val volPageYearDb: RocksDB
    private val authorYearDb: RocksDB
    private val authorVolumeDb: RocksDB
    private val authorPageDb: RocksDB
    private val options: Options
    private val logger = logger()

    init {
        RocksDB.loadLibrary()
        currentId.set(0)
        File("$dbFolderPath/main").mkdir()

        options = Options().setCreateIfMissing(true)
        mainDb = RocksDB.open(options, mainDbPath.absolutePath)
        titleDb = RocksDB.open(options, titleDbPath.absolutePath)
        jpageDb = RocksDB.open(options, jpageDbPath.absolutePath)
        volPageYearDb = RocksDB.open(options, volPageYearDbPath.absolutePath)
        authorYearDb = RocksDB.open(options, authorYDbPath.absolutePath)
        authorVolumeDb = RocksDB.open(options, authorVDbPath.absolutePath)
        authorPageDb = RocksDB.open(options, authorPDbPath.absolutePath)
    }
    fun storeRecords(records: List<SemanticScholarData>) {
        var progress = 0
        records.forEach { record ->
            storeRecord(record)
            progress += 1
            if (progress % 100000 == 0) {
                logger.info("Done $progress out of ${records.size}")
            }
        }
    }

    fun getById(id : Long) : SemanticScholarData? {
        val kbytes = Klaxon().toJsonString(id).toByteArray()
        return Klaxon().parse<SemanticScholarData>(String(mainDb.get(kbytes)))
    }

    fun getByTitle(title: String) : List<Long> {
        val titleBytes = title.toByteArray()
        val recordsBytes = titleDb.get(titleBytes) ?: return listOf()
        return decodeIds(recordsBytes) ?: return listOf()
    }

    fun getByJNamePage(jname: String, firstPage: Int): List<Long> {
        val bytes = encode(Pair(jname, firstPage))
        val recordsBytes = jpageDb.get(bytes) ?: return listOf()
        return decodeIds(recordsBytes) ?: listOf()
    }

    fun getByVolPageYear(volume: String, firstPage: Int, year: Int) : List<Long> {
        val bytes = encode(Triple(volume, firstPage, year))
        val recordsBytes = volPageYearDb.get(bytes) ?: return listOf()
        return decodeIds(recordsBytes) ?: listOf()
    }

    fun getByAuthorVolume(authors: String, volume: String) : List<Long> {
        val bytes = encode(Pair(authors, volume))
        val recordsBytes = authorVolumeDb.get(bytes) ?: return listOf()
        return decodeIds(recordsBytes) ?: listOf()
    }

    fun getByAuthorPage(authors: String, firstPage: Int) : List<Long> {
        val bytes = encode(Pair(authors, firstPage))
        val recordsBytes = authorPageDb.get(bytes) ?: return listOf()
        return decodeIds(recordsBytes) ?: listOf()
    }

    fun getByAuthorYear(authors: String, year: Int) : List<Long> {
        val bytes = encode(Pair(authors, year))
        val recordsBytes = authorYearDb.get(bytes) ?: return listOf()
        return decodeIds(recordsBytes) ?: listOf()
    }

    private fun storeRecord(record: SemanticScholarData) {
        val id = currentId.getAndIncrement()
        val recordBytes = Klaxon().toJsonString(record).toByteArray()
        val idBytes = Klaxon().toJsonString(id).toByteArray()
        mainDb.put(idBytes, recordBytes)

        if (!record.title.isNullOrBlank()) {
            val titleBytes = record.title!!.toByteArray()
            val recordList = getByTitle(record.title!!).toMutableList()
            recordList.add(id)
            titleDb.put(titleBytes, encode(recordList.toList()))
        }

        if (!record.journalName.isNullOrBlank() && record.firstPage != null) {
            val bytes = encode(Pair(record.journalName, record.firstPage))
            val recordList = getByJNamePage(record.journalName, record.firstPage!!).toMutableList()
            recordList.add(id)
            jpageDb.put(bytes, encode(recordList.toList()))
        }

        if (!record.journalVolume.isNullOrBlank() &&
                record.firstPage != null && record.year != null) {
            val bytes = encode(Triple(record.journalVolume, record.firstPage, record.year))
            val recordList = getByVolPageYear(record.journalVolume, record.firstPage!!, record.year).toMutableList()
            recordList.add(id)
            volPageYearDb.put(bytes, encode(recordList.toList()))
        }

        if (record.authors.size >= 3) {
            val authorString = getFirstAuthorLetters(record)
            println(authorString)
            if (!record.journalVolume.isNullOrBlank()) {
                val bytes = encode(Pair(authorString, record.journalVolume))
                val recordList = getByAuthorVolume(authorString, record.journalVolume).toMutableList()
                recordList.add(id)
                authorVolumeDb.put(bytes, encode(recordList.toList()))
            }

            if (record.firstPage != null) {
                val bytes = encode(Pair(authorString, record.firstPage))
                val recordList = getByAuthorPage(authorString, record.firstPage!!).toMutableList()
                recordList.add(id)
                authorPageDb.put(bytes, encode(recordList.toList()))
            }

            if (record.year != null) {
                val bytes = encode(Pair(authorString, record.year))
                val recordList = getByAuthorYear(authorString, record.year).toMutableList()
                recordList.add(id)
                authorYearDb.put(bytes, encode(recordList.toList()))
            }
        }
    }

    private fun encode(a: Any): ByteArray {
        return Klaxon().toJsonString(a).toByteArray()
    }

    private fun decodeIds(bytes: ByteArray) : List<Long>? {
        return Klaxon().parseArray(String(bytes))
    }

    private fun getFirstAuthorLetters(record: SemanticScholarData) : String {
        return record.authors.joinToString(separator = ",") { author ->
            val words = author.name.split("""\s""".toRegex()).filter {!it.isBlank()}
            words.joinToString(separator = "") { it[0].toString() }
        }
    }

    override fun close() {
        mainDb.close()
        titleDb.close()
        volPageYearDb.close()
        jpageDb.close()
        authorVolumeDb.close()
        authorYearDb.close()
        authorPageDb.close()
        options.close()
    }
}