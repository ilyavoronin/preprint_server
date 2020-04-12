package com.preprint.server

import com.preprint.server.arxiv.ArxivData
import com.preprint.server.arxiv.ArxivXMLParser
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*
import java.io.File
import kotlin.math.exp

class ArxivXmlParserTests {
    fun loadFromResourses(fileName : String) : String {
        return this.javaClass.getResource(fileName).readText()
    }

    @Test
    fun fullDataTest() {
        val xmlText = loadFromResourses("/xmlParserTests/xmlArxivData.xml")
        val expectedResumptionToken = "4342418|1001"
        val expectedTotalRecords = 7002
        val expectedRecord1 = ArxivData(
            identifier = "oai:arXiv.org:0801.2407",
            datestamp = "2020-03-25",
            id = "0801.2407",
            creationDate = "2008-01-15",
            authors = mutableListOf(
                ArxivData.Author("Laetitia Abel-Tiberini II", "LAOG"),
                ArxivData.Author("Pierre Labeye", "CEA Leti")
            ),
            title = "ABCD ABCD DEFG",
            categories = mutableListOf("astro-ph"),
            comments = "This is comment",
            doi = "10.1364/OE.15.018005",
            abstract = "This is abstract",
            mscClass = "55p35"
        )

        val expectedRecord2 = ArxivData(
            identifier = "oai:arXiv.org:0909.2882",
            datestamp = "2020-03-24",
            id = "0909.2882",
            creationDate = "2009-09-15",
            lastUpdateDate = "2020-03-21",
            authors = mutableListOf(
                ArxivData.Author("Wm. G. Hoover"),
                ArxivData.Author("Carol G. Hoover")
            ),
            title = "Shockwaves and Local Hydrodynamics; Failure of the Navier-Stokes Equations",
            categories = mutableListOf("physics.flu-dyn", "nlin.CD"),
            comments = "many lines comment",
            doi = "10.1142/9789814307543_0002",
            license = "http://arxiv.org/licenses/nonexclusive-distrib/1.0/",
            abstract = "Big abstract abacaba DABA-DABA",
            acmClass = "56asdf",
            journalRef = "Phys.Rev.D76:013009,2007",
            reportNo = "IGPG-07/03-2"
        )

        val (arxivRecords, resumptionToken, recordsTotal) = ArxivXMLParser.parseArxivRecords(xmlText)

        assertTrue(resumptionToken == expectedResumptionToken)
        assertTrue(recordsTotal == expectedTotalRecords)
        assertTrue(arxivRecords.size == 2)
        assertEquals(expectedRecord1, arxivRecords[0])
        assertEquals(expectedRecord2, arxivRecords[1])
    }

    @Test
    fun testMakeOneLineOneWord() {
        val tstring = "  abacaba-  \ndaba-\n  caba  "
        val expected = "abacabadabacaba"

        val actual = ArxivXMLParser.makeOneLine(tstring)

        assertEquals(expected, actual)
    }
}