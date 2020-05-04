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
    private val mainDb: RocksDB
    private val titleDb: RocksDB
    private val jpageDb: RocksDB
    private val volPageYearDb: RocksDB
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
    }

    private fun encode(a: Any): ByteArray {
        return Klaxon().toJsonString(a).toByteArray()
    }

    private fun decodeIds(bytes: ByteArray) : List<Long>? {
        return Klaxon().parseArray(String(bytes))
    }

    override fun close() {
        mainDb.close()
        titleDb.close()
        volPageYearDb.close()
        jpageDb.close()
        options.close()
    }
}