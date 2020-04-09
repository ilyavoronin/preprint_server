package preprint.server.examples

import preprint.server.ref.*
import java.lang.Exception
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

const val FILES_FOLDER = "./files/test/"
const val BENCHMARKS_FOLDER = "./benchmarks"

/**
 * Compare implemented reference extractors on a set of files from FILES_FOLDER.
 * Output CSV contains the following metrics:
 *  - number of parsed references
 *  - execution time in milliseconds
 * Format: filename,extractor,referencesNumber,executionTime
 *
 * This CSV is used by benchmark.ipynb to summarize and visualize results.
 */
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
            files.forEachIndexed { i, file ->
                extractors.keys.forEach { extractor ->
                    var referencesNumber = 0
                    val progressPrefix = "(${i + 1} / ${files.size})"
                    val timeMillis = measureTimeMillis {
                        try {
                            referencesNumber = extractor.extract(file.readBytes()).size
                        } catch (e: Exception) {
                            println("$progressPrefix ${file.nameWithoutExtension},${extractors[extractor]} - e.message")
                        }
                    }
                    val data = "${file.nameWithoutExtension},${extractors[extractor]},${referencesNumber},${timeMillis}"
                    csvWriter.write("$data\n")
                    println("$progressPrefix $data")
                }
            }
        }
    }
}