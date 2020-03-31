package preprint.server.ref

import org.grobid.core.data.BibDataSet
import org.grobid.core.data.BiblioItem
import org.grobid.core.engines.Engine
import org.grobid.core.factory.GrobidFactory
import org.grobid.core.main.GrobidHomeFinder
import org.grobid.core.utilities.GrobidProperties
import java.io.File
import java.util.*

object GrobidEngine {
    var engine : Engine
    init {
        //the path to the grobid home folder
        val homePath = "/home/ilya/staff/grobid/grobid-home"
        val grobidHomeFinder = GrobidHomeFinder(Arrays.asList(homePath))
        GrobidProperties.getInstance(grobidHomeFinder)

        engine = GrobidFactory.getInstance().createEngine()
    }

    fun processReferences(pdfFile : File, consolidate : Int) : List<BibDataSet> {
        return engine.processReferences(pdfFile, consolidate)
    }

    fun processRawReference(ref : String, consolidate: Int) : BiblioItem {
        return engine.processRawReference(ref, consolidate)
    }

    fun processRawReferences(refList : List<String>, consolidate: Int) : List<BiblioItem> {
        return engine.processRawReferences(refList, consolidate)
    }
}