package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.Call

class CallSymbol(
    private val call: Call,
    parent: SourceSymbol
): SourceSymbol(call.referenceId, parent, call), ReferenceSymbol {
    override val reference: SourceSymbol? = null

    override fun toString(): String = "call(to=${call.referenceId})"
}