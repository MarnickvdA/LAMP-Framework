package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.SourceElement

class SemanticComponent(
    override val name: String,
) : SemanticElement {
    override val parent: SemanticElement? = null
    override val sourceElement: SourceElement? = null
    override val children: MutableMap<String, SemanticElement> = mutableMapOf()
    override fun toString(): String = "component://$name"
}