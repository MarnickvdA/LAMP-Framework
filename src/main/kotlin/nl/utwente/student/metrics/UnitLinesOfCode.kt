package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.UnitVisitor

class UnitLinesOfCode : UnitVisitor() {

    private var curLinesCovered = mutableSetOf<Int>()
    private var currentUnit: Unit? = null
    private val linesPerUnit = mutableListOf<Pair<String, Int>>()

    override fun getTag(): String = "LOC"

    override fun getResult(): List<Pair<String, Int>> {
        return linesPerUnit
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null) return

        val parentUnit = currentUnit
        currentUnit = unit

        val parentLines = curLinesCovered.toMutableSet()
        curLinesCovered = mutableSetOf() // Reset
        curLinesCovered.add(unit.metadata.startLine.toInt())
        curLinesCovered.add(unit.metadata.endLine.toInt())

        super.visitUnit(unit)

        linesPerUnit.add(Pair(unit.getUniqueName(module), curLinesCovered.size))

        curLinesCovered.addAll(parentLines)
        currentUnit = parentUnit
    }

    override fun visitExpression(expression: Expression?) {
        if (expression == null) return

        curLinesCovered.add(expression.metadata.startLine.toInt())
        curLinesCovered.add(expression.metadata.endLine.toInt())

        // TODO(Document: if expression has empty body on a new line, this wont be added to the sum of lines.
        // TODO(Document: See todo above, therefore, the LOC is a (very) close approximation.
        when (expression) {
            is Conditional -> this.visitConditional(expression)
            is Loop -> this.visitLoop(expression)
            is LogicalSequence -> this.visitLogicalSequence(expression)
            is Jump -> this.visitJump(expression)
            is Declaration -> this.visitDeclaration(expression)
            is Assignment -> this.visitAssignment(expression)
            is Lambda -> this.visitLambda(expression)
            is UnitCall -> this.visitUnitCall(expression)
            is Catch -> this.visitCatch(expression)
            else -> if (expression.nestedScope == null) {
                curLinesCovered.addAll(expression.metadata.startLine.toInt()..expression.metadata.endLine.toInt())
            } else {
                curLinesCovered.add(expression.metadata.startLine.toInt())
                curLinesCovered.add(expression.metadata.endLine.toInt())
                this.visitBlockScope(expression.nestedScope)
            }
        }
    }
}