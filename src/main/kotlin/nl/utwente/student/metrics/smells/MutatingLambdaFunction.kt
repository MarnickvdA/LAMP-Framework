package nl.utwente.student.metrics.smells

import nl.utwente.student.models.metrics.SemanticMetric

// TODO (Create: We have a lambda function that is mutating outside of its own scope)
class MutatingLambdaFunction : SemanticMetric {
    override fun getTag(): String = "MLF"
    override fun getResult(): Map<String, Int> {
        TODO("Not yet implemented")
    }
}