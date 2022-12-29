package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.utils.getUniqueName

/**
 * Increment for:
 * Unit
 * If
 * Loop
 * Switch
 * Catch Clause
 * Binary Expression (including LogicalSequence -> count of operands minus 1)
 * Lambda Expression
 * LabeledStatement
 * Jump (continue, break)
 * ReturnValue (throw, return, yield)
 */
class CyclomaticComplexity : Metric<Int>() {
    override fun getTag(): String = "CC"

    private val metricResults = mutableMapOf<String, Int>()
    private var module: Module? = null

    fun getMetricResults() = metricResults

    private fun logCount(expression: Expression, count: Int) {
        println("+$count for ${expression::class.java.simpleName} (${expression.context}) on line ${expression.metadata.startLine}:${expression.metadata.endLine}")
    }


    override fun visitModule(module: Module?): Int {
        this.module = module
        return this.visitModuleScope(module?.moduleScope)
    }

    override fun visitUnit(unit: Unit?): Int {
        if (unit == null) {
            System.err.println("Visiting Unit, but it's null.")
            return 0
        }

        println("\nEvaluating CC for ${unit.identifier.value}")

        // Visit the body and register the complexity as a metric result
        logCount(Expression().also {
            it.context = "Unit"
            it.metadata = unit.metadata
        }, 1)
        metricResults[unit.identifier.getUniqueName(module)] = 1 + visitBlockScope(unit.body)

        return 1
    }

    override fun visitAssignment(assignment: Assignment?): Int {
        return if (assignment != null) {
            var count = (visitExpression(assignment.reference))

            count += (visitBlockScope(assignment.nestedScope))

            count
        } else 0
    }

    override fun visitCatch(catch: Catch?): Int {
        if (catch == null) return 0

        var count = 1
        logCount(catch, count)

        count += (visitBlockScope(catch.nestedScope))

        return count
    }

    override fun visitConditional(conditional: Conditional?): Int {
        if (conditional == null) return 0

        var count = 1
        logCount(conditional, count)

        count += visitExpression(conditional.ifExpr)

        conditional.elseIfExpr?.forEach {
            count += visitExpression(it)
        }

        conditional.elseExpr?.let {
            count += visitExpression(conditional.elseExpr)
        }

        return count
    }

    override fun visitLoop(loop: Loop?): Int {
        return if (loop != null) {
            // Increment for any loop
            var count = 1
            logCount(loop, count)
            count += (loop.evaluations.sumOf(this::visitExpression))
            count += (visitBlockScope(loop.nestedScope))
            count
        } else 0
    }

    override fun visitDeclaration(declaration: Declaration?): Int {
        return when (declaration?.value) {
            is Unit -> visitBlockScope((declaration.value as Unit).body)
            is Property -> visitExpression((declaration.value as Property).value)
            is ModuleScope -> visitModuleScope(declaration.value as ModuleScope)
            else -> return 0
        }

    }

    override fun visitJump(jump: Jump?): Int {
        return (1).also { logCount(jump!!, 1) }
    }

    override fun visitReturnValue(returnValue: ReturnValue?): Int {
        return (1 + visitExpression(returnValue?.value)).also {
            logCount(returnValue!!, 1)
        }
    }

    override fun visitLabeledExpression(labeledExpression: LabeledExpression?): Int {
        return (1 + visitBlockScope(labeledExpression?.nestedScope)).also { logCount(labeledExpression!!, 1) }
    }

    override fun visitBinaryExpression(binaryExpression: BinaryExpression?): Int {
        return (1 +
                visitExpression(binaryExpression?.leftOperand) +
                visitExpression(binaryExpression?.rightOperand) +
                visitBlockScope(binaryExpression?.nestedScope))
            .also { logCount(binaryExpression!!, 1) }
    }

    override fun visitLambda(lambda: Lambda?): Int {
        return if (lambda != null) {
            var count = 1
            logCount(lambda, count)
            count += visitBlockScope(lambda.nestedScope)
            count
        } else 0
    }

    override fun visitLogicalSequence(sequence: LogicalSequence?): Int {
        if (sequence == null) return 0

        var count = sequence.operands.size - 1
        logCount(sequence, count)

        count += sequence.operands.sumOf(this::visitExpression)
        count += this.visitBlockScope(sequence.nestedScope)

        return count
    }

    override fun visitModuleScope(module: ModuleScope?): Int {
        return (module?.members?.sumOf { this.visitScope(it) } ?: 0)
    }

    override fun visitProperty(property: Property?): Int {
        return if (property != null) visitExpression(property.value) else 0
    }

    override fun visitUnitCall(unitCall: UnitCall?): Int {
        return if (unitCall != null) {
            var count = (unitCall.arguments?.sumOf(this::visitExpression) ?: 0)
            count += (this.visitBlockScope(unitCall.nestedScope))
            count
        } else 0
    }

    override fun visitBlockScope(scope: BlockScope?): Int {
        return (scope?.expressions?.sumOf {
            this.visitExpression(it)//.also { c -> if (c > 0) println("${it.context}: $c") }
        } ?: 0)
    }

    override fun visitExpression(expression: Expression?): Int {
        return when (expression) {
            is Loop -> this.visitLoop(expression)
            is Conditional -> this.visitConditional(expression)
            is LogicalSequence -> this.visitLogicalSequence(expression)
            is Jump -> this.visitJump(expression)
            is ReturnValue -> this.visitReturnValue(expression)
            is BinaryExpression -> this.visitBinaryExpression(expression)
            is LabeledExpression -> this.visitLabeledExpression(expression)
            is Declaration -> this.visitDeclaration(expression)
            is Assignment -> this.visitAssignment(expression)
            is Lambda -> this.visitLambda(expression)
            is UnitCall -> this.visitUnitCall(expression)
            is Catch -> this.visitCatch(expression)
            else -> this.visitBlockScope(expression?.nestedScope)
        }
    }

    override fun visitIdentifier(aBean: Identifier?): Int {
        return 0
    }

    override fun visitMetadata(aBean: Metadata?): Int {
        return 0
    }

    override fun visitScope(scope: Scope?): Int {
        return when (scope) {
            is ModuleScope -> this.visitModuleScope(scope)
            is Unit -> this.visitUnit(scope)
            is Property -> this.visitProperty(scope)
            is BlockScope -> this.visitBlockScope(scope)
            else -> 0
        }
    }
}