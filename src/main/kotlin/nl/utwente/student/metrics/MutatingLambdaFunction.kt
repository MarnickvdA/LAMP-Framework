package nl.utwente.student.metrics

import nl.utwente.student.models.semantics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree
import nl.utwente.student.visitors.UnitVisitor

class MutatingLambdaFunction(override var semanticTree: SemanticTree) : UnitVisitor(), SemanticMetric {
    override fun getTag(): String = "MLF"
}