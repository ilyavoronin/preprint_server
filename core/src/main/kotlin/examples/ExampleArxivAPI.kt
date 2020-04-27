import com.preprint.server.arxiv.ArxivAPI

val recordIds = listOf("1507.11111", "1604.08289", "1608.08082")

fun main() {
    for (elem in ArxivAPI.getArxivRecords(recordIds)) {
        println(elem)
    }
}