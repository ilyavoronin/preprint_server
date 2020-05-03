package com.preprint.server.ref

import com.preprint.server.data.Reference
import com.preprint.server.validation.Validator
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.lang.Thread.sleep

interface ReferenceExtractor {
    fun getReferences(pdf : ByteArray, validators : List<Validator> = listOf()) : List<Reference> {
        val refs = extractUnverifiedReferences(pdf)
        logger().info("Begin validation of ${refs.size} references")
        var attemptsDone = 0
        while (true) {
            var validationSussess = true
            runBlocking {
                try {
                    validators.forEach { validator ->
                        validator.validate(refs)
                    }
                } catch (e: Validator.ValidatorException) {
                    validationSussess = false
                    attemptsDone += 1
                    logger().error(e.message)
                    logger().info("Attempts done: $attemptsDone. Waiting for 2 seconds")
                    sleep(2000)
                    logger().info("Trying to validate references one more time")
                }
            }
            if (validationSussess) {
                break
            }
        }
        if (validators.isNotEmpty()) {
            logger().info("Validated ${refs.count { it.validated }} out of ${refs.size}")
        }
        return refs
    }
    fun extractUnverifiedReferences(pdf : ByteArray) : List<Reference>
}