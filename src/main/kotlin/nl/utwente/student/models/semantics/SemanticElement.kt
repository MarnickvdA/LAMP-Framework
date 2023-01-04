package nl.utwente.student.models.semantics

interface SemanticElement {
    val name: String
    val elements: MutableMap<String, SemanticElement>

    fun add(element: SemanticElement) {
        elements[element.name] = element
    }

    fun print(depth: Int = 0) {
        println("\t".repeat(depth) + toString())
        elements.values.forEach { it.print(depth + 1) }
    }
    override fun toString(): String
}