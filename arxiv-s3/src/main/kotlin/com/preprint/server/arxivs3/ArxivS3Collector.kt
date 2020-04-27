package com.preprint.server.arxivs3

import com.preprint.server.Config
import com.preprint.server.arxiv.ArxivAPI
import com.preprint.server.data.Reference
import com.preprint.server.neo4j.DatabaseHandler
import com.preprint.server.ref.ReferenceExtractor
import com.preprint.server.validation.Validator
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.nio.file.Paths
import java.security.MessageDigest


object ArxivS3Collector {
    val logger = logger()
    val path = Config.config["arxiv_pdf_path"].toString()
    val manifestFileName = "manifest.xml"
    fun beginBulkDownload(
        dbHandler : DatabaseHandler,
        referenceExtractor: ReferenceExtractor?,
        validators : List<Validator>
    ) {
        File("$path/pdf/").mkdir()
        val manifestPath = "$path/$manifestFileName"
        if (!File(manifestPath).exists()) {
            ArxivS3Downloader.downloadManifest(manifestPath)
        }
        else {
            logger.info("manifest is already downloaded")
        }

        val fileNames = ManifestParser.parseFilenames(manifestPath)
        fileNames.forEach {(filename, md5sum) ->
            val pdfPath = "$path/$filename"
            if (!File(pdfPath).exists() || !compareMD5(pdfPath, md5sum)) {
                ArxivS3Downloader.download(filename, path + "/" + pdfPath)
            }
            else {
                logger.info("$filename is already downloaded")
            }
            processFile(pdfPath, dbHandler, referenceExtractor, validators)
        }
    }

    private fun processFile(
        filename : String,
        dbHandler: DatabaseHandler,
        referenceExtractor: ReferenceExtractor?,
        validators: List<Validator>
    ) {
        val outputDir = File("$path/tmp")
        outputDir.mkdir()
        try {
            val filenames = unzip(filename, outputDir)
            val ids = filenames.map {getIdFromFilename(it)}
            val records = ArxivAPI.getArxivRecords(ids)
            if (referenceExtractor != null) {
               records.zip(filenames).forEach { (record, filepath) ->
                   record.refList = getRefList(filepath, referenceExtractor, validators)
               }
            }
            dbHandler.storeArxivData(records)
        }
        finally {
            deleteDir(outputDir)
        }
    }

    private fun unzip(input: String, outputDir : File) : List<String> {
        val untaredFiles = mutableListOf<String>()
        val inputStream: InputStream = FileInputStream(File(input))
        val debInputStream =
            ArchiveStreamFactory().createArchiveInputStream("tar", inputStream) as TarArchiveInputStream
        var entry: TarArchiveEntry? = null
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

    private fun deleteDir(dir : File) {
        val files = dir.listFiles()
        for (file in files) {
            file.delete()
        }
        dir.delete()
    }

    private fun getIdFromFilename(filename: String) : String {
        val fn = Paths.get(filename).fileName.toString()
        val i = fn.indexOfFirst { it.isDigit() }
        return fn.substring(0, i) + "/" + fn.substring(i).dropLast(4)
    }

    private fun getRefList(
        filepath : String,
        referenceExtractor: ReferenceExtractor,
        validators: List<Validator>
    ) : MutableList<Reference>{
        try {
            val refs = referenceExtractor.extractUnverifiedReferences(
                File(filepath).readBytes()
            ).toMutableList()
            logger.info("Begin validation")
            runBlocking {
                validators.forEach { validator ->
                    validator.validate(refs)
                }
            }
            logger.info("Validated ${refs.count { it.validated }} out of ${refs.size}")
            return refs
        } catch (e : Exception) {
            return mutableListOf()
        }
    }

    private fun compareMD5(path : String, md5sumToCompare : String) : Boolean {
        val md = DigestUtils.md5Hex(File(path).readBytes())
        return md == md5sumToCompare
    }
}