package com.preprint.server.arxivs3

import com.preprint.server.core.arxiv.ArxivAPI
import com.preprint.server.core.data.Reference
import com.preprint.server.core.neo4j.DatabaseHandler
import com.preprint.server.core.ref.ReferenceExtractor
import com.preprint.server.core.validation.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.logging.log4j.kotlin.logger
import java.io.*
import java.lang.Exception
import java.nio.file.Paths
import java.util.concurrent.Executors

/**
 * Download all archives(in .tar format) from Amazon S3 "requester pays" bucket sequentially,
 * extract references and store all data into the database.
 */
object ArxivS3Collector {
    private val logger = logger()
    private val path = ArxivS3Config.config["arxiv_pdf_path"].toString()
    private val bufferSize = ArxivS3Config.config["buffer_size"].toString().toInt() //33554432
    private val manifestFileName = "manifest.xml"


    /**
     * Uses previosly created dbHandler to store the data.
     * Validators will be applied to references after extraction
     * in the order that they stored in the list
     * FixedThreadPool with `maxThreads` will be used during
     * the reference extraction, or `Dispatchers.Default` from coroutines
     * library if `maxThreads` = -1
     *
     * `maxParallelDownload` is the number of archives that
     * will be loaded at the same time
     *
     * If dbHandler is null, then this method will only download archives
     */
    fun beginBulkDownload(
        dbHandler: DatabaseHandler?,
        referenceExtractor: ReferenceExtractor?,
        validators: List<Validator>,
        maxParallelDownload: Int,
        maxThreads: Int = -1
    ) {
        if (dbHandler == null) {
            logger.info("Download only mode")
        }
        File("$path/pdf/").mkdir()
        val manifestPath = "$path/$manifestFileName"
        ArxivS3Downloader.downloadManifest(manifestPath)

        //get filenames and md5 hash of each file from manifest
        val fileNames = ManifestParser.parseFilenames(manifestPath)
        fileNames.chunked(maxParallelDownload).forEach { fileNamesChunk ->
            runBlocking(Dispatchers.IO) {
                fileNamesChunk.forEach { (filename, md5sum) ->
                    val pdfPath = "$path/$filename"

                    launch {
                        //download archive only if it wasn't downloaded before
                        if (!File(pdfPath).exists() || !compareMD5(pdfPath, md5sum)) {
                            ArxivS3Downloader.download(filename, pdfPath)
                        } else {
                            logger.info("$filename is already downloaded")
                        }

                        if (dbHandler != null) {
                            synchronized(dbHandler) {
                                processFile(pdfPath, dbHandler, referenceExtractor, validators, maxThreads)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Extracts all pdf files from one archive(with `filename` name)
     * Then extracts references from all pdf's concurrently
     * and stored data into the database
     */
    private fun processFile(
        filename: String,
        dbHandler: DatabaseHandler,
        referenceExtractor: ReferenceExtractor?,
        validators: List<Validator>,
        maxThreads: Int
    ) {
        //this directory will be used to store extracted pdf
        //and will be deleted after processing all of them
        val outputDir = File("$path/tmp")
        deleteDir(outputDir)
        outputDir.mkdir()

        try {
            val filenames = unzip(filename, outputDir)
            val ids = filenames.map {getIdFromFilename(it)}

            //get metadata about each extracted pdf
            val records = ArxivAPI.getArxivRecords(ids)

            if (referenceExtractor != null) {
                val dispatcher =
                    if (maxThreads == -1) Dispatchers.Default
                    else Executors.newFixedThreadPool(maxThreads).asCoroutineDispatcher()

                runBlocking(dispatcher) {
                    records.zip(filenames).forEach { (record, filepath) ->
                        launch {
                            println(record.pdfUrl)
                            record.refList = getRefList(filepath, referenceExtractor, validators)
                        }
                    }
                }

                val refs = mutableListOf<Reference>()
                records.forEach {refs.addAll(it.refList)}
                logger.info("Begin validation")

                runBlocking {
                    validators.forEach { validator ->
                        validator.validate(refs)
                    }
                }

                logger.info("Validated ${refs.count { it.validated }} out of ${refs.size}")
            }
            dbHandler.storeArxivData(records)
        }
        finally {
            deleteDir(outputDir)
        }
    }

    /**
     * Extracts files from tar archive(whith `inputFilePath` path) and
     * stores all extracted files in the `outputDir`
     * Returns list of paths to files
     */
    private fun unzip(inputFilePath: String, outputDir: File) : List<String> {
        val untaredFiles = mutableListOf<String>()
        val inputStream: InputStream = FileInputStream(File(inputFilePath))

        val debInputStream =
            ArchiveStreamFactory().createArchiveInputStream("tar", inputStream)
                    as TarArchiveInputStream

        var entry: TarArchiveEntry?
        while (debInputStream.nextTarEntry.also { entry = it } != null) {
            if (!entry!!.isDirectory) {
                val file = File(outputDir, Paths.get(entry!!.name).fileName.toString())
                val outputStream = FileOutputStream(file)
                org.apache.commons.io.IOUtils.copy(debInputStream, outputStream)
                outputStream.close()
                untaredFiles.add(file.absolutePath)
            }
        }
        return untaredFiles
    }

    /**
     * Deleted directory and all stored files(not recursievly)
     */
    private fun deleteDir(dir: File) {
        val files = dir.listFiles()?.filterNotNull() ?: return
        for (file in files) {
            file.delete()
        }
        dir.delete()
    }

    /**
     * This function relies on the fact that filename
     * is exactly the arxivId but without '/'
     * Returns valid arxiv id
     */
    private fun getIdFromFilename(filename: String) : String {
        val fn = Paths.get(filename).fileName.toString()
        val i = fn.indexOfFirst { it.isDigit() }
        if (i == 0) {
            return fn
        }
        return fn.substring(0, i) + "/" + fn.substring(i).dropLast(4)
    }


    /**
     * Extracts references from the given file(with `filepath` path)
     * Validates references with each validator from `validators`
     */
    private fun getRefList(
        filepath: String,
        referenceExtractor: ReferenceExtractor,
        validators: List<Validator>
    ) : MutableList<Reference>{
        try {
            val refs = referenceExtractor.extractUnverifiedReferences(
                File(filepath).readBytes()
            ).toMutableList()

            return refs
        } catch (e : Exception) {
            return mutableListOf()
        }
    }

    /**
     * compares md5hash of the file with `md5sumToCompare`
     */
    private fun compareMD5(path : String, md5sumToCompare : String) : Boolean {
        val md = DigestUtils.md5Hex(BufferedInputStream(FileInputStream(path), bufferSize))
        return md == md5sumToCompare
    }
}