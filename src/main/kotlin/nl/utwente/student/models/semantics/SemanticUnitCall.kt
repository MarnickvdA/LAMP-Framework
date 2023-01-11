package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Identifier
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.utils.getUniquePosition

class SemanticUnitCall(
    override val sourceElement: UnitCall,
    override val parent: SemanticElement?
) : SemanticExpression(
    (sourceElement.reference as? Identifier)?.value ?: (sourceElement.reference as? UnitCall)?.getUniqueName(null)
    ?: ("?" + getUniquePosition(sourceElement)), sourceElement, parent) {
    override fun toString(): String = "call://$name"
}