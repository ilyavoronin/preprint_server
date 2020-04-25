package com.preprint.server.ref

import com.preprint.server.data.Reference
import com.preprint.server.validation.Validator
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger

interface ReferenceExtractor {
    fun getReferences(pdf : ByteArray, validators : List<Validator> = listOf()) : List<Reference> {
        val refs = extractUnverifiedReferences(pdf)
        runBlocking {
            validators.forEach { validator ->
                validator.validate(refs)
            }
        }
        if (validators.isNotEmpty()) {
            logger().info("Validated ${refs.count { it.validated }} out of ${refs.size}")
        }
        return refs
    }
    fun extractUnverifiedReferences(pdf : ByteArray) : List<Reference>
}