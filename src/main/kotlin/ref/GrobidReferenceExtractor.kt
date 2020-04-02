package preprint.server.ref


object GrobidReferenceExtractor : ReferenceExtractor {
    override fun extract(pdf: ByteArray): List<Reference> {
        val tmpFile = createTempFile()
        tmpFile.writeBytes(pdf)
        val res =  GrobidEngine.processReferences(tmpFile, 1)
        tmpFile.deleteOnExit()
        return res.map {Reference(it)}
    }
}