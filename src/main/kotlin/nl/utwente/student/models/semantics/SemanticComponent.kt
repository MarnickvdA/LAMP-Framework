package nl.utwente.student.models.semantics

class SemanticComponent(
    override val name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf(),
) : SemanticElement {
    override val parent: SemanticElement? = null
    fun addModule(module: SemanticModule) = add(module)
    override fun toString(): String = "component://$name"
}