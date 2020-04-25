package com.preprint.server.validation

import com.preprint.server.data.Reference

object ArxivValidator : Validator {
    val ids = this.javaClass.getResource("/ids.txt").readText().lines()
    override fun validate(ref: Reference) {
        val beg = ref.rawReference.lastIndexOf("arxiv:", ignoreCase = true)
        if (beg != -1) {
            val arx = ref.rawReference.substring(beg).substringAfter(":")
            if (arx.length > 8) {
                var res : String? = null
                if (arx.substring(0, 4).all {it.isDigit()} && arx[4] == '.') {
                    res = arx.substring(0, 5)
                    for (c in arx.substring(5)) {
                        if (!c.isDigit()) break
                        res += c
                    }
                }
                else {
                    val i = arx.indexOf('/')
                    if (i != -1) {
                        res = arx.substring(0, i + 1)
                        for (c in arx.substring(i + 1)) {
                            if (!c.isDigit()) {
                                break
                            }
                            res += c
                        }
                    }
                }

                if (res != null) {
                    ref.arxivId = res
                }
            }
        }
        else {
            ids.forEach {idPrefix ->
                val beg = ref.rawReference.indexOf(idPrefix + "/")
                if (beg != -1) {
                    var res = idPrefix + "/"
                    var i = ref.rawReference.indexOf('/', beg) + 1
                    while (i < ref.rawReference.length && ref.rawReference[i].isDigit()) {
                        res += ref.rawReference[i]
                        i += 1
                    }
                    ref.arxivId = res
                    return
                }
            }
        }
    }
}