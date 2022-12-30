package nl.utwente.student.metrics.inheritance

import nl.utwente.student.metrics.Metric

class DepthOfInheritanceTree: Metric<Int>() {
    override fun getTag(): String = "DIT"
}