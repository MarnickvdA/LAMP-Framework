package nl.utwente.student.app

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metrics.CognitiveComplexity
import nl.utwente.student.utils.getFile
import java.io.File

class MetricRunner(private val modules: List<Module>) {
    fun run(output: String?): File? {
        val out = output?.let { getFile(it) }
        val modulesToEvaluate = modules.filter { it.moduleScope?.members?.isNotEmpty() == true }

        modulesToEvaluate.parallelStream().forEach {
            val cognitiveComplexity = CognitiveComplexity().also { cc -> cc.visitModule(it) }
            val results = cognitiveComplexity.getMetricResults()

            if (results.isNotEmpty()) {
                println("\nCognitive Complexity for ${it.packageName}.${it.moduleScope.identifier.value}")
                results.forEach { (unitId, coco) ->
                    println("${unitId.padEnd(32)} = $coco")
                }
            }
        }

        return out
    }
}