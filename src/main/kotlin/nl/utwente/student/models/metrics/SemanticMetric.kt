package nl.utwente.student.models.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot

interface SemanticMetric: Metric<List<Pair<String, Int>>> {
    fun visitProject(modules: List<ModuleRoot>)
}
