package com.preprint.server.arxivs3

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import javax.xml.parsers.SAXParserFactory


/**
 * Parses arxiv's manifest.xml file, that contains information
 * about files stored in arxiv's Amazon S3 bucket
 */
object ManifestParser {

    /**
     * Returns a list of filename and md5 hash of each file stored in Amazon S3 bucket
     */
    fun parseFilenames(path: String) : List<Pair<String, String> > {
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()

        val handler = object : DefaultHandler() {
            val filenames = mutableListOf<String>()
            val checksums = mutableListOf<String>()
            var bFilename = false
            var bCheckSum = false
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                if (qName.equals("filename")) {
                    bFilename = true
                }
                if (qName.equals("md5sum")) {
                    bCheckSum = true
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                if (qName.equals("filename")) {
                    bFilename = false
                }
                if (qName.equals("md5sum")) {
                    bCheckSum = false
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (bFilename) {
                    filenames.add(String(ch, start, length).trim())
                }
                if (bCheckSum) {
                    checksums.add(String(ch, start, length).trim())
                }
            }
        }

        parser.parse(File(path), handler)

        return handler.filenames.zip(handler.checksums)
    }
}