package nl.utwente.student.models.metrics

interface Metric<R> {
    fun getTag(): String

    fun getResult(): R
}





