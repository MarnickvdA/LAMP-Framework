package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.symbol.SymbolTree
import nl.utwente.student.models.metrics.SymbolMetric

class NumberOfChildren: SymbolMetric {
    private var moduleResults = mutableListOf<Pair<String, Int>>()

    override fun visitProject(modules: List<ModuleRoot>, symbolTree: SymbolTree) {
        moduleResults = mutableListOf()
        symbolTree.modules.forEach {
            moduleResults.add(Pair(it.key, it.value.subModules.size))
        }
    }

    override fun getTag(): String = "NOC"

    override fun getResult(): List<Pair<String, Int>> {
        return moduleResults
    }
}