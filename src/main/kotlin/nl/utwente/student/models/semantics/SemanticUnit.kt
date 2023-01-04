package nl.utwente.student.models.semantics

class SemanticUnit(
    override val name: String,
    val module: SemanticModule,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticDeclarable {
    override fun toString(): String = "unit://$name"
}