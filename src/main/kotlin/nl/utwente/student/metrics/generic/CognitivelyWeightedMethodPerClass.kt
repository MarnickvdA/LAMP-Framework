package nl.utwente.student.metrics.generic

import nl.utwente.student.metrics.Metric

class CognitivelyWeightedMethodPerClass: Metric<Int>() {
    override fun getTag(): String = "CWMC"
}