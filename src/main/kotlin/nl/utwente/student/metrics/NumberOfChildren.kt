package nl.utwente.student.metrics

import nl.utwente.student.models.inheritance.InheritanceTree
import nl.utwente.student.models.metrics.InheritanceMetric

class NumberOfChildren: InheritanceMetric {
    private var moduleResults = mutableListOf<Pair<String, Int>>()

    override fun visitProject(inheritanceTree: InheritanceTree) {
        moduleResults = mutableListOf()
        inheritanceTree.forEach {
            moduleResults.add(Pair(it.key, it.value.children.size))
        }
    }

    override fun getTag(): String = "NOC"

    override fun getResult(): List<Pair<String, Int>> {
        return moduleResults
    }
}