package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.SourceElement

abstract class SemanticDeclarable(
    override val name: String,
    override val sourceElement: SourceElement,
    override val parent: SemanticElement?,
) : SemanticElement {
    override val children: MutableMap<String, SemanticElement> = mutableMapOf()

    override fun toString(): String = "scope://$name"
}