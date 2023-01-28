package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.LinesOfCodeVisitor

class UnitLinesOfCode : LinesOfCodeVisitor() {

    private var currentUnit: Unit? = null

    override fun getTag(): String = "ULOC"

    override fun visitUnit(unit: Unit?) {
        if (unit == null) return

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