package nl.utwente.student.models.semantics

/**
 * Semantic Model of one source code project
 */
class SemanticTree(
    override val name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticElement {
    override val parent: SemanticElement? = null
    override fun toString(): String {
        return "project://$name"
    }

    fun addComponent(component: SemanticComponent): SemanticComponent {
        if (!elements.containsKey(component.name))
            add(component)

        return this.elements[component.name] as SemanticComponent
    }

    fun addModule(module: SemanticModule) = add(module)
}