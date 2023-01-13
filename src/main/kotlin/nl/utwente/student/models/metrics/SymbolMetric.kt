package nl.utwente.student.models.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.symbol.SymbolTree

interface SymbolMetric: Metric<List<Pair<String, Int>>> {
    fun visitProject(modules: List<ModuleRoot>, symbolTree: SymbolTree)
}
