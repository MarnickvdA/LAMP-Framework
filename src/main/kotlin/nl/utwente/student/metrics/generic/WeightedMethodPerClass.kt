package nl.utwente.student.metrics.generic

import nl.utwente.student.metrics.Metric

class WeightedMethodPerClass: Metric<Int>() {
    override fun getTag(): String = "WMC"
}