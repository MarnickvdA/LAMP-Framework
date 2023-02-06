package nl.utwente.student.io

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metrics.*
import nl.utwente.student.models.symbol.SymbolTree
import nl.utwente.student.models.metrics.*
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.SymbolVisitor
import nl.utwente.student.visitors.UnitVisitor
import java.io.File

typealias MetricResult = MutableMap<String, Pair<String, Int>>
typealias MetricResults = MutableMap<String, MutableList<Pair<String, Int>>>

object MetricsEngine {

    private fun getMetrics(): List<Metric<*>> {
        return listOf(
            // Module metrics
            WeightedMethodPerClass(),
            CognitivelyWeightedMethodPerClass(),
            LambdaCount(),
            ResponseForAClass(),
            LackOfCohesionInMethods(),
            ModuleLinesOfCode(),
            NumberOfUnits(),

            // Symbolic relationship metrics
            DepthOfInheritanceTree(),
            NumberOfChildren(),
            CouplingBetweenObjectClasses(),

            // Unit metrics
            CyclomaticComplexity(),
            CognitiveComplexity(),
            ParameterCount(),
            UnitLinesOfCode(),
            LambdaLinesOfCode(),
        )
    }

    fun MetricResults.add(result: Map.Entry<String, MutableList<Pair<String, Int>>>) {
        if (this[result.key] == null)
            this[result.key] = result.value.toMutableList()
        else this[result.key]?.addAll(result.value)
    }

    fun run(modules: List<ModuleRoot>, output: File?): File {
        val metrics = getMetrics()

        val moduleResults: MetricResults = mutableMapOf()
        var unitResults: MetricResults = mutableMapOf()

        calculateMetrics(
            metrics.filterIsInstance<SymbolMetric>(),
            modules = modules,
            symbolTree = SymbolVisitor().visitProject(modules)
        ).forEach { moduleResults.add(it) }

        // Calculate the module specific metrics
        modules.filter { it.module.members.isNotEmpty() }.forEach {
            // Calculate metrics for modules
            calculateMetrics(metrics.filterIsInstance<ModuleMetric>(), it).forEach { entry -> moduleResults.add(entry) }

            // Calculate unit metrics
            unitResults = calculateMetrics(metrics.filterIsInstance<UnitMetric>(), it)
        }

        if (output == null) {
            println("\n==== MODULE METRICS ====")
            moduleResults.forEach { metric -> printModuleMetrics(metric.key, metric.value) }

            println("\n==== SOURCE ELEMENT METRICS ====")
            unitResults.forEach { metric -> printUnitMetrics(metric.key, metric.value) }
        } else {
            WriterEngine.writeToCSV(output, moduleResults, unitResults)
        }

        return output!!
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

    private fun calculateMetrics(
        metrics: List<Metric<*>>,
        moduleRoot: ModuleRoot? = null,
        symbolTree: SymbolTree? = null,
        modules: List<ModuleRoot>? = null,
    ): MetricResults {
        val results: MetricResults = mutableMapOf()
        metrics.mapNotNull {
            when (it) {
                is UnitVisitor -> evaluateUnitMetric(it, moduleRoot!!)
                is ModuleVisitor -> evaluateModuleMetric(it, moduleRoot!!)
                is SymbolMetric -> evaluateSymbolMetric(it, modules!!, symbolTree!!)
                else -> null
            }
        }.forEach { aggregateMultipleMetricOutput(it, results) }
        return results
    }

    private fun evaluateUnitMetric(metric: UnitVisitor, moduleRoot: ModuleRoot): MetricResult {
        val results: MetricResult = mutableMapOf()

        // Calculate the metric
        metric.visitModuleRoot(moduleRoot)

        // Put the results in a format grouped by unitId
        metric.getResult().forEach { (unitId, result) ->
            if (results[unitId] != null) System.err.println("We are overriding ${metric.getTag()} for $unitId!")
            results[unitId] = Pair(metric.getTag(), result)
        }

        return results
    }

    private fun evaluateModuleMetric(metric: ModuleVisitor, moduleRoot: ModuleRoot): MetricResult {
        val results: MetricResult = mutableMapOf()

        // Calculate the metric
        metric.visitModuleRoot(moduleRoot)

        // Put the results in a format grouped by moduleId
        metric.getResult().also { (moduleId, result) ->
            if (results[moduleId] != null) System.err.println("We are overriding ${metric.getTag()} for $moduleId!")
            results[moduleId] = Pair(metric.getTag(), result)
        }

        return results
    }

    private fun evaluateSymbolMetric(
        metric: SymbolMetric,
        modules: List<ModuleRoot>,
        symbolTree: SymbolTree
    ): MetricResult {
        val results: MetricResult = mutableMapOf()

        // Calculate the metric
        metric.visitProject(modules, symbolTree)

        // Put the results in a format grouped by moduleId
        metric.getResult().forEach {
            results[it.first] = Pair(metric.getTag(), it.second)
        }

        return results
    }
}