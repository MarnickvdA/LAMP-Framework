package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.LinesOfCodeVisitor
import nl.utwente.student.visitors.UnitVisitor

class UnitLinesOfCode : LinesOfCodeVisitor() {

    private var currentUnit: Unit? = null

    override fun getTag(): String = "LOC"

    override fun visitUnit(unit: Unit?) {
        if (unit == null) return

        val parentUnit = currentUnit
        currentUnit = unit

        val parentLines = curLinesCovered.toMutableSet()
        curLinesCovered = mutableSetOf() // Reset
        curLinesCovered.add(unit.metadata.startLine.toInt())
        curLinesCovered.add(unit.metadata.endLine.toInt())

        super.visitUnit(unit)

        metricResults.add(Pair(unit.getUniqueName(module), curLinesCovered.size))

        curLinesCovered.addAll(parentLines)
        currentUnit = parentUnit
    }
}