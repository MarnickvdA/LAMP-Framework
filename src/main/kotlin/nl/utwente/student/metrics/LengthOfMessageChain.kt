package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Expression
import nl.utwente.student.metamodel.v3.Identifier
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.UnitVisitor

class LengthOfMessageChain: UnitVisitor() {

    override fun getTag(): String {
        return "LMC"
    }

    private var currentChain: UnitCall? = null
    override fun visitUnitCall(unitCall: UnitCall?) {
        if (unitCall == null) return

        if (currentChain == null) currentChain = unitCall

        if (currentChain != unitCall) return // only process the root of the call chain

        fun traverse(expression: Expression?): Int {
            return when(val child = expression?.innerScope?.firstOrNull()) {
                is UnitCall -> traverse(child) + 1
                is Identifier -> 1
                else -> 0
            }
        }

        if (currentChain == unitCall) {
            metricResults.add(Pair(unitCall.getUniqueName(moduleRoot), traverse(unitCall)))
            currentChain = null
        }
    }
}