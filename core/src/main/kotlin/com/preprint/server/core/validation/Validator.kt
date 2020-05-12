package com.preprint.server.core.validation

import com.preprint.server.core.data.Reference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.lang.Thread.sleep

interface Validator {
    suspend fun validate(refList : List<Reference>) = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Job>()
        var failedRefs = mutableListOf<Reference>()
        for (ref in refList) {
            jobs.add(launch {
                try {
                    validate(ref)
                } catch (e : Exception) {
                    failedRefs.add(ref)
                }
            })
        }
        jobs.forEach { it.join() }

        //try failed requests again
        if (failedRefs.size != 0) {
            sleep(2000)
            jobs.clear()
            var failed = 0
            for (ref in failedRefs) {
                jobs.add(launch {
                    try {
                        validate(ref)
                    } catch (e: Exception) {
                        failed++
                    }
                })
            }
            if (failed != 0) {
                throw ValidatorException("Validation failed twice for some references")
            }
        }
    }

    fun validate(ref : Reference)

    class ValidatorException(override val message: String = "") : Exception(message)
}