package nl.utwente.student.metrics.callgraph

import nl.utwente.student.metrics.Metric

class ComplexSwitchStatement: Metric<Int>() {
    override fun getTag(): String = "CSS"
}