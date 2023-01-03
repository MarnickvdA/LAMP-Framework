package nl.utwente.student.models.semantics

class SemanticModule(
    name: String,
    val component: SemanticComponent?,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf(),
) : SemanticDeclarable {

    override val name = name
        get() = "module://$field"
}