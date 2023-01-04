package nl.utwente.student.models.semantics

class SemanticProperty(
    override val name: String,
    val module: SemanticModule,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticDeclarable {

    override fun toString(): String = "property://$name"
}