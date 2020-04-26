package com.preprint.server.arxivs3

import com.preprint.server.Config
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths


object ArxivS3Collector {
    val logger = logger()
    val path = Config.config["arxiv_pdf_path"].toString()
    val manifestFileName = "manifest.xml"
    fun beginBulkDownload() {
        File("$path/pdf/").mkdir()
        val manifestPath = "$path/$manifestFileName"
        if (!File(manifestPath).exists()) {
            ArxivS3Downloader.downloadManifest(manifestPath)
        }
        else {
            logger.info("manifest is already downloaded")
        }

        val fileNames = ManifestParser.parseFilenames(manifestPath)
        fileNames.forEach {filename ->
            val pdfPath = "$path/$filename"
            if (!File(pdfPath).exists()) {
                ArxivS3Downloader.download(filename, pdfPath)
            }
            else {
                logger.info("$filename is already downloaded")
            }
            processFile(pdfPath)
        }
    }

    private fun processFile(filename : String) {
        val outputDir = File("$path/tmp")
        outputDir.mkdir()
        try {
            val filenames = unzip(filename, outputDir)
        } finally {
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
}