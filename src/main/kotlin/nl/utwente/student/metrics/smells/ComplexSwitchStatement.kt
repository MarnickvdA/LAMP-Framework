package nl.utwente.student.metrics.smells

import nl.utwente.student.metrics.Metric

class ComplexSwitchStatement: Metric<Int>() {
    override fun getTag(): String = "CSS"
}