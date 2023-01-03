package nl.utwente.student.models.semantics

class SemanticAssignment(
    name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
    ) : SemanticElement {

    override val name = name
        get() = "assign://$field"
}