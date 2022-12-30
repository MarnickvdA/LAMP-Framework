package nl.utwente.student.metrics.generic

import nl.utwente.student.metrics.Metric

class LackOfListComprehension: Metric<Int>() {
    override fun getTag(): String = "LOLC"
}