package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.symbol.*
import nl.utwente.student.models.metrics.SymbolMetric

class DepthOfInheritanceTree: SymbolMetric {
    private var moduleResults = mutableListOf<Pair<String, Int>>()

    override fun visitProject(modules: List<ModuleRoot>, symbolTree: SymbolTree) {
        moduleResults = mutableListOf()
        symbolTree.modules.forEach {
            moduleResults.add(Pair(it.key, getDepthOfInheritanceTree(it.value)))
        }
    }

    override fun getTag(): String = "DIT"

    override fun getResult(): List<Pair<String, Int>> {
        return moduleResults
    }

    private fun getDepthOfInheritanceTree(module: SourceSymbol): Int {
        return when {
            module.parent != null -> getDepthOfInheritanceTree(module.parent!!) + 1
            else -> 0
        }
    }
}