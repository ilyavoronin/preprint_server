package preprint.server.ref

object ReferenceParserBC : ReferenceParser {
    override fun parse(
        lines: List<CustomReferenceExtractor.Line>,
        isTwoColumns: Boolean,
        pageWidth: Int
    ): List<String> {
        //find if references type B or C
        val refType = when {
            ReferenceType.B.firstRegex.containsMatchIn(lines[0].str) -> ReferenceType.B
            else                                                     -> ReferenceType.C
        }
        val refRegex = refType.regex
        val refList = mutableListOf<String>()
        if (isTwoColumns == false) {
            //analyze this text
            var canUseSecondIndentPattern = true
            var i = 0
            var secondLineIndent = -1
            var curRefNum = 1
            val firstLineIndices = mutableListOf<Int>()
            var maxWidth = 0
            while (i < lines.size) {
                firstLineIndices.add(i)
                //find next reference
                maxWidth = Integer.max(maxWidth, lines[i].lastPos - lines[i].indent)
                var j = i + 1
                while (j < lines.size) {
                    val match = refRegex.find(lines[j].str)
                    val value = match?.value?.dropLast(1)?.toInt()
                    if (value != null && value == curRefNum + 1) {
                        break
                    } else {
                        if (value == curRefNum) {
                            return listOf()
                        }
                    }
                    j += 1
                }
                if (j != lines.size) {
                    for (k in i + 1 until j) {
                        if (secondLineIndent == -1) {
                            secondLineIndent = lines[k].indent
                        }
                        if (secondLineIndent != lines[k].indent) {
                            canUseSecondIndentPattern = false
                        }
                    }
                } else {
                    //this was the last reference
                    break
                }
                curRefNum += 1
                i = j
            }

            if (!canUseSecondIndentPattern) {
                return listOf()
            }

            fun addLineToReference(ref: String, line: String): String {
                return if (ref.length > 2 && ref.last() == '-') {
                    if (ref[ref.lastIndex - 1].isLowerCase() && line.first().isLowerCase()) {
                        ref.dropLast(1) + line
                    } else {
                        ref + line
                    }
                } else {
                    if (ref == "") {
                        line
                    } else {
                        ref + " " + line
                    }
                }
            }

            //parse references
            for ((j, lineInd) in firstLineIndices.withIndex()) {
                var curRef = ""
                if (j != firstLineIndices.lastIndex) {
                    val nextLineInd = firstLineIndices[j + 1]
                    for (k in lineInd until nextLineInd) {
                        curRef = addLineToReference(curRef, lines[k].str)

                        if ((curRef.last() == '.' || nextLineInd - lineInd > 5)
                            && ((k != lineInd && lines[k].lastPos < lines[k - 1].lastPos * 0.9)
                                    || (lines[k].lastPos - lines[k].indent) < maxWidth * 0.7)
                        ) {

                            //then this is the end of reference
                            break
                        }
                    }
                } else {
                    //this is the last reference and we should find it's end
                    for (k in lineInd until lines.size) {
                        curRef = addLineToReference(curRef, lines[k].str)
                        if (canUseSecondIndentPattern && k < lines.lastIndex &&
                            lines[k + 1].indent != secondLineIndent
                        ) {
                            //we find the end of reference
                            break
                        }
                        if (k > lineInd) {
                            if (lines[k].lastPos < lines[k - 1].lastPos * 0.9) {
                                //we find the end of reference
                                break
                            }
                        } else {
                            if (lines[k].lastPos - lines[k].indent < 0.9 * maxWidth) {
                                //we find the end of reference
                                break
                            }
                        }
                    }
                }
                refList.add(curRef)
            }
        } else {
            //analyze this text
            var canUseSecondIndentPattern = true
            var i = 0
            var secondLineIndentLeft = -1
            var secondLineIndentRight = -1

            //current page side(0 -- left, 1 -- right)
            fun getSide(line: CustomReferenceExtractor.Line): Int {
                return if (lines[0].lastPos < pageWidth * 0.7) 0 else 1
            }

            var curRefNum = 1
            val firstLineIndices = mutableListOf<Int>()
            var maxWidth = 0
            while (i < lines.size) {
                firstLineIndices.add(i)
                //find next reference
                maxWidth = Integer.max(maxWidth, lines[i].lastPos - lines[i].indent)
                var j = i + 1
                while (j < lines.size) {
                    val match = refRegex.find(lines[j].str)
                    val value = match?.value?.dropLast(1)?.toInt()
                    if (value != null && value == curRefNum + 1) {
                        break
                    } else {
                        if (value == curRefNum) {
                            return listOf()
                        }
                    }
                    j += 1
                }
                if (j != lines.size) {
                    for (k in i + 1 until j) {
                        val curSide = getSide(lines[k])
                        if (curSide == 0) {
                            if (secondLineIndentLeft == -1) {
                                secondLineIndentLeft = lines[k].indent
                            }
                            if (secondLineIndentLeft != lines[k].indent) {
                                canUseSecondIndentPattern = false
                            }
                        } else {
                            if (secondLineIndentRight == -1) {
                                secondLineIndentRight = lines[k].indent
                            }
                            if (secondLineIndentRight != lines[k].indent) {
                                canUseSecondIndentPattern = false
                            }
                        }
                    }
                } else {
                    //this was the last reference
                    break
                }
                curRefNum += 1
                i = j
            }

            if (!canUseSecondIndentPattern) {
                return listOf()
            }

            fun addLineToReference(ref: String, line: String): String {
                return if (ref.length > 2 && ref.last() == '-') {
                    if (ref[ref.lastIndex - 1].isLowerCase() && line.first().isLowerCase()) {
                        ref.dropLast(1) + line
                    } else {
                        ref + line
                    }
                } else {
                    if (ref == "") {
                        line
                    } else {
                        ref + " " + line
                    }
                }
            }

            //parse references
            for ((j, lineInd) in firstLineIndices.withIndex()) {
                var curRef = ""
                if (j != firstLineIndices.lastIndex) {
                    val nextLineInd = firstLineIndices[j + 1]
                    var prevSide = 0
                    for (k in lineInd until nextLineInd) {
                        curRef = addLineToReference(curRef, lines[k].str)
                        val curSide = getSide(lines[k])
                        if ((curRef.last() == '.' || nextLineInd - lineInd > 10)
                            && ((k != lineInd && curSide == prevSide && lines[k].lastPos < lines[k - 1].lastPos * 0.9)
                                    || (lines[k].lastPos - lines[k].indent) < maxWidth * 0.7)
                        ) {

                            //then this is the end of reference
                            break
                        }
                        prevSide = curSide
                    }
                } else {
                    //this is the last reference and we should find it's end
                    var prevSide = 0
                    for (k in lineInd until lines.size) {
                        curRef = addLineToReference(curRef, lines[k].str)
                        val curSide = getSide(lines[k])
                        if (canUseSecondIndentPattern && k < lines.lastIndex) {
                            if (curSide == 0 && lines[k + 1].indent != secondLineIndentLeft
                                || curSide == 1 && lines[k + 1].indent != secondLineIndentRight
                            ) {

                                //we find the end of reference
                                break
                            }
                        }
                        if (k > lineInd) {
                            if (prevSide == curSide && lines[k].lastPos < lines[k - 1].lastPos * 0.9) {
                                //we find the end of reference
                                break
                            }
                        } else {
                            if (lines[k].lastPos - lines[k].indent < 0.9 * maxWidth) {
                                //we find the end of reference
                                break
                            }
                        }
                        prevSide = curSide
                    }
                }
                refList.add(curRef)
            }
        }
        return refList
    }
}