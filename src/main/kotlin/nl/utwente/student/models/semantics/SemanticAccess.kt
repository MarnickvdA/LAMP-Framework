package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Identifier

class SemanticAccess(
    override val sourceElement: Identifier,
    override val parent: SemanticElement?,
) : SemanticExpression(sourceElement.value, sourceElement, parent) {
    override fun toString(): String = "access://$name"
}