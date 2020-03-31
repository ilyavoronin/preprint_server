package preprint.server.ref

enum class ReferenceType(val regex : Regex, val firstRegex : Regex, val num : Int) {
    A("""^\[\d{1,4}]""".toRegex(), """^\[1]""".toRegex(), 0),
    B("""^\d{1,4}\.""".toRegex(), """1\.""".toRegex(), 1),
    C("""^\d{1,4}\s""".toRegex(), """1 """.toRegex(), 2),
    D("""^\[.*]""".toRegex(), """^\[.*]""".toRegex(), 3),
    E("""^""".toRegex(), "^".toRegex(), 4)
}