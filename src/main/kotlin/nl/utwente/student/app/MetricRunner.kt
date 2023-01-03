package nl.utwente.student.app

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metrics.*
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getFile
import nl.utwente.student.visitors.SemanticTreeVisitor
import java.io.File

class MetricRunner(private val modules: List<Module>) {
    private val unitMetrics = listOf(
        CyclomaticComplexity(),
        CognitiveComplexity(),
        LinesOfLambda(),
        NumberOfParameters()
    )

    private val moduleMetrics = listOf(
        WeightedMethodPerClass(),
        CognitivelyWeightedMethodPerClass(),
        LambdaCount()
    )

    fun run(output: String?): File? {
        val out = output?.let { getFile(it) }
        val modulesToEvaluate = modules.filter { it.moduleScope?.members?.isNotEmpty() == true }

        modulesToEvaluate.forEach {
            println("\n==== MODULE METRICS ====")
            val moduleMetrics = calculateModuleMetrics(it)
            moduleMetrics.forEach { metric -> this.printModuleMetrics(metric.key, metric.value)}

            val unitMetrics = calculateUnitMetrics(it)
            println("\n==== UNIT METRICS ====")
            unitMetrics.forEach { metric -> this.printUnitMetrics(metric.key, metric.value) }
        }

//        SemanticTreeVisitor("Test", modulesToEvaluate).getResult().print()

        return out
    }

    private fun printUnitMetrics(unitId: String, metrics: List<Pair<String, Int>>) {
        println(unitId.padEnd(100))
        metrics.forEach {
            println("\t${it.first.padEnd(6)}: ${it.second}")
        }
    }

    private fun printModuleMetrics(unitId: String, metrics: List<Pair<String, Int>>) {
        println(unitId.padEnd(100))
        metrics.forEach {
            println("\t${it.first.padEnd(6)}: ${it.second}")
        }
    }

    private fun aggregateMetricOutput(
        metricOutput: Map<String, Pair<String, Int>>,
        results: MutableMap<String, MutableList<Pair<String, Int>>>
    ) {
        metricOutput.forEach { (k, v) ->
            results[k] = (results[k] ?: mutableListOf()).also { metrics ->
                metrics.add(v)
            }
        }
    }

    private fun calculateUnitMetrics(module: Module): Map<String, MutableList<Pair<String, Int>>> {
        val results = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        unitMetrics.map { this.evaluateUnitMetric(it, module) }.forEach { aggregateMetricOutput(it, results) }
        return results
    }

    private fun calculateModuleMetrics(module: Module): Map<String, List<Pair<String, Int>>> {
        val results = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        moduleMetrics.map { this.evaluateModuleMetric(it, module) }.forEach { aggregateMetricOutput(it, results) }
        return results
    }

    private fun evaluateUnitMetric(metric: UnitVisitor, module: Module): Map<String, Pair<String, Int>> {
        val results = mutableMapOf<String, Pair<String, Int>>()

        // Calculate the metric
        metric.visitModule(module)

        // Put the results in a format grouped by unitId
        metric.getResult().forEach { (unitId, result) ->
            if (results[unitId] != null) System.err.println("We are overriding ${metric.getTag()} for $unitId!")
            results[unitId] = Pair(metric.getTag(), result)
        }

        return results
    }

    private fun evaluateModuleMetric(metric: ModuleVisitor, module: Module): Map<String, Pair<String, Int>> {
        val results = mutableMapOf<String, Pair<String, Int>>()

        // Calculate the metric
        metric.visitModule(module)

        // Put the results in a format grouped by moduleId
        metric.getResult().also { (moduleId, result) ->
            if (results[moduleId] != null) System.err.println("We are overriding ${metric.getTag()} for $moduleId!")
            results[moduleId] = Pair(metric.getTag(), result)
        }

        return results
    }
}