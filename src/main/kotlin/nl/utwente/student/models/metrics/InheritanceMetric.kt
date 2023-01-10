package nl.utwente.student.models.metrics

import nl.utwente.student.models.inheritance.InheritanceTree

interface InheritanceMetric: Metric<List<Pair<String, Int>>> {
    fun visitProject(inheritanceTree: InheritanceTree)
}