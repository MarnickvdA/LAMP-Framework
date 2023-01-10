package nl.utwente.student.metrics

import nl.utwente.student.models.metrics.SemanticMetric

class ResponseForAClass : SemanticMetric {
    override fun getTag(): String = "RFC"
    override fun getResult(): Map<String, Int> {
        TODO("Not yet implemented")
    }
}