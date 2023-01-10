package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.SourceElement

class SemanticTree : SemanticElement {
    override val name: String = "project://"
    override val sourceElement: SourceElement? = null
    override val parent: SemanticElement? = null
    override val children: MutableMap<String, SemanticElement> = mutableMapOf()

    override fun toString(): String {
        return name
    }
}