package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class CognitiveComplexity : UnitVisitor() {
    override fun getTag(): String = "COCO"

    private var currentNestingLevel = 0
    private var currentComplexity = 0

    override fun visitUnit(unit: Unit?) {
        // Reset the current nesting level to calculate the whole
        this.currentNestingLevel = 0
        this.currentComplexity = 0

        // Traverse the children of unit
        super.visitUnit(unit)

        if (unit != null) {
            metricResults.add(Pair(unit.getUniqueName(moduleRoot), this.currentComplexity))
        }
    }

    override fun visitCatch(catch: Catch?) {
        currentComplexity += currentNestingLevel + 1
        logCount(catch, currentNestingLevel + 1)

        currentNestingLevel++
        super.visitCatch(catch)
        currentNestingLevel--
    }

    override fun visitConditional(conditional: Conditional?) {
        // Increase the count for every if or ternary operator.
        currentComplexity += currentNestingLevel + 1
        logCount(conditional, currentNestingLevel + 1)

        // Nesting level increase for the entire if statement
        currentNestingLevel++
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

        currentNestingLevel--
    }

    override fun visitSwitch(switch: Switch?) {
        if (switch == null) return

        currentComplexity += 1

        // TODO Document that we count the subject outside of the nesting level of switch, this is not clear in the whitepaper of Sonarsource.
        this.visitExpression(switch.subject)

        currentNestingLevel++
        switch.cases?.forEach(this::visitSwitchCase)
        currentNestingLevel--
    }

    override fun visitLoop(loop: Loop?) {
        currentComplexity += currentNestingLevel + 1
        logCount(loop, currentNestingLevel + 1)

        loop?.evaluations?.forEach(this::visitExpression)

        currentNestingLevel++
        this.visitInnerScope(loop?.innerScope)
        currentNestingLevel--
    }

    override fun visitLocalDeclaration(declaration: LocalDeclaration?) {
        when (val d = declaration?.declaration) {
            is Unit -> {
                currentNestingLevel++
                this.visitUnit(d) // TODO(Document: whitepaper touches upon decorator functions, we do not detect them here. (because we do not use Python.)
                currentNestingLevel--
            }
            else -> super.visitLocalDeclaration(declaration)
        }
    }

    override fun visitJump(jump: Jump?) {
        if (jump?.label != null) {
            currentComplexity += 1
            logCount(jump, 1)
        }
    }

    override fun visitLambda(lambda: Lambda?) {
        currentNestingLevel++
        super.visitLambda(lambda)
        currentNestingLevel--
    }

    override fun visitLogicalSequence(sequence: LogicalSequence?) {
        currentComplexity += 1
        logCount(sequence, 1)
        super.visitLogicalSequence(sequence)
    }
}