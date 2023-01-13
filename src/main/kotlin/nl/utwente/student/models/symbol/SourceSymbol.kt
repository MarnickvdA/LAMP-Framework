package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.SourceElement

open class SourceSymbol(
    val id: String,
    var parent: SourceSymbol? = null,
    val sourceElement: SourceElement? = null,
    val children: MutableList<SourceSymbol> = mutableListOf()
) {
    fun add(symbol: SourceSymbol?) {
        symbol?.let { children.add(it) }
    }

    fun addAll(symbolList: List<SourceSymbol?>) {
        symbolList.forEach(this::add)
    }

    fun print(depth: Int = 0) {
        println("\t".repeat(depth) + toString())
        children.forEach { it.print(depth + 1) }
    }

    override fun toString(): String = "\"$id\" @$sourceElement"
}