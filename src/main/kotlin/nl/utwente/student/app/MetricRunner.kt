package nl.utwente.student.app

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metrics.CognitiveComplexity
import nl.utwente.student.metrics.CyclomaticComplexity
import nl.utwente.student.utils.getFile
import java.io.File

class MetricRunner(private val modules: List<Module>) {
    fun run(output: String?): File? {
        val out = output?.let { getFile(it) }
        val modulesToEvaluate = modules.filter { it.moduleScope?.members?.isNotEmpty() == true }

        modulesToEvaluate.forEach {
            val cyclomaticComplexity = CyclomaticComplexity().also { cc -> cc.visitModule(it) }
            val cognitiveComplexity = CognitiveComplexity().also { cc -> cc.visitModule(it) }

            val results = mutableMapOf<String, MutableList<Pair<String, Int>>?>()

            cyclomaticComplexity.getMetricResults().forEach { (unitId, result) ->
                results[unitId] = (results[unitId] ?: mutableListOf()).also {
                    it.add(Pair(cyclomaticComplexity.getTag(), result))
                }
            }

            cognitiveComplexity.getMetricResults().forEach { (unitId, result) ->
                results[unitId] = (results[unitId] ?: mutableListOf()).also {
                    it.add(Pair(cognitiveComplexity.getTag(), result))
                }
            }

            if (results.isNotEmpty()) {
                results.forEach { (unitId, metrics) ->
                    println(unitId.padEnd(100))
                    metrics?.forEach {
                        println("\t${it.first.padEnd(6)}: ${it.second}")
                    }
                }
            }
        }

        return out
    }
}