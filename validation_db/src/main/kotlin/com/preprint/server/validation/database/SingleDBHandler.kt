package com.preprint.server.validation.database

import com.beust.klaxon.Klaxon
import com.jsoniter.JsonIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.kotlin.logger
import org.rocksdb.*
import java.io.File
import java.lang.Integer.max
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicLong
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

internal class SingleDBHandler(val dbFolderPath: File) : AutoCloseable {
    private var currentId = AtomicLong(0)

    private val mainDbPath = File(dbFolderPath, "main")
    private val titleDbPath = File(dbFolderPath, "title")
    private val authVolPageYearDbPath = File(dbFolderPath, "volpy")
    private val authorVYDbPath = File(dbFolderPath, "authorv")
    private val authorPYDbPath = File(dbFolderPath, "authorp")
    private val authFlVolDbPath = File(dbFolderPath, "flvol")
    private val doiPath = File(dbFolderPath, "doi")

    private lateinit var mainDb: RocksDB
    private lateinit var titleDb: RocksDB
    private lateinit var authVolPageYearDb: RocksDB
    private lateinit var authorVolumeYearDb: RocksDB
    private lateinit var authorPageYearDb: RocksDB
    private lateinit var authFlVolDb: RocksDB
    lateinit var doiDb: RocksDB

    private val mainKeys = HashMap<String, ByteArray>()
    private val titleKeys = HashMap<String, MutableList<Long>>()
    private val authVolPageYearKeys = HashMap<String, MutableList<Long>>()
    private val authorVolumeYearKeys = HashMap<String, MutableList<Long>>()
    private val authorPageYearKeys = HashMap<String, MutableList<Long>>()
    private val authFlVolKeys = HashMap<String, MutableList<Long>>()
    private val doiKeys = HashMap<String, MutableList<Long>>()

    private val options: Options

    private val logger = logger()

    private val maxIdPath = File(dbFolderPath, "ID.txt")

    private var lastTime: Long? = null

    private var lastReloadTime: Long? = null

    private val maxValueLength = ValidationDBConfig.config["max_value_length"].toString().toInt()

    private val timeLimit = ValidationDBConfig.config["time_limit"].toString().toLong()

    data class Stats(
            var maxTitleLength: Int = 0,
            var maxAuthVolPageYearLength: Int = 0,
            var maxAuthorPageLength: Int = 0,
            var maxAuthorVolumeLength: Int = 0,
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

        val bopt = BlockBasedTableConfig()
                .setCacheIndexAndFilterBlocks(true).setBlockSize(64000)
                .setFilterPolicy(BloomFilter(10.0, false))
        options = Options().setCreateIfMissing(true)
                .setMaxSuccessiveMerges(1000)
                .setOptimizeFiltersForHits(true)
                .setNewTableReaderForCompactionInputs(true)
                .setTableFormatConfig(bopt)
        openDb()
        compactDb(true)
    }
    fun storeRecords(records: List<UniversalData>) {
        mainKeys.clear()
        titleKeys.clear()
        authVolPageYearKeys.clear()
        authorVolumeYearKeys.clear()
        authorPageYearKeys.clear()
        authFlVolKeys.clear()
        doiKeys.clear()

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
                || currentTime > timeLimit
        ) {
            if (currentTime > lastTime!! * 1.5) {
                compactDb(true)
            }
            logger.info("Reloading databases")
            reloadDb()
            lastTime = currentTime
        } else {
            lastTime = currentTime
        }
        println(stats)
    }

    private fun bgetById(bytes: ByteArray) : UniversalData? {
        return Klaxon().parse<UniversalData>(String(mainDb.get(bytes) ?: return null))
    }

    fun getById(id : Long) : UniversalData? {
        val kbytes = encode(id)
        return bgetById(kbytes)
    }

    fun mgetById(ids: List<Long>): List<UniversalData?> {
        return mainDb.multiGetAsList(ids.map {encode(it)}).map {bytes ->
                    if (bytes != null) {
                        JsonIterator.deserialize(String(bytes), UniversalData::class.java)
                    } else {
                        null
                    }
                }
    }

    fun mget(db: RocksDB, list: List<String?>): List<List<Long>> {
        val slist = list.mapIndexed {i, v -> Pair(v, i)}.sortedBy { it.first }.filter {!it.first.isNullOrBlank()}
        val resp = db.multiGetAsList(slist.map {it.first!!.toByteArray()})
        val res = MutableList<List<Long>>(list.size, {listOf()})
        resp.zip(slist).forEach {ba -> res[ba.second.second] = ba.first?.let {decodeIds(it)?.toList()} ?: listOf()}
        return res
    }

    fun getUdata(ids: List<List<Long>>): List<List<UniversalData>> {
        return ids.map {mgetById(it).filterNotNull()}
    }

    private fun bgetByTitle(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = titleDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: return mutableSetOf()
    }

    fun sencodeTitle(title: String): String {
        return getShortenedTitle(title)
    }

    fun getByTitle(title: String) : MutableSet<Long> {
        val bytes = sencodeTitle(title).toByteArray()
        return bgetByTitle(bytes)
    }

    fun mgetByTitle(titleList: List<UniversalData>): List<List<UniversalData>> {
        return getUdata(mget(titleDb, titleList.map {sencodeTitle(it.title ?: return@map null)}))
    }

    data class AuthVolPageYear(
            val auth: String,
            val vol: String,
            val page: Int,
            val year: Int
    )

    private fun bgetByAuthVolPageYear(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authVolPageYearDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun sencodeAuthVolPageYear(auth: String, vol: String, firstPage: Int, year: Int): String {
        return sencode(
            AuthVolPageYear(
                auth,
                vol,
                firstPage,
                year
            )
        )
    }

    fun getByAuthVolPageYear(auth: String, volume: String, firstPage: Int, year: Int) : MutableSet<Long> {
        val bytes = sencodeAuthVolPageYear(auth, volume, firstPage, year).toByteArray()
        return bgetByAuthVolPageYear(bytes)
    }

    fun mgetByAuthVolPageYear(list: List<UniversalData>): List<List<UniversalData>> {
        return getUdata(
            mget(authVolPageYearDb,
                list.map {
                    val auth = DBHandler.getFirstAuthorLetters(it.authors.map {it.name})
                    if (it.authors.isNotEmpty() && !it.journalVolume.isNullOrBlank()
                        && it.firstPage != null && it.year != null
                    ) {
                        sencodeAuthVolPageYear(auth, it.journalVolume!!, it.firstPage!!, it.year)
                    }
                    else {
                        null
                    }
                }
            )
        )
    }

    fun sencodeAuthorVolume(auth: String, vol: String, year: Int): String {
        return sencode(Triple(auth, vol, year))
    }

    private fun bgetByAuthorVolume(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authorVolumeYearDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByAuthorVolume(authors: String, volume: String, year: Int) : MutableSet<Long> {
        val bytes = sencodeAuthorVolume(authors, volume, year).toByteArray()
        return bgetByAuthorVolume(bytes)
    }

    fun mgetByAuthVolume(list: List<UniversalData>): List<List<UniversalData>> {
        return getUdata(
            mget(authorVolumeYearDb,
                list.map {
                    val auth = DBHandler.getFirstAuthorLetters(it.authors.map {it.name})
                    if (it.authors.size >= 2 && !it.journalVolume.isNullOrBlank() && it.year != null) {
                        sencodeAuthorVolume(auth, it.journalVolume!!, it.year)
                    }
                    else {
                        null
                    }
                }
            )
        )
    }

    fun sencodeAuthorPage(authors: String, firstPage: Int, year: Int): String {
        return sencode(Triple(authors, firstPage, year))
    }

    private fun bgetByAuthorPage(bytes: ByteArray) : MutableSet<Long> {
        val recordsBytes = authorPageYearDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    fun getByAuthorPage(authors: String, firstPage: Int, year: Int) : MutableSet<Long> {
        val bytes = sencodeAuthorPage(authors, firstPage, year).toByteArray()
        return bgetByAuthorPage(bytes)
    }

    fun mgetByAuthPage(list: List<UniversalData>): List<List<UniversalData>> {
        return getUdata(
            mget(authorPageYearDb,
                list.map {
                    val auth = DBHandler.getFirstAuthorLetters(it.authors.map {it.name})
                    if (it.authors.size >= 2 && it.firstPage != null && it.year != null) {
                        sencodeAuthorPage(auth, it.firstPage!!, it.year)
                    }
                    else {
                        null
                    }
                }
            )
        )
    }

    private fun bgetByFirsLastPageVolume(bytes: ByteArray): MutableSet<Long> {
        val recordsBytes = authFlVolDb.get(bytes) ?: return mutableSetOf()
        return decodeIds(recordsBytes) ?: mutableSetOf()
    }

    data class AuthFLVolume(
            val auth: String,
            val fpage: Int,
            val lpage: Int,
            val vol: String
    )

    fun sencodeAuthFLVol(auth: String, fpage: Int, lpage: Int, vol: String): String {
        return sencode(
            AuthFLVolume(
                auth,
                fpage,
                lpage,
                vol
            )
        )
    }

    fun getByFirsLastPageVolume(auth: String, fpage: Int, lpage: Int, vol: String): MutableSet<Long> {
        val bytes = sencodeAuthFLVol(auth, fpage, lpage, vol).toByteArray()
        return bgetByFirsLastPageVolume(bytes)
    }

    fun mgetByAuthFLVolume(list: List<UniversalData>): List<List<UniversalData>> {
        return getUdata(
            mget(authFlVolDb,
                list.map {
                    val auth = DBHandler.getFirstAuthorLetters(it.authors.map {it.name})
                    if (it.authors.size >= 2 && it.firstPage != null
                        && it.lastPage != null && !it.journalVolume.isNullOrBlank()
                    ) {
                        sencodeAuthFLVol(auth, it.firstPage!!, it.lastPage!!, it.journalVolume!!)
                    }
                    else {
                        null
                    }
                }
            )
        )
    }

    private fun encode(a: Any): ByteArray {
        return Klaxon().toJsonString(a).toByteArray()
    }

    private fun sencode(a: Any): String {
        return Klaxon().toJsonString(a)
    }

    private fun decodeIds(bytes: ByteArray) : MutableSet<Long>? {
        val ids = Klaxon().parseArray<Long>(String(bytes))?.toMutableSet()
        if (ids != null && ids.size > maxValueLength) {
            return ids.take(maxValueLength).toMutableSet()
        }
        else {
            return ids
        }
    }

    fun getShortenedTitle(title: String): String {
        val st =  title.toLowerCase().filter { it.isLetter() }
        return if (st.isEmpty()) title
               else st
    }

    private fun storeRecordLocal(record: UniversalData) {
        val id = currentId.getAndIncrement()
        val recordBytes = Klaxon().toJsonString(record).toByteArray()
        val idString = Klaxon().toJsonString(id)
        mainKeys[idString] = recordBytes

        if (!record.title.isNullOrBlank()) {
            val str = getShortenedTitle(record.title!!)
            titleKeys.getOrPut(str, {mutableListOf()})
            titleKeys[str]!!.add(id)
        }

        if (!record.journalVolume.isNullOrBlank() &&
                record.firstPage != null && record.year != null && record.authors.size > 0) {
            val auth =
                DBHandler.getFirstAuthorLetters(
                    record.authors.map { it.name })
            val str = sencode(
                AuthVolPageYear(
                    auth,
                    record.journalVolume!!,
                    record.firstPage!!,
                    record.year
                )
            )
            authVolPageYearKeys.getOrPut(str, {mutableListOf()})
            authVolPageYearKeys[str]!!.add(id)
        }

        if (record.authors.size >= 2 && record.year != null) {
            val authorString =
                DBHandler.getFirstAuthorLetters(
                    record.authors.map { it.name })
            if (!record.journalVolume.isNullOrBlank()) {
                val str = sencode(Triple(authorString, record.journalVolume, record.year))
                authorVolumeYearKeys.getOrPut(str, {mutableListOf()})
                authorVolumeYearKeys[str]!!.add(id)
            }

            if (record.firstPage != null) {
                val str = sencode(Triple(authorString, record.firstPage, record.year))
                authorPageYearKeys.getOrPut(str, {mutableListOf()})
                authorPageYearKeys[str]!!.add(id)
            }
        }

        if (!record.journalVolume.isNullOrBlank() &&
                record.firstPage != null && record.lastPage != null && record.authors.isNotEmpty()) {
            val auth =
                DBHandler.getFirstAuthorLetters(
                    record.authors.map { it.name })
            val str = sencode(
                AuthFLVolume(
                    auth,
                    record.firstPage!!,
                    record.lastPage!!,
                    record.journalVolume!!
                )
            )
            authFlVolKeys.getOrPut(str, {mutableListOf()})
            authFlVolKeys[str]!!.add(id)
        }

        if (!record.doi.isNullOrBlank()) {
            doiKeys.getOrPut(record.doi, { mutableListOf()})
            doiKeys[record.doi]!!.add(id)
        }
    }

    private fun getFullLists() {
        fun updateLists(db: RocksDB, dbkeys: MutableMap<String, MutableList<Long>>) {
            val keys = dbkeys.keys.toList().sorted()
            keys.forEach{key ->
                db.get(key.toByteArray())?.let {bytes ->
                    decodeIds(bytes)?.let {idList ->
                        dbkeys[key]!!.addAll(idList)
                    }
                }
                if (dbkeys[key]!!.size > maxValueLength) {
                    dbkeys[key] = dbkeys[key]!!.take(maxValueLength).toMutableList()
                }
            }
        }
        runBlocking(Dispatchers.Default) {

            launch {
                updateLists(titleDb, titleKeys)
                titleKeys.forEach { (_, list) -> stats.maxTitleLength = max(stats.maxTitleLength, list.size) }
            }

            launch {
                updateLists(authVolPageYearDb, authVolPageYearKeys)
                authVolPageYearKeys.forEach { (_, list) ->
                    stats.maxAuthVolPageYearLength = max(stats.maxAuthVolPageYearLength, list.size)
                }
            }

            launch {
                updateLists(authorVolumeYearDb, authorVolumeYearKeys)
                authorVolumeYearKeys.forEach { (_, list) ->
                    stats.maxAuthorVolumeLength = max(stats.maxAuthorVolumeLength, list.size)
                }
            }

            launch {
                updateLists(authorPageYearDb, authorPageYearKeys)
                authorPageYearKeys.forEach { (_, list) ->
                    stats.maxAuthorPageLength = max(stats.maxAuthorPageLength, list.size)
                }
            }

            launch {
                updateLists(authFlVolDb, authFlVolKeys)
                authFlVolKeys.forEach { (_, list) -> stats.maxFLVolLength = max(stats.maxFLVolLength, list.size) }
            }

            launch {
                updateLists(doiDb, doiKeys)
            }
        }
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
        val limitLengthFun: (Pair<ByteArray, List<Long>>) -> Boolean =
                {(_, list) -> list.size < maxValueLength}
        val sMainKeys = getSortedByteKeys(mainKeys)
        val sTitleKeys = getSortedByteKeys(titleKeys).filter {limitLengthFun(it)}
        val sVolPageYearKeys = getSortedByteKeys(authVolPageYearKeys).filter {limitLengthFun(it)}
        val sAuthorVolumeKeys = getSortedByteKeys(authorVolumeYearKeys).filter {limitLengthFun(it)}
        val sAuthorPageKeys = getSortedByteKeys(authorPageYearKeys).filter {limitLengthFun(it)}
        val sFlVolKeys = getSortedByteKeys(authFlVolKeys).filter {limitLengthFun(it)}
        val sDoiKeys = getSortedByteKeys(doiKeys).filter(limitLengthFun)

        logger.info("Begin creating WriteBatch objects")

        val sMain = WriteBatch()
        sMainKeys.forEach { sMain.put(it.first, it.second)}

        val sTitle = WriteBatch()
        sTitleKeys.forEach {sTitle.put(it.first, encode(it.second))}

        val sVolPageYear = WriteBatch()
        sVolPageYearKeys.forEach { sVolPageYear.put(it.first, encode(it.second))}

        val sAuthorVolume =  WriteBatch()
        sAuthorVolumeKeys.forEach { sAuthorVolume.put(it.first, encode(it.second))}

        val sAuthorPage = WriteBatch()
        sAuthorPageKeys.forEach { sAuthorPage.put(it.first, encode(it.second))}

        val sFlVol = WriteBatch()
        sFlVolKeys.forEach { sFlVol.put(it.first, encode(it.second))}

        val sDoi = WriteBatch()
        sDoiKeys.forEach { sDoi.put(it.first, encode(it.second)) }

        logger.info("Begin batch write to the databases")

        runBlocking(Dispatchers.Default) {
            launch {
                mainDb.write(WriteOptions(), sMain)
            }
            launch {
                titleDb.write(WriteOptions(), sTitle)
            }
            launch {
                authVolPageYearDb.write(WriteOptions(), sVolPageYear)
            }
            launch {
                authorVolumeYearDb.write(WriteOptions(), sAuthorVolume)
            }
            launch {
                authorPageYearDb.write(WriteOptions(), sAuthorPage)
            }
            launch {
                authFlVolDb.write(WriteOptions(), sFlVol)
            }

            launch {
                doiDb.write(WriteOptions(), sDoi)
            }
        }
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
        sleep(5000)
        openDb()
        compactDb()
    }

    private fun openDb() {
        mainDb = RocksDB.open(options, mainDbPath.absolutePath)
        titleDb = RocksDB.open(options, titleDbPath.absolutePath)
        authVolPageYearDb = RocksDB.open(options, authVolPageYearDbPath.absolutePath)
        authorVolumeYearDb = RocksDB.open(options, authorVYDbPath.absolutePath)
        authorPageYearDb = RocksDB.open(options, authorPYDbPath.absolutePath)
        authFlVolDb = RocksDB.open(options, authFlVolDbPath.absolutePath)
        doiDb = RocksDB.open(options, doiPath.absolutePath)
    }

    fun compactDb(ignoreTime: Boolean = false) {
        if (lastReloadTime != null && !ignoreTime) {
            val diff = System.currentTimeMillis() - lastReloadTime!!
            if (diff < 1000 * 60 * 90) {
                logger.info("Compaction can only be done once every three hours")
                return
            }
        }
        logger.info("Begin compaction")
        runBlocking {
            launch {
                mainDb.compactRange()
            }
            launch {
                titleDb.compactRange()
            }
            launch {
                authVolPageYearDb.compactRange()
            }
            launch {
                authorVolumeYearDb.compactRange()
            }
            launch {
                authorPageYearDb.compactRange()
            }
            launch {
                authFlVolDb.compactRange()
            }

            launch { doiDb.compactRange() }
        }
        lastReloadTime = System.currentTimeMillis()
    }

    private fun closeDb() {
        mainDb.closeE()
        titleDb.closeE()
        authVolPageYearDb.closeE()
        authorVolumeYearDb.closeE()
        authorPageYearDb.closeE()
        authFlVolDb.closeE()
        doiDb.closeE()
    }

    override fun close() {
        closeDb()
        options.close()
    }
}