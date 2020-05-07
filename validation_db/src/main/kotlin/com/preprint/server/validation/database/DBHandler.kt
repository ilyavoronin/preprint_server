package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.kotlin.logger
import org.rocksdb.*
import org.rocksdb.util.BytewiseComparator
import java.io.File
import java.lang.Integer.max
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

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
    private val authorDbPath = File(dbFolderPath, "author")
    private val flVolDbPath = File(dbFolderPath, "flvol")

    private lateinit var mainDb: RocksDB
    private lateinit var titleDb: RocksDB
    private lateinit var jpageDb: RocksDB
    private lateinit var volPageYearDb: RocksDB
    private lateinit var authorYearDb: RocksDB
    private lateinit var authorVolumeDb: RocksDB
    private lateinit var authorPageDb: RocksDB
    private lateinit var authorDb: RocksDB
    private lateinit var flVolDb: RocksDB

    private val mainKeys = HashMap<String, ByteArray>()
    private val titleKeys = HashMap<String, MutableList<Long>>()
    private val jpageKeys = HashMap<String, MutableList<Long>>()
    private val volPageYearKeys = HashMap<String, MutableList<Long>>()
    private val authorYearKeys = HashMap<String, MutableList<Long>>()
    private val authorVolumeKeys = HashMap<String, MutableList<Long>>()
    private val authorPageKeys = HashMap<String, MutableList<Long>>()
    private val authorKeys = HashMap<String, MutableList<Long>>()
    private val flVolKeys = HashMap<String, MutableList<Long>>()

    private val options: Options

    private val logger = logger()

    private val maxIdPath = File(dbFolderPath, "ID.txt")

    private var lastTime: Long? = null

    data class Stats(
            var maxTitleLength: Int = 0,
            var maxJPageLength: Int = 0,
            var maxVolPageYearLength: Int = 0,
            var maxAuthorYearLength: Int = 0,
            var maxAuthorPageLength: Int = 0,
            var maxAuthorVolumeLength: Int = 0,
            var maxAuthorLength: Int = 0,
            var maxFLVolLength: Int = 0
    )
    val stats = Stats()

    init {
        dbFolderPath.mkdir()
        RocksDB.loadLibrary()
        if (maxIdPath.exists()) {
            currentId.set(maxIdPath.readText().toLong() + 10_000_000)
        }
        else {
            currentId.set(0)
            maxIdPath.writeText("0")
        }
        File("$dbFolderPath/main").mkdir()

        options = Options().setCreateIfMissing(true)
                .setMaxSuccessiveMerges(1000)
                .setOptimizeFiltersForHits(true)
                .setNewTableReaderForCompactionInputs(true)
        openDb()
    }
    fun storeRecords(records: List<SemanticScholarData>) {
        mainKeys.clear()
        titleKeys.clear()
        jpageKeys.clear()
        volPageYearKeys.clear()
        authorYearKeys.clear()
        authorVolumeKeys.clear()
        authorPageKeys.clear()
        authorKeys.clear()
        flVolKeys.clear()

        val currentTime = measureTimeMillis {
            logger.info("Begin storing records to maps")
            records.forEach { storeRecordLocal(it) }

            logger.info("Begin requests to get full lists for keys")
            getFullLists()

            storeAllKeys()
        }
        maxIdPath.writeText(currentId.getAcquire().toString())

        logger.info("Done in ${currentTime / 60_000} minutes," +
                " ${(currentTime % 60_000) / 1000} seconds")

        if (lastTime != null && currentTime > lastTime!! * 1.5
                || currentTime > 1000 * 60 * 10
        ) {
            logger.info("Reloading databases")
            reloadDb()
            lastTime = -1
        } else {
            lastTime = currentTime
        }
    }

    private fun bgetById(bytes: ByteArray) : SemanticScholarData? {
        return Klaxon().parse<SemanticScholarData>(String(mainDb.get(bytes)))
    }

    fun getById(id : Long) : SemanticScholarData? {
        val kbytes = Klaxon().toJsonString(id).toByteArray()
        return bgetById(kbytes)
    }

    private fun bgetByTitle(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = titleDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: return mutableSetOf()
    }

    fun getByTitle(title: String) : MutableSet<Long> {
        val bytes = title.toByteArray()
        return bgetByTitle(bytes)
    }

    private fun bgetByJNamePage(bytes: ByteArray): MutableSet<Long> {
        val recordsBytes = jpageDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByJNamePage(jname: String, firstPage: Int): MutableSet<Long> {
        val bytes = encode(Pair(jname, firstPage))
        return bgetByJNamePage(bytes)
    }

    private fun bgetByVolPageYear(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = volPageYearDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByVolPageYear(volume: String, firstPage: Int, year: Int) : MutableSet<Long> {
        val bytes = encode(Triple(volume, firstPage, year))
        return bgetByVolPageYear(bytes)
    }

    private fun bgetByAuthorVolume(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authorVolumeDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByAuthorVolume(authors: String, volume: String) : MutableSet<Long> {
        val bytes = encode(Pair(authors, volume))
        return bgetByAuthorVolume(bytes)
    }

    private fun bgetByAuthorPage(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authorPageDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByAuthorPage(authors: String, firstPage: Int) : MutableSet<Long> {
        val bytes = encode(Pair(authors, firstPage))
        return bgetByAuthorPage(bytes)
    }

    private fun bgetByAuthorYear(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authorYearDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByAuthorYear(authors: String, year: Int) : MutableSet<Long> {
        val bytes = encode(Pair(authors, year))
        return bgetByAuthorYear(bytes)
    }

    private fun bgetByAuthors(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authorDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByAuthors(authors: String) : MutableSet<Long> {
        val bytes = encode(authors)
        return bgetByAuthors(bytes)
    }

    private fun bgetByFirsLastPageVolume(bytes: ByteArray): MutableSet<Long> {
        val recordsBytes = flVolDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByFirsLastPageVolume(fpage: Int, lpage: Int, vol: String): MutableSet<Long> {
        val bytes = encode(Triple(fpage, lpage, vol))
        return bgetByFirsLastPageVolume(bytes)
    }

    private fun encode(a: Any): ByteArray {
        return Klaxon().toJsonString(a).toByteArray()
    }

    private fun sencode(a: Any): String {
        return Klaxon().toJsonString(a)
    }

    private fun decodeIds(bytes: ByteArray) : MutableSet<Long>? {
        return Klaxon().parseArray<Long>(String(bytes))?.toMutableSet()
    }

    fun getFirstAuthorLetters(record: SemanticScholarData) : String {
        return record.authors.joinToString(separator = ",") { author ->
            val words = author.name.split("""\s""".toRegex()).filter {!it.isBlank()}
            words.joinToString(separator = "") { it[0].toString() }
        }
    }

    private fun storeRecordLocal(record: SemanticScholarData) {
        val id = currentId.getAndIncrement()
        val recordBytes = Klaxon().toJsonString(record).toByteArray()
        val idString = Klaxon().toJsonString(id)
        mainKeys[idString] = recordBytes

        if (!record.title.isNullOrBlank()) {
            val str = record.title!!
            titleKeys.getOrPut(str, {mutableListOf()})
            titleKeys[str]!!.add(id)
        }

        if (!record.journalName.isNullOrBlank() && record.firstPage != null) {
            val str = sencode(Pair(record.journalName, record.firstPage))
            jpageKeys.getOrPut(str, {mutableListOf()})
            jpageKeys[str]!!.add(id)
        }

        if (!record.journalVolume.isNullOrBlank() &&
                record.firstPage != null && record.year != null) {
            val str = sencode(Triple(record.journalVolume, record.firstPage, record.year))
            volPageYearKeys.getOrPut(str, {mutableListOf()})
            volPageYearKeys[str]!!.add(id)
        }

        if (record.authors.size >= 2) {
            val authorString = getFirstAuthorLetters(record)
            if (!record.journalVolume.isNullOrBlank()) {
                val str = sencode(Pair(authorString, record.journalVolume))
                authorVolumeKeys.getOrPut(str, {mutableListOf()})
                authorVolumeKeys[str]!!.add(id)
            }

            if (record.firstPage != null) {
                val str = sencode(Pair(authorString, record.firstPage))
                authorPageKeys.getOrPut(str, {mutableListOf()})
                authorPageKeys[str]!!.add(id)
            }

            if (record.year != null) {
                val str = sencode(Pair(authorString, record.year))
                authorYearKeys.getOrPut(str, {mutableListOf()})
                authorYearKeys[str]!!.add(id)
            }
        }

        if (record.authors.size >= 3) {
            val authorString = getFirstAuthorLetters(record)
            val str = sencode(authorString)
            authorKeys.getOrPut(str, {mutableListOf()})
            authorKeys[str]!!.add(id)
        }

        if (!record.journalVolume.isNullOrBlank() &&
                record.firstPage != null && record.lastPage != null) {
            val str = sencode(Triple(record.firstPage, record.lastPage, record.journalVolume))
            flVolKeys.getOrPut(str, {mutableListOf()})
            flVolKeys[str]!!.add(id)
        }
    }

    private fun getFullLists() {
        fun updateLists(db: RocksDB, dbkeys: Map<String, MutableList<Long>>) {
            val keys = dbkeys.keys.toList()
            keys.chunked(5000).flatMap {db.multiGetAsList(it.map {it.toByteArray()})}
                    .map {if (it == null) mutableListOf<Long>() else (decodeIds(it) ?: mutableListOf<Long>())}
                    .zip(keys).forEach { (list, key) -> dbkeys[key]!!.addAll(list)}
        }

        updateLists(titleDb, titleKeys)
        titleKeys.forEach{(_, list) -> stats.maxTitleLength = max(stats.maxTitleLength, list.size)}

        updateLists(jpageDb, jpageKeys)
        jpageKeys.forEach{(_, list) -> stats.maxJPageLength = max(stats.maxJPageLength, list.size)}

        updateLists(volPageYearDb, volPageYearKeys)
        volPageYearKeys.forEach{(_, list) ->
            stats.maxVolPageYearLength = max(stats.maxVolPageYearLength, list.size)}

        updateLists(authorYearDb, authorYearKeys)
        authorYearKeys.forEach{(_, list) ->
            stats.maxAuthorYearLength = max(stats.maxAuthorYearLength, list.size)}

        updateLists(authorVolumeDb, authorVolumeKeys)
        authorVolumeKeys.forEach{(_, list) ->
            stats.maxAuthorVolumeLength = max(stats.maxAuthorVolumeLength, list.size)}

        updateLists(authorPageDb, authorPageKeys)
        authorPageKeys.forEach{(_, list) ->
            stats.maxAuthorPageLength = max(stats.maxAuthorPageLength, list.size)}

        updateLists(authorDb, authorKeys)
        authorKeys.forEach{(_, list) -> stats.maxAuthorLength = max(stats.maxAuthorLength, list.size)}

        updateLists(flVolDb, flVolKeys)
        flVolKeys.forEach{(_, list) -> stats.maxFLVolLength = max(stats.maxFLVolLength, list.size)}
    }

    private fun storeAllKeys() {
        logger.info("Begin keys sorting")
        val cmp = Comparator<Pair<ByteArray, *>> { p0, p1 ->
            fun posByte(b: Byte): Int {
                return (b + 256) % 256
            }
            p0.first.zip(p1.first).forEach {(b1, b2) ->
                val dif = posByte(b1) - posByte(b2)
                if (dif != 0) {
                    return@Comparator dif
                }
            }
            return@Comparator p0.first.size - p1.first.size
        }
        fun <T> getSortedByteKeys(m: Map<String, T>): List<Pair<ByteArray, T>> {
            return m.toList().map {(first, second) -> Pair(first.toByteArray(), second)}.sortedWith(cmp)
        }
        val sMainKeys = getSortedByteKeys(mainKeys)
        val sTitleKeys = getSortedByteKeys(titleKeys)
        val sJpageKeys = getSortedByteKeys(jpageKeys)
        val sVolPageYearKeys = getSortedByteKeys(volPageYearKeys)
        val sAuthorYearKeys = getSortedByteKeys(authorYearKeys)
        val sAuthorVolumeKeys = getSortedByteKeys(authorVolumeKeys)
        val sAuthorPageKeys = getSortedByteKeys(authorPageKeys)
        val sAuthorKeys = getSortedByteKeys(authorKeys)
        val sFlVolKeys = getSortedByteKeys(flVolKeys)

        logger.info("Begin creating sst files")

        val sMainPath = generateRandomSstName(mainDbPath)
        val sMain = SstFileWriter(EnvOptions(), options)
        sMain.open(sMainPath)
        sMainKeys.forEach { sMain.put(it.first, it.second)}

        val sTitlePath = generateRandomSstName(titleDbPath)
        val sTitle = SstFileWriter(EnvOptions(), options)
        sTitle.open(sTitlePath)
        sTitleKeys.forEach {sTitle.put(it.first, encode(it.second))}

        val sJpagePath = generateRandomSstName(jpageDbPath)
        val sJpage = SstFileWriter(EnvOptions(), options)
        sJpage.open(sJpagePath)
        sJpageKeys.forEach { sJpage.put(it.first, encode(it.second))}

        val sVolPageYearPath = generateRandomSstName(volPageYearDbPath)
        val sVolPageYear = SstFileWriter(EnvOptions(), options)
        sVolPageYear.open(sVolPageYearPath)
        sVolPageYearKeys.forEach { sVolPageYear.put(it.first, encode(it.second))}

        val sAuthorYearPath = generateRandomSstName(authorYDbPath)
        val sAuthorYear = SstFileWriter(EnvOptions(), options)
        sAuthorYear.open(sAuthorYearPath)
        sAuthorYearKeys.forEach { sAuthorYear.put(it.first, encode(it.second))}

        val sAuthorVolumePath = generateRandomSstName(authorVDbPath)
        val sAuthorVolume = SstFileWriter(EnvOptions(), options)
        sAuthorVolume.open(sAuthorVolumePath)
        sAuthorVolumeKeys.forEach { sAuthorVolume.put(it.first, encode(it.second))}

        val sAuthorPagePath = generateRandomSstName(authorPDbPath)
        val sAuthorPage = SstFileWriter(EnvOptions(), options)
        sAuthorPage.open(sAuthorPagePath)
        sAuthorPageKeys.forEach { sAuthorPage.put(it.first, encode(it.second))}

        val sAuthorPath = generateRandomSstName(authorDbPath)
        val sAuthor = SstFileWriter(EnvOptions(), options)
        sAuthor.open(sAuthorPath)
        sAuthorKeys.forEach { sAuthor.put(it.first, encode(it.second))}

        val sFlVolPath = generateRandomSstName(flVolDbPath)
        val sFlVol = SstFileWriter(EnvOptions(), options)
        sFlVol.open(sFlVolPath)
        sFlVolKeys.forEach { sFlVol.put(it.first, encode(it.second))}

        sMain.finish()
        sTitle.finish()
        sJpage.finish()
        sVolPageYear.finish()
        sAuthorYear.finish()
        sAuthorVolume.finish()
        sAuthorPage.finish()
        sAuthor.finish()
        sFlVol.finish()

        logger.info("Begin adding sst file to the databases")

        val ifo = IngestExternalFileOptions()
        mainDb.ingestExternalFile(listOf(sMainPath), ifo)
        titleDb.ingestExternalFile(listOf(sTitlePath), ifo)
        jpageDb.ingestExternalFile(listOf(sJpagePath), ifo)
        volPageYearDb.ingestExternalFile(listOf(sVolPageYearPath), ifo)
        authorYearDb.ingestExternalFile(listOf(sAuthorYearPath), ifo)
        authorVolumeDb.ingestExternalFile(listOf(sAuthorVolumePath), ifo)
        authorPageDb.ingestExternalFile(listOf(sAuthorPagePath), ifo)
        authorDb.ingestExternalFile(listOf(sAuthorPath), ifo)
        flVolDb.ingestExternalFile(listOf(sFlVolPath), ifo)
    }

    private fun generateRandomSstName(path: File): String {
        val randomString = RandomStringUtils.randomAlphanumeric(10)
        val newFile = File(path, "$randomString.sst")
        if (!newFile.exists()) {
            return newFile.absolutePath
        }
        else {
            return generateRandomSstName(path)
        }
    }

    fun reloadDb() {
        closeDb()
        sleep(10000)
        openDb()
    }

    private fun openDb() {
        mainDb = RocksDB.open(options, mainDbPath.absolutePath)
        titleDb = RocksDB.open(options, titleDbPath.absolutePath)
        jpageDb = RocksDB.open(options, jpageDbPath.absolutePath)
        volPageYearDb = RocksDB.open(options, volPageYearDbPath.absolutePath)
        authorYearDb = RocksDB.open(options, authorYDbPath.absolutePath)
        authorVolumeDb = RocksDB.open(options, authorVDbPath.absolutePath)
        authorPageDb = RocksDB.open(options, authorPDbPath.absolutePath)
        authorDb = RocksDB.open(options, authorDbPath.absolutePath)
        flVolDb = RocksDB.open(options, flVolDbPath.absolutePath)
    }

    private fun closeDb() {
        mainDb.closeE()
        titleDb.closeE()
        volPageYearDb.closeE()
        jpageDb.closeE()
        authorVolumeDb.closeE()
        authorYearDb.closeE()
        authorPageDb.closeE()
        authorDb.closeE()
        flVolDb.closeE()
    }

    override fun close() {
        closeDb()
        options.close()
    }
}