package nl.utwente.student.metrics.generic

import nl.utwente.student.metrics.Metric

class DataClass: Metric<Int>() {
    override fun getTag(): String = "DC"
}