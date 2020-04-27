package com.preprint.server.arxivs3

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import javax.xml.parsers.SAXParserFactory

object ManifestParser {
    fun parseFilenames(path : String) : List<Pair<String, String> > {
        val file = File(path)
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()

        val handler = object: DefaultHandler() {
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