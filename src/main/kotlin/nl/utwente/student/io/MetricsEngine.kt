package nl.utwente.student.io

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metrics.*
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getFile
import nl.utwente.student.visitors.InheritanceTreeVisitor
import nl.utwente.student.visitors.SemanticTreeVisitor
import java.io.File

object MetricsEngine {

    private val moduleMetrics = listOf(
        WeightedMethodPerClass(),
        CognitivelyWeightedMethodPerClass(),
        LambdaCount()
    )

    private val unitMetrics = listOf(
        CyclomaticComplexity(),
        CognitiveComplexity(),
        NumberOfParameters(),
        UnitLinesOfCode()
    )

    private val expressionMetrics = listOf(
        LinesOfLambda(),
//        LengthOfMessageChain()
    )

    fun run(modules: List<Module>, output: String?): File? {
        val out = output?.let { getFile(it) }

//        val moduleResults = mutableMapOf<String, MutableList<Pair<String, Int>>>()
//        var unitResults = mutableMapOf<String, MutableList<Pair<String, Int>>>()
//        var expressionResults = mutableMapOf<String, MutableList<Pair<String, Int>>>()
//
//        InheritanceTreeVisitor(modules).getResult()
//            .forEach {
//                moduleResults[it.key] = mutableListOf(
//                    Pair("DIT", it.value.getDepthOfInheritanceTree()),
//                    Pair("NOC", it.value.getNumberOfChildren())
//                )
//            }
//
//        modules.filter { it.moduleScope.members.size > 0 }.forEach {
//            calculateModuleMetrics(moduleMetrics, it).forEach { moduleMetrics ->
//                if (moduleResults[moduleMetrics.key] == null)
//                    moduleResults[moduleMetrics.key] = moduleMetrics.value.toMutableList()
//                else moduleResults[moduleMetrics.key]?.addAll(moduleMetrics.value)
//            }
//
//            unitResults = calculateUnitMetrics(unitMetrics, it)
//
//            expressionResults = calculateUnitMetrics(expressionMetrics, it)
//        }
//
//        println("\n==== MODULE METRICS ====")
//        moduleResults.forEach { metric -> printModuleMetrics(metric.key, metric.value) }
//
//        println("\n==== UNIT METRICS ====")
//        unitResults.forEach { metric -> printUnitMetrics(metric.key, metric.value) }
//
//        println("\n==== EXPRESSION METRICS ====")
//        expressionResults.forEach { metric -> printUnitMetrics(metric.key, metric.value) }

        println("\n==== SEMANTIC TREE ====")
        SemanticTreeVisitor("Test", modules).getResult().print()

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

    private fun aggregateMultipleMetricOutput(
        metricOutput: Map<String, Pair<String, Int>>,
        results: MutableMap<String, MutableList<Pair<String, Int>>>
    ) {
        metricOutput.forEach { (k, v) ->
            results[k] = (results[k] ?: mutableListOf()).also { metrics ->
                metrics.add(v)
            }
        }
    }

    private fun calculateUnitMetrics(
        unitMetrics: List<UnitVisitor>,
        module: Module
    ): MutableMap<String, MutableList<Pair<String, Int>>> {
        val results = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        unitMetrics.map { evaluateUnitMetric(it, module) }.forEach { aggregateMultipleMetricOutput(it, results) }
        return results
    }

    private fun calculateModuleMetrics(
        moduleMetrics: List<ModuleVisitor>,
        module: Module
    ): MutableMap<String, MutableList<Pair<String, Int>>> {
        val results = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        moduleMetrics.map { evaluateModuleMetric(it, module) }
            .forEach { aggregateMultipleMetricOutput(it, results) }
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