package com.preprint.server.validation

import com.preprint.server.algo.LCS
import com.preprint.server.algo.LvnstDist
import com.preprint.server.crossref.CRData
import com.preprint.server.crossref.CrossRefApi
import com.preprint.server.data.Reference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger

interface Validator {
    suspend fun validate(refList : List<Reference>) = withContext(Dispatchers.IO) {
        logger().info("Begin validation of ${refList.size} references")
        val jobs = mutableListOf<Job>()
        for (ref in refList) {
            jobs.add(launch { validate(ref) })
        }
        jobs.forEach {it.join()}
    }

    fun validate(ref : Reference)
}