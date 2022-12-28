package nl.utwente.student.metrics.smells

import nl.utwente.student.metrics.Metric

class MutatingLambdaFunction: Metric<Int>() {
    override fun getTag(): String = "MLF"
}