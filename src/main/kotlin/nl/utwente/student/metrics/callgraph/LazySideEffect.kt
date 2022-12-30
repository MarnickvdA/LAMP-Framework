package nl.utwente.student.metrics.callgraph

import nl.utwente.student.metrics.Metric

class LazySideEffect: Metric<Int>() {
    override fun getTag(): String = "LSE"
}