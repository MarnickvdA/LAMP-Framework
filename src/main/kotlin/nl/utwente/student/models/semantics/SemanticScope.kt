package nl.utwente.student.models.semantics

class SemanticScope(
    override val name: String,
    override val parent: SemanticElement?,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf(),
) : SemanticElement {

    init {
        parent?.add(this)
    }

    override fun toString(): String = "scope://$name"
}