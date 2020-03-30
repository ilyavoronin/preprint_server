package examples

import arxiv.ArxivCollector
import java.io.File

const val START_DATE = "2020-03-20"
const val fileName = "files/arxivRecords.txt"

fun main() {
    val arxivRecords = ArxivCollector.collect(START_DATE)

    val outFile = File(fileName)
    outFile.writeText("")
    arxivRecords.forEach { outFile.appendText(it.toString()) }
}