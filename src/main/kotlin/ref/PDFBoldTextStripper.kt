package preprint.server.ref

import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.lang.Math.abs


object PDFBoldTextStripper: PDFTextStripper() {
    val fontWidthToCnt = mutableMapOf<Float, Int>()
    var lastY = 0f
    var lastPageN = 0
    var firstLineOfNewPage = false
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        var newText = text
        if (text != null && textPositions != null && textPositions.size > 0) {
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
        super.writeString(newText, textPositions)
    }
}
