package com.preprint.server.ref

interface ReferenceExtractor {
    fun extract(pdf : ByteArray) : List<Reference>
}