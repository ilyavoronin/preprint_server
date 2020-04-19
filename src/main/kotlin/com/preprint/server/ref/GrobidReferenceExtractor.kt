package com.preprint.server.ref

import com.preprint.server.data.Reference


object GrobidReferenceExtractor : ReferenceExtractor {
    override fun extract(pdf: ByteArray): List<Reference> {
        val tmpFile = createTempFile()
        tmpFile.writeBytes(pdf)
        val res = GrobidEngine.processReferences(tmpFile, 1)
        tmpFile.deleteOnExit()
        return res.map { Reference(it) }
    }
}