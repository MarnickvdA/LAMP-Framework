package nl.utwente.student.metrics.smells

import nl.utwente.student.models.semantics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree
import nl.utwente.student.visitors.UnitVisitor

// TODO (Create: We have a lambda function that is mutating outside of its own scope)
class MutatingLambdaFunction(override var semanticTree: SemanticTree) : UnitVisitor(), SemanticMetric {
    override fun getTag(): String = "MLF"
}