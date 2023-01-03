package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.Expression
import nl.utwente.student.metamodel.v2.Module

abstract class UnitVisitor: MetamodelVisitor<Unit, List<Pair<String, Int>>>() {

    abstract override fun getTag(): String

    protected var module: Module? = null
    protected val metricResults = mutableListOf<Pair<String, Int>>()

    override fun getResult(): List<Pair<String, Int>> {
        return metricResults
    }

    protected fun logCount(expression: Expression?, count: Int) {
//        println("+$count (nesting = $currentNestingLevel) for ${expression?.context} on line ${expression?.metadata?.startLine}:${expression?.metadata?.endLine}")
    }

    override fun visitModule(module: Module?) {
        this.module = module
        super.visitModule(module)
    }
}