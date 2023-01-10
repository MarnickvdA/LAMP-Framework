package nl.utwente.student.metrics

import nl.utwente.student.models.metrics.SemanticMetric

/**
 * Count of references in both directions between two classes (property access or unit calls (excluding constructor calls?))
 * Per module: A set of outgoing AND incoming calls and property access
 *
 * TODO Q: Should library references to be included or only classes written in the project?
 */
class CouplingBetweenObjectClasses : SemanticMetric {
    override fun getTag(): String = "CBO"
    override fun getResult(): Map<String, Int> {
        TODO("Not yet implemented")
    }

}