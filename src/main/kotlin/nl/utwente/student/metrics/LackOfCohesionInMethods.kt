package nl.utwente.student.metrics

import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.models.semantics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree

class LackOfCohesionInMethods(override var semanticTree: SemanticTree) : ModuleVisitor(), SemanticMetric {
    override var result: Int? = 0
    override fun getTag(): String = "LCOM"
}