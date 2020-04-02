package preprint.server.arxiv

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory


object ArxivXMLParser {
    fun parseArxivRecords(xmlText : String) : Pair<List<ArxivData>, String> {
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
                recordHeader.getValue("identifier") ?: ""
            ) //TODO throw exception

            arxivData.datestamp = recordHeader.getValue("datestamp") ?: ""

            //get all specs from header
            val specs = recordHeader.getElementsByTagName("setSpecs")
            for (j in 0 until specs.length) {
                arxivData.specs.add(specs.item(i).textContent)
            }

            arxivData.id = recordMetadata.getValue("id") ?: "" //TODO throw exception

            arxivData.creationDate = recordMetadata.getValue("created") ?: ""

            arxivData.lastUpdateDate = recordMetadata.getValue("updated")

            arxivData.title = recordMetadata.getValue("title") ?: ""

            //get authors' names with affiliations(if present)
            val authorsNodeList = recordMetadata.getElementsByTagName("authors").item(0) as Element
            val authorsList = authorsNodeList.getElementsByTagName("author")
            for (j in 0 until authorsList.length) {
                val authorInfo = authorsList.item(j) as Element
                val name = authorInfo.getValue("keyname") + " " + authorInfo.getValue("forenames")
                val affiliation : String? = authorInfo.getValue("affiliations")
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
            arxivData.license = recordMetadata.getValue("license") ?: ""
            arxivData.abstract = recordMetadata.getValue("abstract") ?: ""
            arxivRecords.add(arxivData)
        }
        val resumptionTokenElem = xmlDoc.getElementsByTagName("resumptionToken").item(0) as Element //TODO throw an exception

        return Pair(arxivRecords, resumptionTokenElem.textContent)
    }

    fun getPdfLinks(xmlText: String) : List<String>? {
        val pdfList = mutableListOf<String>()

        val inputStream = InputSource(ByteArrayInputStream(xmlText.toByteArray()))
        val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)

        val entryList = xmlDoc.getElementsByTagName("entry")
        for (i in 0 until entryList.length) {
            val elem = entryList.item(i) as Element
            val links = elem.getElementsByTagName("link")
            for (j in 0 until links.length) {
                val linkElem = links.item(j) as Element
                if (linkElem.hasAttribute("title") && linkElem.getAttribute("title") == "pdf") {
                    pdfList.add(linkElem.getAttribute("href"))
                }
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
            return elems.item(0).textContent
        }
    }
}