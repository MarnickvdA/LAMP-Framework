package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.Expression
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.metrics.UnitMetric

abstract class UnitVisitor: MetamodelVisitor<Unit>(), UnitMetric {

    abstract override fun getTag(): String

    protected var moduleRoot: ModuleRoot? = null
    protected var metricResults = mutableListOf<Pair<String, Int>>()

    override fun getResult(): List<Pair<String, Int>> {
        return metricResults
    }

    protected fun logCount(expression: Expression?, count: Int) {
//        println("+$count for ${expression?.context} on line ${expression?.metadata?.startLine}:${expression?.metadata?.endLine}")
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        this.moduleRoot = moduleRoot
        super.visitModuleRoot(moduleRoot)
    }

    open fun reset() {
        moduleRoot = null
        metricResults = mutableListOf()
    }
}