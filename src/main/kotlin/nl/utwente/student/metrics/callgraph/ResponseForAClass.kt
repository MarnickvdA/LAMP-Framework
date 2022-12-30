package nl.utwente.student.metrics.callgraph

import nl.utwente.student.metrics.Metric

class ResponseForAClass: Metric<Int>() {
    override fun getTag(): String = "RFC"
}