package nl.utwente.student.models.semantics

class SemanticAssignment(
    override val name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
    ) : SemanticElement {

    override fun toString(): String = "assign://$name"
}