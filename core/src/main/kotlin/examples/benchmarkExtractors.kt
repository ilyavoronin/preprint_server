package preprint.server.examples

import com.preprint.server.core.data.Reference
import com.preprint.server.core.ref.*
import com.preprint.server.core.validation.ArxivValidator
import com.preprint.server.core.validation.LocalValidator
import java.io.File
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

    val validators = listOf(LocalValidator, ArxivValidator)

    val extractors = mapOf(
        CustomReferenceExtractor to "Custom",
        GrobidReferenceExtractor to "Grobid"
    )

    val references = mutableMapOf(
        CustomReferenceExtractor to mutableListOf<Pair<String, Reference>>(),
        GrobidReferenceExtractor to mutableListOf<Pair<String, Reference>>()
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
                            val newRefs = extractor.getReferences(file.readBytes(), validators)
                            references[extractor]!!.addAll(newRefs.map {Pair(file.nameWithoutExtension, it)})
                            referencesNumber = newRefs.size
                        } catch (e: Exception) {
                            println("$progressPrefix ${file.nameWithoutExtension},${extractors[extractor]} - e.message")
                        }
                    }
                    val validatedCnt = references[extractor]!!.count { it.second.validated }
                    val data = "${file.nameWithoutExtension},${extractors[extractor]},${referencesNumber},${validatedCnt},${timeMillis}"
                    csvWriter.write("$data\n")
                    println("$progressPrefix $data")
                }
            }
        }
    }

    /**
     * Crete files that stores differences in extracted references(only validated are considered)
     * For example reference that was extracted with Grobid and was not extracted by Custom
     * will be stored in file GrobidWithoutCustom.txt.
     * This will be done for each file separately and references will be compared by doi or arxivId
     */

    for ((extractor1, name1) in extractors) {
        for ((extractor2, name2) in extractors) {
            if (extractor1 == extractor2) {
                continue
            }
            val m1 = references[extractor1]!!.groupBy({it.first}, {it.second})
            val m2 = references[extractor2]!!.groupBy({it.first}, {it.second})
            val outputFile = File("${name1}Without${name2}")
            var diffCnt = 0
            outputFile.writeText("")
            for ((filename, refs) in m1) {
                val valRefs1 = refs.filter {it.validated}
                val valRefs2 = m2[filename]?.filter { it.validated }
                if (valRefs2 == null) {
                    valRefs1.forEach { outputFile.appendText("$filename\n $it\n\n") }
                    diffCnt += valRefs1.size
                } else {
                    for (ref in valRefs1) {
                        if (
                            valRefs2.count {
                                ref.arxivId != null && ref.arxivId == it.arxivId
                                    || ref.doi != null && ref.doi == it.doi } == 0
                        ) {
                            outputFile.appendText("$filename\n $ref\n\n")
                            diffCnt += 1
                        }
                    }
                }
            }
            println("$name1 extractor extracted $diffCnt references more than $name2")
        }
    }
}