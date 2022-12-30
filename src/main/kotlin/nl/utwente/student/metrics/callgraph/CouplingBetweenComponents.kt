package nl.utwente.student.metrics.callgraph

import nl.utwente.student.metrics.Metric

class CouplingBetweenComponents: Metric<Int>() {
    override fun getTag(): String = "CBC"
}