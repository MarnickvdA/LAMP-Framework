package nl.utwente.student.app

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metrics.CognitiveComplexity

class MetricRunner(private val modules: List<Module>) {
    fun run() {
        modules.forEach {
            val cognitiveComplexity = CognitiveComplexity().also { cc -> cc.visitModule(it) }
            val results = cognitiveComplexity.getMetricResults()

            println("\nCognitive Complexity for ${it.packageName}.${it.moduleScope.identifier.value}")
            results.forEach { (unitId, coco) ->
                println("${coco.toString().padEnd(4)} => $unitId")
            }
        }
    }
}