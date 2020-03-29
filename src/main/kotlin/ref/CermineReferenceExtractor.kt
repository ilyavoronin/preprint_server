package preprint.server.ref

import pl.edu.icm.cermine.ContentExtractor
import java.io.ByteArrayInputStream
import java.io.InputStream

object CermineReferenceExtractor : ReferenceExtractor {
    override fun extract(pdf : ByteArray): List<String> {
        val extractor = ContentExtractor()
        val inputStream: InputStream = ByteArrayInputStream(pdf)
        extractor.setPDF(inputStream)
        val references = extractor.references
        return references.map {it.text}
    }
}