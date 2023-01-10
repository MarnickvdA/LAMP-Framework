package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Lambda
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class NumberOfParameters: UnitVisitor() {

    override fun getTag(): String {
        return "NOP"
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null || moduleRoot == null) return
        metricResults.add(Pair(unit.getUniqueName(moduleRoot), unit.parameters.size))
    }

    override fun visitLambda(lambda: Lambda?) {
        if (lambda == null || moduleRoot == null) return

        metricResults.add(Pair(lambda.getUniqueName(moduleRoot), lambda.parameters.size))
    }
}