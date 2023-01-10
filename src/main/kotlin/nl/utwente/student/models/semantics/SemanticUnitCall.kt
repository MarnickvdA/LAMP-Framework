package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Identifier
import nl.utwente.student.metamodel.v3.UnitCall

class SemanticUnitCall(
    override val sourceElement: UnitCall,
    override val parent: SemanticElement?
) : SemanticExpression((sourceElement.reference as Identifier).value, sourceElement, parent) {
    override fun toString(): String = "call://$name"
}