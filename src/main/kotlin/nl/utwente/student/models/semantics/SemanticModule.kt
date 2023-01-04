package nl.utwente.student.models.semantics

class SemanticModule(
    override val name: String,
    val component: SemanticComponent?,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf(),
) : SemanticDeclarable {
    override fun toString(): String = "module://$name"
}