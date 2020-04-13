package com.preprint.server.arxiv

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory


//fields that must always be presented: identifier, datestamp, id, title, abstract, creation date
//otherwise the record won't be saved
object ArxivXMLParser {
    fun parseArxivRecords(xmlText : String) : Triple<List<ArxivData>, String, Int> {
        val inputStream = InputSource(ByteArrayInputStream(xmlText.toByteArray()))
        val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        xmlDoc.documentElement.normalize()

        val arxivRecords = mutableListOf<ArxivData>()
        val recordList = xmlDoc.getElementsByTagName("record")
        for (i in 0 until recordList.length) {
            val recordElem = recordList.item(i) as Element
            val recordHeader = recordElem.getElementsByTagName("header").item(0) as Element
            val recordMetadata = recordElem.getElementsByTagName("metadata").item(0) as Element
            recordHeader.normalize()
            recordMetadata.normalize()

            //create ArxivData from identifier string
            val arxivData = ArxivData(
                recordHeader.getValue("identifier") ?: continue
            )

            //parse all required fields
            arxivData.datestamp = recordHeader.getValue("datestamp") ?: continue
            arxivData.id = recordMetadata.getValue("id") ?: continue
            arxivData.creationDate = recordMetadata.getValue("created") ?: continue
            arxivData.title = recordMetadata.getValue("title") ?: continue
            arxivData.abstract = recordMetadata.getValue("abstract") ?: continue

            //parse optional fields
            arxivData.lastUpdateDate = recordMetadata.getValue("updated")
            //get authors' names with affiliations(if present)
            val authorsNodeList = recordMetadata.getElementsByTagName("authors").item(0) as Element
            val authorsList = authorsNodeList.getElementsByTagName("author")
            for (j in 0 until authorsList.length) {
                val authorInfo = authorsList.item(j) as Element
                var name = authorInfo.getValue("keyname") ?: ""
                val forenames = authorInfo.getValue("forenames")
                val suffix = authorInfo.getValue("suffix")
                if (forenames != null) {
                    name = forenames + " " + name
                }
                if (suffix != null) {
                    name = name + " " + suffix
                }

                val affiliation : String? = authorInfo.getValue("affiliation")
                arxivData.authors.add(ArxivData.Author(name, affiliation))
            }

            arxivData.categories = recordMetadata.getValue("categories")
                ?.split(" ")?.toMutableList() ?: mutableListOf()

            arxivData.comments = recordMetadata.getValue("comments")
            arxivData.reportNo = recordMetadata.getValue("report-no")
            arxivData.journalRef = recordMetadata.getValue("journal-ref")
            arxivData.mscClass = recordMetadata.getValue("msc-class")
            arxivData.acmClass = recordMetadata.getValue("acm-class")
            arxivData.doi = recordMetadata.getValue("doi")
            arxivData.license = recordMetadata.getValue("license")
            arxivRecords.add(arxivData)
        }
        val resumptionTokenElem = xmlDoc.getElementsByTagName("resumptionToken").item(0) as Element //TODO throw an exception
        val recordsTotal = resumptionTokenElem.getAttribute("completeListSize").toInt()

        return Triple(arxivRecords, resumptionTokenElem.textContent, recordsTotal)
    }

    fun getPdfLinks(xmlText: String) : List<String> {
        val pdfList = mutableListOf<String>()

        val inputStream = InputSource(ByteArrayInputStream(xmlText.toByteArray()))
        val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)

        val entryList = xmlDoc.getElementsByTagName("entry")
        for (i in 0 until entryList.length) {
            val elem = entryList.item(i) as Element
            val links = elem.getElementsByTagName("link")
            var added = false
            for (j in 0 until links.length) {
                val linkElem = links.item(j) as Element
                if (linkElem.hasAttribute("title") && linkElem.getAttribute("title") == "pdf") {
                    pdfList.add(linkElem.getAttribute("href"))
                    added = true
                }
            }
            if (!added) {
                pdfList.add("")
            }
        }
        return pdfList
    }

    fun Element.getValue(tagName : String) : String? {
        val elems =  this.getElementsByTagName(tagName)
        if (elems.length == 0) {
            return null
        }
        else {
            return makeOneLine(elems.item(0).textContent)
        }
    }

    //convert multiline string to oneline string
    fun makeOneLine(str: String) : String {
        val lines = str.split("\n").map {it.trim()}.filter { it.isNotEmpty() }
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