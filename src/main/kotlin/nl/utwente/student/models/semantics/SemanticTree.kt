package nl.utwente.student.models.semantics

/**
 * Semantic Model of one source code project
 */
class SemanticTree(
    name: String,
    override val elements: MutableMap<String, SemanticElement> = mutableMapOf()
) : SemanticElement {

    override val name = name
        get() = "project://$field"

    private fun addSemanticElement(element: SemanticElement) {
        if (elements.containsKey(element.name)) return
        add(element)
    }

    fun addComponent(component: SemanticComponent): SemanticComponent {
        this.addSemanticElement(component)
        return this.elements[component.name] as SemanticComponent
    }

    fun addModule(module: SemanticModule) = add(module)
}