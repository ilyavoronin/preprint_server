package preprint.server.ref


interface ReferenceParser {
    fun parse(lines : List<CustomReferenceExtractor.Line>,
              isTwoColumns : Boolean, pageWidth : Int) : List<String>
}