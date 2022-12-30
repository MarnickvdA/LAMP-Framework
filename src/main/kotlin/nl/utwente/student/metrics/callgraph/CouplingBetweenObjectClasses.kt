package nl.utwente.student.metrics.callgraph

import nl.utwente.student.metrics.Metric

class CouplingBetweenObjectClasses: Metric<Int>() {
    override fun getTag(): String = "CBO"
}