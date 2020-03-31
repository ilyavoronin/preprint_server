package preprint.server.ref

enum class ReferenceType(val regex : Regex, val firstRegex : Regex, val num : Int) {
    A("""^\[\d{1,4}]""".toRegex(), """^\[1]""".toRegex(), 0),
    B("""^(\d{1,4})""".toRegex(), "(1)".toRegex(), 1),
    C("""^\[.*]""".toRegex(), """^\[.*]""".toRegex(), 2),
    D("""^\d{1,4}\.""".toRegex(), """1\.""".toRegex(), 3),
    E("""^\d{1,4} """.toRegex(), """1 """.toRegex(), 4),
    F("""^""".toRegex(), "^".toRegex(), 5)
}