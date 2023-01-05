package nl.utwente.student.metrics

import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.models.semantics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree

/**
 * Count of references in both directions between two classes (property access or unit calls (excluding constructor calls?))
 * Per module: A set of outgoing AND incoming calls and property access
 *
 * TODO Q: Should library references be included or only classes written in the project?
 */
class CouplingBetweenObjectClasses(override var semanticTree: SemanticTree) : ModuleVisitor(), SemanticMetric {
    override var result: Int? = 0
    override fun getTag(): String = "CBO"
}