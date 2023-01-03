package nl.utwente.student.models.semantics

class SemanticComponent(
    name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticElement {
    override val name = name
        get() = "component://$field"

    fun addModule(module: SemanticModule) = add(module)
}