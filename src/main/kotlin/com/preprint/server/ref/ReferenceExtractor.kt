package com.preprint.server.ref

import com.preprint.server.data.Reference

interface ReferenceExtractor {
    fun extract(pdf : ByteArray) : List<Reference>
}