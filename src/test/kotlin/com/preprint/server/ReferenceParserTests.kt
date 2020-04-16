package com.preprint.server

import com.preprint.server.ref.CustomReferenceExtractor
import com.preprint.server.ref.custom.ReferenceParser
import com.preprint.server.ref.custom.ReferenceType
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.Test

class ReferenceParserTests {
    fun loadFromResourses(fileName : String) : String {
        return this.javaClass.getResource("/referenceParserTests/$fileName").readText()
    }

    @Test
    fun referenceTypeAOneColumnWithIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceA1"))
        println(lines)
        val expected = loadFromResourses("referenceA1.exp").split("\n")

        val refs = ReferenceParser.parse(lines, ReferenceType.A, false, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeAOneColumnWithoutIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceA2"))
        println(lines)
        val expected = loadFromResourses("referenceA2.exp").split("\n")

        val refs = ReferenceParser.parse(lines, ReferenceType.A, false, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeATwoColumnWithIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceA3"))
        println(lines)
        val expected = loadFromResourses("referenceA3.exp").split("\n")

        val refs = ReferenceParser.parse(lines, ReferenceType.A, true, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeATwoColumnWithoutIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceA4"))
        println(lines)
        val expected = loadFromResourses("referenceA4.exp").split("\n")

        val refs = ReferenceParser.parse(lines, ReferenceType.A, true, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeADrop1Test() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceA5"))
        println(lines)
        val expected = listOf<String>()

        val refs = ReferenceParser.parse(lines, ReferenceType.A, false, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeADrop2Test() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceA6"))
        println(lines)
        val expected = listOf<String>()

        val refs = ReferenceParser.parse(lines, ReferenceType.A, false, 900)

        assertEquals(expected, refs)
    }
}