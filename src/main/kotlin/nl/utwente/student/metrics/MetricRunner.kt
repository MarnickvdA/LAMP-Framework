package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metrics.generic.CognitiveComplexity
import nl.utwente.student.metrics.generic.CyclomaticComplexity
import nl.utwente.student.utils.getFile
import java.io.File

class MetricRunner(private val modules: List<Module>) {
    fun run(output: String?): File? {
        val out = output?.let { getFile(it) }
        val modulesToEvaluate = modules.filter { it.moduleScope?.members?.isNotEmpty() == true }

        modulesToEvaluate.forEach {

            val results = mutableMapOf<String, MutableList<Pair<String, Int>>?>()

            fun addToResults(metricResults: Map<String, Pair<String, Int>>) {
                metricResults.forEach { (k, v) ->
                    results[k] = (results[k] ?: mutableListOf()).also { metrics ->
                        metrics.add(v)
                    }
                }
            }

            addToResults(getCyclomaticComplexity(it))
            addToResults(getCognitiveComplexity(it))

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

    fun getCyclomaticComplexity(module: Module): Map<String, Pair<String, Int>> {
        val cyclomaticComplexity = CyclomaticComplexity().also { cc -> cc.visitModule(module) }
        val results = mutableMapOf<String, Pair<String, Int>>()

        cyclomaticComplexity.getMetricResults().forEach { (unitId, result) ->
            if (results[unitId] != null) System.err.println("WE ARE OVERRIDING COCO FOR $unitId! :(")
            results[unitId] = Pair(cyclomaticComplexity.getTag(), result)
        }

        return results
    }

    fun getCognitiveComplexity(module: Module): Map<String, Pair<String, Int>> {
        val cognitiveComplexity = CognitiveComplexity().also { cc -> cc.visitModule(module) }
        val results = mutableMapOf<String, Pair<String, Int>>()

        cognitiveComplexity.getMetricResults().map { (unitId, result) ->
            if (results[unitId] != null) System.err.println("WE ARE OVERRIDING COCO FOR $unitId! :(")
            results[unitId] = Pair(cognitiveComplexity.getTag(), result)
        }

        return results
    }
}