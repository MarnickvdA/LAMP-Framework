package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class LinesOfLambda: UnitVisitor() {

    private var linesCovered = mutableSetOf<Int>()
    private var inLambda = false
    override fun getTag(): String = "LOL"

    override fun visitLambda(lambda: Lambda?) {
        val alreadyInLambda = inLambda

        inLambda = true
        super.visitLambda(lambda)

        // TODO Document that if we are in an inner lambda, we do not trigger the inLambda shutoff.
        if (!alreadyInLambda) {
            inLambda = false

            metricResults.add(Pair(
                lambda!!.getUniqueName(module),
                linesCovered.size
            ))

            linesCovered = mutableSetOf()
        }
    }

    override fun visitExpression(expression: Expression?) {
        if (expression == null || !inLambda) {
            super.visitExpression(expression)
            return
        }

        if (expression.nestedScope == null) {
            linesCovered.addAll(expression.metadata.startLine.toInt()..expression.metadata.endLine.toInt())
        } else {
            linesCovered.add(expression.metadata.startLine.toInt())
            super.visitExpression(expression)
            linesCovered.add(expression.metadata.endLine.toInt())
        }
    }
}