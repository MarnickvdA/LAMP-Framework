package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.*

abstract class LinesOfCodeVisitor : UnitVisitor() {

    protected var curLinesCovered = mutableSetOf<Int>()

    override fun getTag(): String = "LOC"

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