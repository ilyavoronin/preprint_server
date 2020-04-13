import com.preprint.server.arxiv.ArxivAPI

val recordIds = listOf("1507.00493", "1604.08289", "1608.08082")

fun main() {
    for (elem in ArxivAPI.getRecordsLinks(recordIds)) {
        println(elem)
    }
}