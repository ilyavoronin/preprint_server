package preprint.server.examples

import preprint.server.ref.*
import java.lang.Exception
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

const val FILES_FOLDER = "./files/test/"
const val BENCHMARKS_FOLDER = "./benchmarks"

fun main() {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")

    val extractors = mapOf(
        CermineReferenceExtractor to "Cermine",
        CustomReferenceExtractor to "Custom",
        GrobidReferenceExtractor to "Grobid"
    )

    val pdfPath = Paths.get(FILES_FOLDER)
    val benchmarksPath = Paths.get(BENCHMARKS_FOLDER)
    val csvStats = benchmarksPath.resolve("benchmark.csv")
    csvStats.toFile().outputStream().bufferedWriter().use { csvWriter ->
        pdfPath.toAbsolutePath().normalize().toFile().listFiles { file ->
            file.name.endsWith(".pdf")
        }?.let { files ->
            files.forEach { file ->
                extractors.keys.forEach { extractor ->
                    var referencesNumber = 0
                    val timeMillis = measureTimeMillis {
                        try {
                            referencesNumber = extractor.extract(file.readBytes()).size
                        } catch (e: Exception) {
                            println("${file.nameWithoutExtension},${extractors[extractor]} - e.message")
                        }
                    }
                    val data = "${file.nameWithoutExtension},${extractors[extractor]},${referencesNumber},${timeMillis}"
                    csvWriter.write("$data\n")
                    println(data)
                }
            }
        }
    }
}