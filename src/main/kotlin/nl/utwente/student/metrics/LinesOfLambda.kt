package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Lambda
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.LinesOfCodeVisitor

class LinesOfLambda: LinesOfCodeVisitor() {

    private var currentLambda: Lambda? = null
    override fun getTag(): String = "LOL"

    override fun visitLambda(lambda: Lambda?) {
        if (lambda == null) return

        val parentLambda = currentLambda
        currentLambda = lambda

        val parentLines = curLinesCovered.toMutableSet()
        curLinesCovered = mutableSetOf() // Reset
        curLinesCovered.add(lambda.metadata.startLine.toInt())
        curLinesCovered.add(lambda.metadata.endLine.toInt())

        super.visitLambda(lambda)

        metricResults.add(Pair(lambda.getUniqueName(moduleRoot), curLinesCovered.size))

        curLinesCovered.addAll(parentLines)
        currentLambda = parentLambda
    }
}