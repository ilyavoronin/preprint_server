package preprint.server.ref

import org.grobid.core.engines.Engine

data class Reference(val rawReference : String, val engine : Engine? = null) {
    var arxivId : String? = null
    var doi : String? = null

    init {
        if (engine != null) {
            val p = engine.processRawReference(rawReference, 0)
            arxivId = p.arXivId
            doi = p.doi
        }
    }
}