package nl.utwente.student.metrics

import nl.utwente.student.models.metrics.SemanticMetric

class LackOfCohesionInMethods : SemanticMetric {
    var result: Int? = 0
    override fun getTag(): String = "LCOM"
    override fun getResult(): Map<String, Int> {
        TODO("Not yet implemented")
    }

    /**
     * TODO Document how Lack Of Cohesion In Methods is measured
     */
}