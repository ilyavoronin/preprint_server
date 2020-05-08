package com.preprint.server.utils

object Common {
    fun splitPages(pages: String): Pair<Int?, Int?> {
        var i = pages.indexOfFirst { it.isDigit() }
        var firstPageString = ""
        var lastPageString = ""
        while (i < pages.length && pages[i].isDigit()) {
            firstPageString += pages[i]
            i += 1
        }

        while (i < pages.length && !pages[i].isDigit()) {
            i += 1
        }

        while (i < pages.length && pages[i].isDigit()) {
            lastPageString += pages[i]
            i += 1
        }
        return Pair(firstPageString.toIntOrNull(), lastPageString.toIntOrNull())
    }

    fun parseYear(dataStr: String?): Int? {
        if (!dataStr.isNullOrBlank()) {
            val yearRegex = """(19|20)\d\d""".toRegex()
            val match = yearRegex.find(dataStr)
            if (match != null) {
                return match.value.toIntOrNull()
            }
        }
        return null
    }
}