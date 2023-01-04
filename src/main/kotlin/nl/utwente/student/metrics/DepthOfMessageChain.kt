package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.Expression
import nl.utwente.student.metamodel.v2.Identifier
import nl.utwente.student.metamodel.v2.UnitCall
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.UnitVisitor

class DepthOfMessageChain: UnitVisitor() {

    override fun getTag(): String {
        return "DOC"
    }

    private var currentChain: UnitCall? = null
    override fun visitUnitCall(unitCall: UnitCall?) {
        if (unitCall == null) return

        if (currentChain == null) currentChain = unitCall

        if (currentChain != unitCall) return // only process the root of the call chain

        fun traverse(expression: Expression?): Int {
            return when(val child = expression?.nestedScope?.expressions?.firstOrNull()) {
                is UnitCall -> traverse(child) + 1
                is Identifier -> 1
                else -> 0
            }
        }

        if (currentChain == unitCall) {
            metricResults.add(Pair(unitCall.getUniqueName(module), traverse(unitCall)))
            currentChain = null
        }
    }
}