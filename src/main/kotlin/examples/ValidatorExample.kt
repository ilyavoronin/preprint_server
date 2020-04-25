package examples

import com.preprint.server.validation.CrossRefValidator
import com.preprint.server.data.Reference
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    println(measureTimeMillis {
        val refs = mutableListOf(
            Reference("L. Infeld and J. Plebański, Bull. Acad. Polon. III, 4, 749 (1956).", true)
        )
        refs.forEach {println(it)}
        val job = launch {
            CrossRefValidator.validate(refs)
        }
        job.join()
        println(refs)
    })
}