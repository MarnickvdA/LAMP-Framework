package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

/**
 * Increment for:
 * Unit
 * If
 * Loop (for, foreach, while, do while)
 * Switch (TODO: add support for fallthrough cases)
 * Catch Clause
 * Logical Sequence
 * ReturnValue (throw, return, yield)
 *
 * TODO(evaluate CC for getWords (CC=2, expected 5), toRegexp(CC=15, excepted 12)
 * TODO(Document: we do not count for 'default' case, because this is not part of the official McCabe CC.)
 *
 */
class CyclomaticComplexity: UnitVisitor() {
    override fun getTag(): String = "CC"

    private var currentComplexity = 0

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        this.moduleRoot = moduleRoot

        moduleRoot?.module?.members?.filterIsInstance<Unit>()?.forEach {
            // Reset the current nesting level to calculate the whole
            logCount(it.let { Expression() }.also {e ->
                e.context = "Unit"
                e.metadata = it.metadata
            }, 1)

            this.currentComplexity = 1

            this.visitUnit(it)

            metricResults.add(Pair(it.getUniqueName(moduleRoot), this.currentComplexity))
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

    override fun visitSwitchCase(switchCase: SwitchCase?) {
        if (switchCase != null && switchCase.pattern != null) {
            currentComplexity++
            logCount(switchCase, 1)
        }

        super.visitSwitchCase(switchCase)
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