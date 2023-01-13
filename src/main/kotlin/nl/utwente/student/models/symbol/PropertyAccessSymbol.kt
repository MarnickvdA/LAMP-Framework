package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.Assignment

open class PropertyAccessSymbol(
    private val referenceId: String,
    parent: SourceSymbol,
    assignment: Assignment
) : SourceSymbol(referenceId, parent, assignment), ReferenceSymbol {
    override val reference: SourceSymbol? = null

    override fun toString(): String = "access(id=\"${referenceId}\")"
}