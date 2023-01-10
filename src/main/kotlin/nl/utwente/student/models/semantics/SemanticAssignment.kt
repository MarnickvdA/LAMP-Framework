package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Assignment
import nl.utwente.student.metamodel.v3.Identifier

class SemanticAssignment(
    override val sourceElement: Assignment,
    override val parent: SemanticElement?
) : SemanticExpression((sourceElement.reference as Identifier).value, sourceElement, parent) {
    override fun toString(): String = "assignment://$name"
}