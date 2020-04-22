package examples

import com.preprint.server.crossref.Validator
import com.preprint.server.data.Reference
import kotlinx.coroutines.*
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    println(measureTimeMillis {
        val refs = mutableListOf(
            Reference("Napiwotzki, R., Koester, D., & Nelemans, G., et al., 2002, A&A, 386, 957"),
            Reference("White Dwarfs"),
            Reference("Black Holes"),
            Reference("BWT")
        )
        for (i in 0..70) {
            refs.add(refs[0])
        }
        val job = launch {
            Validator.validate(refs)
        }
        job.join()
        println(refs)
    })
}