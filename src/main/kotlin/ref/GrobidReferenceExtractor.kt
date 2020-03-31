package preprint.server.ref

import org.grobid.core.engines.Engine
import org.grobid.core.factory.GrobidFactory
import org.grobid.core.main.GrobidHomeFinder
import org.grobid.core.utilities.GrobidProperties
import java.util.*

object GrobidReferenceExtractor : ReferenceExtractor {
    override fun extract(pdf: ByteArray): List<Reference> {
        val tmpFile = createTempFile()
        tmpFile.writeBytes(pdf)
        val res =  GrobidEngine.processReferences(tmpFile, 1)
        tmpFile.deleteOnExit()
        return res.map {Reference(it)}
    }
}