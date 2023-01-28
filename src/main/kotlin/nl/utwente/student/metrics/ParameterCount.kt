package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class ParameterCount: UnitVisitor() {

    override fun getTag(): String {
        return "PC"
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null || moduleRoot == null) return
        metricResults.add(Pair(unit.getUniqueName(moduleRoot), unit.parameters.size))
    }
}