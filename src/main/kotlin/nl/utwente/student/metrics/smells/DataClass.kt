package nl.utwente.student.metrics.smells

import nl.utwente.student.metrics.Metric

class DataClass: Metric<Int>() {
    override fun getTag(): String = "DC"
}