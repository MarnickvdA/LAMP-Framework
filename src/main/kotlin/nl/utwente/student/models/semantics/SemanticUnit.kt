package nl.utwente.student.models.semantics

class SemanticUnit(
    name: String,
    val module: SemanticModule,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticDeclarable {
    override val name = name
        get() = "unit://$field"
}