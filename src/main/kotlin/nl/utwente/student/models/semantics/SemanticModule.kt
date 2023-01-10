package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.Module

class SemanticModule(
    override val name: String,
    override val sourceElement: Module,
    override val parent: SemanticElement?,
) : SemanticDeclarable(name, sourceElement, parent) {
    override fun toString(): String = "module://$name"
}