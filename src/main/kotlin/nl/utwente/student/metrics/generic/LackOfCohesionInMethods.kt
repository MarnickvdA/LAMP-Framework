package nl.utwente.student.metrics.generic

import nl.utwente.student.metrics.Metric

class LackOfCohesionInMethods: Metric<Int>() {
    override fun getTag(): String = "LCOM"
}