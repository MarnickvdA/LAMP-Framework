package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Unit

class SemanticUnit(
    override val sourceElement: Unit,
    override val parent: SemanticElement?,
) : SemanticDeclarable(sourceElement.identifier.value, sourceElement, parent) {
    override fun toString(): String = "unit://$name"
}