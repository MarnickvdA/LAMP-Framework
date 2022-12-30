package nl.utwente.student.metrics.generic

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.metrics.Metric
import nl.utwente.student.utils.getUniqueName

/**
 * Increment for:
 * Unit
 * If
 * Loop (for, foreach, while, do while)
 * Switch (TODO: create a 'Switch' Expression. Increment for cases, fallthrough cases and default is currently not supported)
 * Catch Clause
 * Logical Sequence
 * ReturnValue (throw, return, yield)
 *
 * TODO(evaluate CC for getWords (CC=2, expected 5), toRegexp(CC=15, excepted 12)
 */
class CyclomaticComplexity : Metric<kotlin.Unit>() {
    override fun getTag(): String = "CC"

    private val metricResults = mutableMapOf<String, Int>()
    private var module: Module? = null
    private var currentComplexity = 0

    fun getMetricResults() = metricResults

    private fun logCount(expression: Expression?, count: Int) {
//        println("+$count for ${expression::class.java.simpleName} (${expression.context}) on line ${expression.metadata.startLine}:${expression.metadata.endLine}")
    }

    override fun visitModule(module: Module?) {
        this.module = module
        super.visitModule(module)
    }

    override fun visitUnit(unit: Unit?) {
        // Visit the body and register the complexity as a metric result
        logCount(unit?.let { Expression() }?.also {
            it.context = "Unit"
            it.metadata = unit.metadata
        }, 1)

        currentComplexity = 1

        super.visitUnit(unit)

        if (unit != null) {
            metricResults[unit.identifier.getUniqueName(module)] = currentComplexity
        }
    }

    override fun visitCatch(catch: Catch?) {
        currentComplexity++
        logCount(catch, 1)

        super.visitCatch(catch)
    }

    override fun visitConditional(conditional: Conditional?) {
        currentComplexity++
        logCount(conditional, 1)

        this.visitExpression(conditional?.ifExpr)

        conditional?.elseIfExpr?.forEach {
            currentComplexity += 1
            logCount(it, 1)
            this.visitExpression(it)
        }

        conditional?.elseExpr?.let {
            currentComplexity += 1
            logCount(it, 1)
            this.visitExpression(it)
        }
    }

    override fun visitLoop(loop: Loop?) {
        currentComplexity++
        logCount(loop, 1)

        super.visitLoop(loop)
    }

    override fun visitLogicalSequence(sequence: LogicalSequence?) {
        if (sequence == null) return

        currentComplexity += sequence.operands.size - 1
        logCount(sequence, sequence.operands.size - 1)

        super.visitLogicalSequence(sequence)
    }
}