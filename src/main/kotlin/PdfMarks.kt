package testpdf

enum class PdfMarks(val str : String, val num: Int) {
    PageStart("\\@pe\\", -1),
    PageEnd("\\@ps\\", -2),
    RareFont("\\r\\", 0),
    IntBeg("@d", 0),
    IntEnd("@d", 0),
}