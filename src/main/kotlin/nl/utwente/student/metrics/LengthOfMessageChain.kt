package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Call
import nl.utwente.student.metamodel.v3.Expression
import nl.utwente.student.metamodel.v3.ReferenceCall
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.UnitVisitor

class LengthOfMessageChain: UnitVisitor() {

    override fun getTag(): String {
        return "LMC"
    }

    private var currentChain: Call? = null
    override fun visitUnitCall(unitCall: UnitCall?) {
        this.visitCall(unitCall)
    }

    override fun visitReferenceCall(referenceCall: ReferenceCall?) {
        this.visitReferenceCall(referenceCall)
    }

    private fun visitCall(call: Call?) {
        if (call == null) return

        if (currentChain == null) currentChain = call

        if (currentChain != call) return // only process the root of the call chain

        fun traverse(expression: Expression?): Int {
            return when(val child = expression?.innerScope?.firstOrNull()) {
                is Call -> traverse(child) + 1
                else -> 0
            }
        }

        if (currentChain == call) {
            metricResults.add(Pair(call.getUniqueName(moduleRoot), traverse(call)))
            currentChain = null
        }
    }
}