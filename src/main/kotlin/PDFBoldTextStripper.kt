package testpdf

import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition


class PDFBoldTextStripper(): PDFTextStripper() {
    val fontWidthToCnt = mutableMapOf<Float, Int>()
    var lastY = 0f
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        var newText = text
        if (text != null && textPositions != null && textPositions.size > 0) {
            newText = text.trim(' ')
            //find if the font is bold
            val currentFontWidth = textPositions.last().font.boundingBox.width
            if (!fontWidthToCnt.containsKey(currentFontWidth)) {
                fontWidthToCnt[currentFontWidth] = 0
            }
            if (currentFontWidth > fontWidthToCnt.maxBy { it.value }!!.key || text.all {it.isUpperCase()} ||
                    textPositions[0].font.fontDescriptor?.fontName?.contains("bold",ignoreCase = true) ?: false) {
                newText = "${newText}\\\$\\"
            }
            fontWidthToCnt[currentFontWidth] = fontWidthToCnt[currentFontWidth]!! + 1

            //find big empty spaces between the lines
            val curY = textPositions[0].y
            print(curY)
            print(" ")
            print(lastY)
            print(" ")
            println(textPositions[0].pageHeight)
            val diff =
                if (curY + 30 >= lastY) curY - lastY
                else textPositions[0].pageHeight - lastY + curY - 80
            println(diff)
            if (diff > 200) {
                newText = "\\%\\${newText}"
            }
            lastY = curY
        }
        super.writeString(newText, textPositions)
    }
}
