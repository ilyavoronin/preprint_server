package com.preprint.server.validation

import com.preprint.server.data.Reference

object ArxivValidator : Validator {
    val ids = this.javaClass.getResource("/ids.txt").readText().lines()
    override fun validate(ref: Reference) {
        ref.arxivId = ""
        val beg = ref.rawReference.lastIndexOf("arxiv:", ignoreCase = true)
        if (beg != -1 && beg + 6 < ref.rawReference.length && ref.rawReference[beg + 6].isDigit()) {
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
                    if (res.isNotBlank()) {
                        ref.arxivId = res
                    }
                    return
                }
                else {
                    val beg = """$idPrefix\.\p{Upper}{2}/""".toRegex().find(ref.rawReference)
                    if (beg != null) {
                        var i = beg.range.last + 1
                        var res = idPrefix + "/"
                        while (i < ref.rawReference.length && ref.rawReference[i].isDigit()) {
                            res += ref.rawReference[i]
                            i += 1
                        }
                        if (res.isNotBlank()) {
                            ref.arxivId = res
                        }
                        return
                    }
                }
            }
        }
    }

    fun containsId(ref : String) : Boolean {
        return ids.any {ref.contains(it)}
    }
}