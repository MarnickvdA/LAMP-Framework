package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class ParameterCount: UnitVisitor() {

    override fun getTag(): String {
        return "PC"
    }

    private var unitsToEvaluate: Set<Unit>? = null
    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        unitsToEvaluate = moduleRoot?.module?.members?.filterIsInstance<Unit>()?.toSet()
        super.visitModuleRoot(moduleRoot)
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null || unitsToEvaluate?.contains(unit) == false) return
        metricResults.add(Pair(unit.getUniqueName(moduleRoot), unit.parameters.size))
    }
}