package nl.utwente.student.metrics

import nl.utwente.student.models.metrics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree

/**
 * TODO Document how Lack Of Cohesion In Methods is measured
 */
class LackOfCohesionInMethods : SemanticMetric {
    override fun visitProject(semanticTree: SemanticTree) {
        TODO("Not yet implemented")
    }

    override fun getTag(): String = "LCOM"

    override fun getResult(): List<Pair<String, Int>> {
        TODO("Not yet implemented")
    }
}