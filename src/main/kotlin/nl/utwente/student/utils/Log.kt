package nl.utwente.student.utils

object Log {
    private const val DEBUG = false
    fun d(message: Any?) {
        if (DEBUG) println(message)
    }
}