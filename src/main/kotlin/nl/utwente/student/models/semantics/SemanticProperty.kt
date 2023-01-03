package nl.utwente.student.models.semantics

class SemanticProperty(
    name: String,
    val module: SemanticModule,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticDeclarable {

    override val name = name
        get() = "property://$field"
}