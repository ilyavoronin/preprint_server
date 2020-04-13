package com.preprint.server

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.preprint.server.arxiv.ArxivAPI
import com.preprint.server.arxiv.ArxivData
import com.preprint.server.arxiv.ArxivXMLParser
import io.mockk.*
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.junit.jupiter.api.AfterEach

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class ArxivAPITests {

    @BeforeEach
    fun initTest() {
        mockkConstructor(KotlinLogger::class)
        every {anyConstructed<KotlinLogger>().info(any<String>())} just Runs
    }

    @AfterEach
    fun clear() {
        unmockkAll()
    }

    @Test
    fun testGetBulkApiRecordsWithResumptionTokenSuccess() {
        val spyArxiApi = spyk(ArxivAPI)
        val requestMock = mockk<Request>()
        val resultMock = mockk<Result.Success<String>>()
        val responseMock = mockk<Response>()

        val url = ArxivAPI.requestBulkUrlPrefix + "verb=ListRecords&resumptionToken=" + "token1111"
        mockkStatic("com.github.kittinunf.fuel.FuelKt")
        every { url.httpGet().timeoutRead(any()).responseString() }returns
                Triple(requestMock, responseMock, resultMock)

        every { resultMock.get()} returns "xml text"

        mockkObject(ArxivXMLParser)
        every {ArxivXMLParser.parseArxivRecords("xml text")} returns
                Triple(listOf(ArxivData("ident", id = "id1")), "new token", 1000)
        val slot = slot<List<String>>()
        every { spyArxiApi.getRecordsLinks(capture(slot))} answers {listOf("pdf url ${slot.captured[0]}")}


        val (arxivRecords, newResToken, recTotal) =
            spyArxiApi.getBulkArxivRecords("", "token1111", 100)

        assertTrue(arxivRecords?.size == 1)
        assertEquals("id1", arxivRecords?.get(0)?.id)
        assertEquals("pdf url id1", arxivRecords?.get(0)?.pdfUrl)
        assertEquals("new token", newResToken)
        assertEquals(1000, recTotal)
    }

    @Test
    fun testGetBulkApiRecordsWithDateSuccess() {
        val spyArxiApi = spyk(ArxivAPI)
        val requestMock = mockk<Request>()
        val resultMock = mockk<Result.Success<String>>()
        val responseMock = mockk<Response>()

        val url = ArxivAPI.requestBulkUrlPrefix + "verb=ListRecords&from=2020-01-10&metadataPrefix=arXiv"
        mockkStatic("com.github.kittinunf.fuel.FuelKt")
        every { url.httpGet().timeoutRead(any()).responseString() }returns
                Triple(requestMock, responseMock, resultMock)

        every { resultMock.get()} returns "xml text"

        mockkObject(ArxivXMLParser)
        every {ArxivXMLParser.parseArxivRecords("xml text")} returns
                Triple(listOf(ArxivData("ident", id = "id1")), "new token", 1000)
        val slot = slot<List<String>>()
        every { spyArxiApi.getRecordsLinks(capture(slot))} answers {listOf("pdf url ${slot.captured[0]}")}


        val (arxivRecords, newResToken, recTotal) =
            spyArxiApi.getBulkArxivRecords("2020-01-10", "", 100)

        assertTrue(arxivRecords?.size == 1)
        assertEquals("id1", arxivRecords?.get(0)?.id)
        assertEquals("pdf url id1", arxivRecords?.get(0)?.pdfUrl)
        assertEquals("new token", newResToken)
        assertEquals(1000, recTotal)
    }
}