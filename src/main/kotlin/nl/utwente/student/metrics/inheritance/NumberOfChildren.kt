package nl.utwente.student.metrics.inheritance

import nl.utwente.student.metrics.Metric

class NumberOfChildren: Metric<Int>() {
    override fun getTag(): String = "NOC"
}