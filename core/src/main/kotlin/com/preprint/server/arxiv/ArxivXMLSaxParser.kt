package com.preprint.server.arxiv

import com.preprint.server.data.Author
import com.preprint.server.data.JournalRef
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import javax.xml.parsers.SAXParserFactory

object ArxivXMLSaxParser {
    val factory = SAXParserFactory.newInstance()
    val parser = factory.newSAXParser()
    fun parse(xmlText : String) : List<ArxivData> {
        val handler = ArxivHandler()
        parser.parse(ByteArrayInputStream(xmlText.toByteArray()), handler)
        return handler.records
    }

    private class ArxivHandler : DefaultHandler() {
        val records = mutableListOf<ArxivData>()
        private val tagStatus = mutableMapOf<String, Boolean>().withDefault { false }
        private var curRecord = ArxivData()
        private val titleLines = mutableListOf<String>()
        private val abstractLines = mutableListOf<String>()
        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            if (qName == null) {
                return
            }
            when (qName) {
                "entry"     -> curRecord = ArxivData()
                "category"  -> {
                    if (attributes != null) {
                        val cat = attributes.getValue("term")
                        if (!cat.isNullOrBlank() && !cat.contentEquals(" ") && cat.all {!it.isDigit()}) {
                            curRecord.categories.add(cat)
                        }
                    }
                }
                "link"     -> {
                    if (attributes != null && attributes.getValue("title") == "pdf") {
                        val link = attributes.getValue("href")
                        if (!link.isNullOrBlank()) {
                            curRecord.pdfUrl = link
                        }
                    }
                }
                "summary"  -> {
                    abstractLines.clear()
                    tagStatus[qName] = true
                }
                "title"    -> {
                    titleLines.clear()
                    tagStatus[qName] = true
                }
                else       -> tagStatus[qName] = true
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            if (qName == null) {
                return
            }
            tagStatus[qName] = false
            when (qName) {
                "entry" -> records.add(curRecord)
                "title" -> curRecord.title = makeOneLine(titleLines)
                "summary" -> curRecord.abstract = makeOneLine(abstractLines)
            }
        }

        @ExperimentalStdlibApi
        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (ch == null) {
                return
            }
            val value = getValue(ch, start, length)
            if (tagStatus.getValue("published")) {
                curRecord.creationDate = value.substring(0, 10)
            }
            if (tagStatus.getValue("updated")) {
                curRecord.lastUpdateDate = value.substring(0, 10)
            }
            if (tagStatus.getValue("title")) {
                titleLines.add(value)
            }
            if (tagStatus.getValue("summary")) {
                abstractLines.add(value)
            }
            if (tagStatus.getValue("author") && tagStatus.getValue("name")) {
                val author = Author(value)
                curRecord.authors.add(author)
            }
            if (tagStatus.getValue("author") && tagStatus.getValue("arxiv:affiliation")) {
                val author = curRecord.authors.removeLastOrNull() ?: return
                curRecord.authors.add(Author(author.name, value))
            }
            if (tagStatus.getValue("arxiv:doi")) {
                curRecord.doi = value
            }
            if (tagStatus.getValue("arxiv:journal_ref")) {
                curRecord.journal = JournalRef(value, true)
            }
        }

        private fun getValue(ch: CharArray, start : Int, length: Int) : String {
            return String(ch, start, length)
        }

        //convert multiline string to oneline string
        private fun makeOneLine(rawLines : List<String>) : String {
            val lines = rawLines.map {it.trim()}.filter { it.isNotEmpty() }
            var res = ""
            for ((i, line) in lines.withIndex()) {
                if (i == 0) {
                    res = line
                    continue
                }
                if (res.length > 1 && line.length > 0 && res.last() == '-') {
                    val lastC = res[res.lastIndex - 1]
                    if (lastC.isLowerCase() && line.first().isLowerCase()) {
                        res = res.dropLast(1) + line
                    }
                    else {
                        res += line
                    }
                }
                else {
                    res += " " + line
                }
            }
            return res
        }
    }
}