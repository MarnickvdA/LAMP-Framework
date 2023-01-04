package nl.utwente.student.models.semantics

class SemanticScope(
    override val name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticDeclarable {
    override fun toString(): String = "scope://$name"
}