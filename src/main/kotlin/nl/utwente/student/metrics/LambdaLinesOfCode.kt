package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Expression
import nl.utwente.student.metamodel.v3.Lambda
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.UnitVisitor

class LambdaLinesOfCode: UnitVisitor() {
    private var curLinesCovered = mutableSetOf<Int>()
    private var currentLambda: Lambda? = null
    override fun getTag(): String = "LLOC"

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        this.moduleRoot = moduleRoot

        moduleRoot?.module?.members?.filterIsInstance<Unit>()?.forEach {
            curLinesCovered = mutableSetOf()
            this.visitUnit(it)
            metricResults.add(Pair(it.getUniqueName(moduleRoot), curLinesCovered.size))
        }
    }

    override fun visitExpression(expression: Expression?) {
        if (expression == null)
            return

        if (currentLambda != null) {
            curLinesCovered.add(expression.metadata.startLine.toInt())
            curLinesCovered.add(expression.metadata.endLine.toInt())
        }

        super.visitExpression(expression)
    }

    override fun visitLambda(lambda: Lambda?) {
        if (lambda == null) return

        val parentLambda = currentLambda
        currentLambda = lambda

        val parentLines = curLinesCovered.toMutableSet()
        curLinesCovered = mutableSetOf() // Reset
        curLinesCovered.add(lambda.metadata.startLine.toInt())
        curLinesCovered.add(lambda.metadata.endLine.toInt())

        super.visitLambda(lambda)

        curLinesCovered.addAll(parentLines)
        currentLambda = parentLambda
    }
}