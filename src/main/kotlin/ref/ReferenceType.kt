package preprint.server.ref

enum class ReferenceType(val regex : Regex, val num : Int) {
    A("""^\[\d[1,3]]""".toRegex(), 0),
    B("""^(\d[1,3])""".toRegex(), 4),
    C("""^\[.*]""".toRegex(), 1),
    D("""^\d[1,3]\.""".toRegex(), 2),
    E("""^\d[1,3] """.toRegex(), 3),
    F("""^""".toRegex(), 5)
}