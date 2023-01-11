package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Assignment
import nl.utwente.student.metamodel.v3.Identifier
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.utils.getUniquePosition

class SemanticAssignment(
    override val sourceElement: Assignment,
    override val parent: SemanticElement?
) : SemanticExpression(
    (sourceElement.reference as? Identifier)?.value ?: (sourceElement.reference as? UnitCall)?.getUniqueName(null)
    ?: ("?" + getUniquePosition(sourceElement)), sourceElement, parent) {
    // TODO Change creation of the name. reference can be a UnitCall.
    override fun toString(): String = "assignment://$name"
}