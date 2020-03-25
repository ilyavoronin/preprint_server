package testpdf

import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.lang.Math.abs
import java.lang.Math.round


class PDFRefTextStripper(): PDFTextStripper() {
    val fontWidthToCnt = mutableMapOf<Int, Int>()
    var lastY = 0f
    var lastPageNo = 0
    var isTwoColumns = false
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        if (text == null || textPositions == null || textPositions.size == 0) {
            return
        }
        var newText = text
        val curY = textPositions[0].y
        val curX = textPositions[0].x
        val curPageNo = currentPageNo
        val pageHeight = currentPage.bBox.height
        val pageWidth = currentPage.bBox.width
        val curFontWidth = round(textPositions.last().font.boundingBox.width)
        for ((i, symbol) in textPositions.withIndex()) {
            val curFontWidth = round(symbol.font.boundingBox.width)
            fontWidthToCnt[curFontWidth] = (fontWidthToCnt[curFontWidth] ?: 0) + 1
        }

        //check if this document has two columns
        if (!isTwoColumns && curPageNo == lastPageNo && curY < lastY
            && lastY - curY > pageWidth / 2 && curX > pageWidth / 2.3) {
            isTwoColumns = true
        }

        //check if the font differs from standard document font and find word 'References'
        if (text.all {it.isUpperCase()} || curFontWidth != fontWidthToCnt.maxBy{it.value}!!.key ||
                textPositions[0].font.fontDescriptor?.fontName?.contains("bold",ignoreCase = true) ?: false) {
            val pos1 = text.indexOf("References")
            val pos2 = text.indexOf("REFERENCES")
            val pos = if (pos1 != -1) pos1 else pos2
            if (pos != -1) {
                //mark bold word 'References' with '\r\'
                newText = newText.substring(0, pos) + "\\r\\" + newText.substring(pos)
            }
        }


        //write the coordinate of the word
        newText = "@d" + round(textPositions[0].x).toString() + "@d" + newText

        //mark big spaces in the file
        if (curPageNo == lastPageNo && curY > lastY && curY - lastY > pageHeight / 5 ||
                curPageNo != lastPageNo && pageHeight - lastY > pageHeight / 3) {
            newText = "\n\\%\\\n" + newText
        }

        lastPageNo = curPageNo
        lastY = curY

        /*if (text != null && textPositions != null && textPositions.size > 0) {
            newText = text.trim(' ')
            //find if the font is bold
            val currentFontWidth = textPositions.last().font.boundingBox.width
            if (!fontWidthToCnt.containsKey(currentFontWidth)) {
                fontWidthToCnt[currentFontWidth] = 0
            }
            if (currentFontWidth != fontWidthToCnt.maxBy { it.value }!!.key || text.all {it.isUpperCase()} ||
                textPositions[0].font.fontDescriptor?.fontName?.contains("bold",ignoreCase = true) ?: false) {
                newText = "${newText}\\\$\\"
            }
            fontWidthToCnt[currentFontWidth] = fontWidthToCnt[currentFontWidth]!! + 1

            val curY = textPositions[0].y
            //checking if new line has started
            if (abs(curY - lastY) > 5) {
                //write the beginning coordinate of the line
                newText = "@d" + textPositions[0].x.toString() + "@d" + newText;
                firstLineOfNewPage = false
            }
            //find big empty spaces between the lines
            val diff =
                if (currentPageNo == lastPageN || curY > lastY) curY - lastY
                else textPositions[0].pageHeight - lastY + curY - (textPositions[0].pageHeight / 10)
            if (diff > textPositions[0].pageHeight) {
                println("$diff ${textPositions[0].pageHeight} $lastY $curY $currentPageNo $lastPageN")
                println(text.length)
                newText = "\\%\\${newText}"
            }

            //check if this line contains page number
            if (lastPageN != currentPageNo) {
                firstLineOfNewPage = true
            }
            val pageString = currentPageNo.toString()
            if (firstLineOfNewPage && text == pageString) {
                newText = "\\%p$newText"
            }
            lastY = curY
            lastPageN = currentPageNo
        }
         */
        super.writeString(newText, textPositions)
    }
}
