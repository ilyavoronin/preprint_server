package testpdf

fun main() {
    for (elem in ArxivAPI.getRecordsLinks(listOf("1507.00493", "1604.08289", "1608.08082"))!!) {
        println(elem)
    }
}