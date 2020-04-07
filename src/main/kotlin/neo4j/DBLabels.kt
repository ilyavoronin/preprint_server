package neo4j

enum class DBLabels(val str: String) {
    PUBLICATION("Publication"),
    AUTHOR("Author"),
    AFFILIATION("Affiliation"),
    JOURNAL("Journal"),
    AUTHORED("AUTHORED_BY"),
    WORKS("WORKS"),
    CITES("CITES"),
    PUBLISHED_IN("PUBLISHED_IN")
}