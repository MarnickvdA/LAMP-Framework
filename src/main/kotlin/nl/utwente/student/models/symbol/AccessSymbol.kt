package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.Access

class AccessSymbol(
    private val access: Access,
    parent: SourceSymbol
): SourceSymbol(access.declarableId, parent, access), ReferenceSymbol {
    override val reference: SourceSymbol? = null

    override fun toString(): String = "access(to=${access.declarableId})"
}