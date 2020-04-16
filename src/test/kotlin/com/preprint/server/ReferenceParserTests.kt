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

    @Test
    fun referenceTypeBOneColumnWithIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceB1"))
        println(lines)
        val expected = loadFromResourses("referenceB1.exp").split("\n")

        val refs = ReferenceParser.parse(lines, ReferenceType.B, false, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeBDropWithoutIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceB2"))
        println(lines)
        val expected = listOf<String>()

        val refs = ReferenceParser.parse(lines, ReferenceType.B, false, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeBTwoColumnWithIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceB3"))
        println(lines)
        val expected = loadFromResourses("referenceB3.exp").split("\n")

        val refs = ReferenceParser.parse(lines, ReferenceType.B, true, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeBDropTwoColumnWithoutIndentTest() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceB4"))
        println(lines)
        val expected = listOf<String>()

        val refs = ReferenceParser.parse(lines, ReferenceType.B, true, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeBDrop1Test() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceB5"))
        println(lines)
        val expected = listOf<String>()

        val refs = ReferenceParser.parse(lines, ReferenceType.B, false, 900)

        assertEquals(expected, refs)
    }

    @Test
    fun referenceTypeBDrop2Test() {
        val lines = CustomReferenceExtractor.getLines(loadFromResourses("referenceB6"))
        println(lines)
        val expected = listOf<String>()

        val refs = ReferenceParser.parse(lines, ReferenceType.B, false, 900)

        assertEquals(expected, refs)
    }
}