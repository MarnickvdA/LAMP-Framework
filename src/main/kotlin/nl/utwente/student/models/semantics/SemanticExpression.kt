package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Expression

open class SemanticExpression(
    override val name: String,
    override val sourceElement: Expression,
    override val parent: SemanticElement?,
) : SemanticElement {
    final override val children: MutableMap<String, SemanticElement> = mutableMapOf()

    override fun toString(): String = "scope://$name"
}