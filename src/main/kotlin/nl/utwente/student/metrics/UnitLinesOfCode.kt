package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Expression
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.UnitVisitor

class UnitLinesOfCode : UnitVisitor() {
    private var curLinesCovered = mutableSetOf<Int>()
    private var currentUnit: Unit? = null
    private var unitsToEvaluate: Set<Unit>? = null

    override fun getTag(): String = "ULOC"

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        unitsToEvaluate = moduleRoot?.module?.members?.filterIsInstance<Unit>()?.toSet()
        super.visitModuleRoot(moduleRoot)
    }

    override fun visitExpression(expression: Expression?) {
        if (expression == null)
            return

        curLinesCovered.add(expression.metadata.startLine.toInt())
        curLinesCovered.add(expression.metadata.endLine.toInt())

        super.visitExpression(expression)
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null || unitsToEvaluate?.contains(unit) == false) return

        val parentUnit = currentUnit
        currentUnit = unit

        val parentLines = curLinesCovered.toMutableSet()
        curLinesCovered = mutableSetOf() // Reset
        curLinesCovered.add(unit.metadata.startLine.toInt())
        curLinesCovered.add(unit.metadata.endLine.toInt())

        super.visitUnit(unit)

        if (!unit.id.startsWith("Lambda"))
            metricResults.add(Pair(unit.getUniqueName(moduleRoot), curLinesCovered.size))

        curLinesCovered.addAll(parentLines)
        currentUnit = parentUnit
    }
}