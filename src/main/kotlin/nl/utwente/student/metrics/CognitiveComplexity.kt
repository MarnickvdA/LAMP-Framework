package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.visitors.UnitVisitor
import nl.utwente.student.utils.getUniqueName

class CognitiveComplexity : UnitVisitor() {
    override fun getTag(): String = "COCO"

    private var currentNestingLevel = 0
    private var currentComplexity = 0

    override fun visitModule(module: Module?) {
        this.module = module
        this.visitModuleScope(module?.moduleScope)
    }

    override fun visitUnit(unit: Unit?) {
        // Reset the current nesting level to calculate the whole
        this.currentNestingLevel = 0
        this.currentComplexity = 0

        // Traverse the children of unit
        super.visitUnit(unit)

        if (unit != null) {
            metricResults.add(Pair(unit.getUniqueName(module), this.currentComplexity))
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

    override fun visitLoop(loop: Loop?) {
        currentComplexity += currentNestingLevel + 1
        logCount(loop, currentNestingLevel + 1)

        loop?.evaluations?.forEach(this::visitExpression)

        currentNestingLevel++
        visitBlockScope(loop?.nestedScope)
        currentNestingLevel--
    }

    override fun visitDeclaration(declaration: Declaration?) {
        when (declaration?.value) {
            is Unit -> {
                currentNestingLevel++
                visitBlockScope((declaration.value as Unit).body)
                currentNestingLevel--
            }
            else -> super.visitDeclaration(declaration)
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