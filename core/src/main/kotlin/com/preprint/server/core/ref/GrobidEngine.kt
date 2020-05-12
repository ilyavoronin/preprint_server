package com.preprint.server.core.ref

import com.preprint.server.Config
import org.apache.logging.log4j.kotlin.logger
import org.grobid.core.data.BibDataSet
import org.grobid.core.data.BiblioItem
import org.grobid.core.engines.Engine
import org.grobid.core.factory.GrobidFactory
import org.grobid.core.main.GrobidHomeFinder
import org.grobid.core.utilities.GrobidProperties
import java.io.File
import java.util.*

object GrobidEngine {
    val logger = logger()
    var engine : Engine
    init {
        //the path to the grobid home folder
        val homePath = Config.config["grobid_home"].toString()
        val grobidHomeFinder = GrobidHomeFinder(Arrays.asList(homePath))
        GrobidProperties.getInstance(grobidHomeFinder)

        engine = GrobidFactory.getInstance().createEngine()
    }

    fun processReferences(pdfFile : File, consolidate : Int) : List<BibDataSet> {
        logger.info("Begin process references")
        return engine.processReferences(pdfFile, consolidate)
    }

    fun processRawReference(ref : String, consolidate: Int) : BiblioItem {
        return engine.processRawReference(ref, consolidate) ?: BiblioItem()
    }

    fun processRawReferences(refList : List<String>, consolidate: Int) : List<BiblioItem> {
        logger.info("Begin process raw references")
        if (consolidate == 1) {
            return engine.processRawReferences(refList, consolidate)
        }
        else {
            return refList.map { processRawReference(it, 0)}
        }
    }
}