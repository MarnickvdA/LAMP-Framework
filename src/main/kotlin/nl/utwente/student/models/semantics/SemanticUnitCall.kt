package nl.utwente.student.models.semantics

class SemanticUnitCall(
    override val name: String,
    origin: SemanticDeclarable,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticElement {
    init {
        origin.addOutgoingUnitCall(this)
    }

    fun addArgument(property: SemanticProperty) = add(property)

    override fun toString(): String = "call://$name"
}