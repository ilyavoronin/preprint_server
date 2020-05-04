package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon
import org.apache.logging.log4j.kotlin.logger
import org.rocksdb.Options
import org.rocksdb.RocksDB
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DBHandler : AutoCloseable {
    private var currentId = AtomicLong(0)
    private val dbPath = Config.config["semsch_path_to_db"].toString()
    private val db: RocksDB
    private val options: Options
    private val logger = logger()

    init {
        RocksDB.loadLibrary()
        if (File(dbPath).mkdir()) {
            currentId.set(0)
            File("$dbPath/maxId").writeText("0")
        }
        else {
            val file = File("$dbPath/maxId")
            if (file.exists()) {
                currentId.set(File("$dbPath/maxId").readText().toLong())
            } else {
                File("$dbPath/maxId").writeText("0")
                currentId.set(0)
            }
        }
        File("$dbPath/main").mkdir()

        options = Options().setCreateIfMissing(true)
        db = RocksDB.open(options, "$dbPath/main")
    }
    fun storeRecords(records: List<SemanticScholarData>) {
        var progress = 0
        records.forEach { record ->
            val rbytes = Klaxon().toJsonString(record).toByteArray()
            val kbytes = Klaxon().toJsonString(currentId.getAndIncrement()).toByteArray()
            db.put(kbytes, rbytes)

            progress += 1
            if (progress % 100000 == 0) {
                logger.info("Done $progress out of ${records.size}")
            }
        }
    }

    fun getById(id : Long) : SemanticScholarData? {
        val kbytes = Klaxon().toJsonString(id).toByteArray()
        return Klaxon().parse<SemanticScholarData>(String(db.get(kbytes)))
    }

    override fun close() {
        db.close()
        options.close()
    }
}