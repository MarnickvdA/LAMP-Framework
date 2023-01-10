package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Property

class SemanticProperty(
    override val sourceElement: Property,
    override val parent: SemanticElement?,
) : SemanticDeclarable(sourceElement.identifier.value, sourceElement, parent) {
    override fun toString(): String = "property://$name"
}