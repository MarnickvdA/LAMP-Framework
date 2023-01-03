package nl.utwente.student.models.semantics

class SemanticUnitCall(
    targetUnit: String,
    origin: SemanticDeclarable,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticElement {
    init {
        origin.addOutgoingUnitCall(this)
    }

    override val name = targetUnit
        get() = "call://$field"

    fun addArgument(property: SemanticProperty) = add(property)
}