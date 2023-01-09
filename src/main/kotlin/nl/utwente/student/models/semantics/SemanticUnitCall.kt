package nl.utwente.student.models.semantics

class SemanticUnitCall(
    override val name: String,
    override val parent: SemanticElement?,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf(),
) : SemanticElement {
    init {
        parent?.add(this)
    }

    override fun toString(): String = "call://$name"
}