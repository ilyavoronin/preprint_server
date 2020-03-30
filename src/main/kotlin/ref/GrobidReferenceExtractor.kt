package preprint.server.ref

import org.grobid.core.engines.Engine
import org.grobid.core.factory.GrobidFactory
import org.grobid.core.main.GrobidHomeFinder
import org.grobid.core.utilities.GrobidProperties
import java.util.*

object GrobidReferenceExtractor : ReferenceExtractor {
    lateinit var engine : Engine
    init {
        //the path to the grobid home folder
        val homePath = "/home/ilya/staff/grobid/grobid-home"
        val grobidHomeFinder = GrobidHomeFinder(Arrays.asList(homePath))
        GrobidProperties.getInstance(grobidHomeFinder)

        engine = GrobidFactory.getInstance().createEngine()
    }

    override fun extract(pdf: ByteArray): List<String> {
        val tmpFile = createTempFile()
        tmpFile.writeBytes(pdf)
        val res =  engine.processReferences(tmpFile, 0).map { ref ->
            ref.rawBib.replace("\n", " ") + "\n" + ref.resBib.toBibTeX() + "\n"
        }
        tmpFile.deleteOnExit()
        return res
    }
}