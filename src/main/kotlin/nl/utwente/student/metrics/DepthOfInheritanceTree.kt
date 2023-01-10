package nl.utwente.student.metrics

import nl.utwente.student.models.inheritance.InheritanceNode
import nl.utwente.student.models.inheritance.InheritanceTree
import nl.utwente.student.models.metrics.InheritanceMetric

class DepthOfInheritanceTree: InheritanceMetric {
    private var moduleResults = mutableListOf<Pair<String, Int>>()

    override fun visitProject(inheritanceTree: InheritanceTree) {
        moduleResults = mutableListOf()
        inheritanceTree.forEach {
            moduleResults.add(Pair(it.key, getDepthOfInheritanceTree(it.value)))
        }
    }

    override fun getTag(): String = "DIT"

    override fun getResult(): List<Pair<String, Int>> {
        return moduleResults
    }

    private fun getDepthOfInheritanceTree(inheritanceTree: InheritanceNode): Int {
        return when {
            inheritanceTree.children.isEmpty() -> 1
            else -> inheritanceTree.children.maxOf { getDepthOfInheritanceTree(it) } + 1
        }
    }
}