package nl.utwente.student.metrics.smells

import nl.utwente.student.metrics.Metric

class LackOfListComprehension: Metric<Int>() {
    override fun getTag(): String = "LOLC"
}