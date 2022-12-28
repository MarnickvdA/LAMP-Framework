package nl.utwente.student.metrics

class ResponseForAClass: Metric<Int>() {
    override fun getTag(): String = "RFC"
}