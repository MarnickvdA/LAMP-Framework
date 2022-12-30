package nl.utwente.student.metrics.generic

import nl.utwente.student.metrics.Metric

class MessageChain: Metric<Int>() {
    override fun getTag(): String = "MC"
}