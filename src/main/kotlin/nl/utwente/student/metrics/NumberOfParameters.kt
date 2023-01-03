package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.Lambda
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class NumberOfParameters: UnitVisitor() {

    override fun getTag(): String {
        return "NOP"
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null || module == null) return
        metricResults.add(Pair(unit.getUniqueName(module), unit.parameters.size))
    }

    override fun visitLambda(lambda: Lambda?) {
        if (lambda == null || module == null) return

        metricResults.add(Pair(lambda.getUniqueName(module), lambda.parameters.size))
    }
}