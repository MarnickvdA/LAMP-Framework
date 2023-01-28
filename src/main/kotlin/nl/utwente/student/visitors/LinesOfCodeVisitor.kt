package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.*

abstract class LinesOfCodeVisitor : UnitVisitor() {

    protected var curLinesCovered = mutableSetOf<Int>()

    override fun getTag(): String = "LOC"

    override fun visitExpression(expression: Expression?) {
        if (expression == null) return

        curLinesCovered.add(expression.metadata.startLine.toInt())
        curLinesCovered.add(expression.metadata.endLine.toInt())

        // TODO(Document: The LOC is a (very) close approximation. BinaryExpression operators aren't saved in the metamodel, thus if they are on an independent line, they wont be counted towards a LOC.

        super.visitExpression(expression)
    }
}