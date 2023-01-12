package nl.utwente.student.metrics.smells

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.metrics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree

// TODO (Create: We have a lambda function that is mutating outside of its own scope)
class MutatingLambdaFunction : SemanticMetric {
    override fun visitProject(modules: List<ModuleRoot>) {
        TODO("Not yet implemented")
    }

    override fun getTag(): String = "MLF"

    override fun getResult(): List<Pair<String, Int>> {
        TODO("Not yet implemented")
    }
}