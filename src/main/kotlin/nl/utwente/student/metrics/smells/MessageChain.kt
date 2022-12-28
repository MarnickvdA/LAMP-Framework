package nl.utwente.student.metrics.smells

import nl.utwente.student.metrics.Metric

class MessageChain: Metric<Int>() {
    override fun getTag(): String = "MC"
}