package com.preprint.server.validation

import com.preprint.server.data.Reference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger
import java.lang.Exception

interface Validator {
    suspend fun validate(refList : List<Reference>) = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Job>()
        for (ref in refList) {
            jobs.add(launch {
                try {
                    validate(ref)
                } catch (e : Exception) {
                    throw ValidatorException(e.message.toString())
                }
            })
        }
        jobs.forEach {it.join()}
    }

    fun validate(ref : Reference)

    class ValidatorException(override val message: String = "") : Exception(message)
}