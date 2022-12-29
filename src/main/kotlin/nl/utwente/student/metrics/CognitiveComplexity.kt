package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.utils.getUniqueName

class CognitiveComplexity : Metric<Int>() {
    override fun getTag(): String = "COCO"

    private val metricResults = mutableMapOf<String, Int>()
    private var module: Module? = null
    private var currentNestingLevel: Int = 0

    fun getMetricResults() = metricResults

    private fun logCount(expression: Expression, count: Int) {
//        println("+$count (nesting = $currentNestingLevel) for ${expression.context} on line ${expression.metadata.startLine}:${expression.metadata.endLine}")
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
        // Reset the current nesting level to calculate the whole
        this.currentNestingLevel = 0

        // Visit the body and register the complexity as a metric result
        metricResults[unit.identifier.getUniqueName(module)] = visitBlockScope(unit.body)

        return 0
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

        var count = (currentNestingLevel + 1)
        logCount(catch, count)

        currentNestingLevel++
        count += (visitBlockScope(catch.nestedScope))
        currentNestingLevel--

        return count
    }

    override fun visitConditional(conditional: Conditional?): Int {
        if (conditional == null) return 0

        // Increase the count for every if or ternary operator.
        var count = currentNestingLevel + 1
        logCount(conditional, count)

        // Nesting level increase for the entire if statement
        currentNestingLevel++

        count += visitExpression(conditional.ifExpr)

        conditional.elseIfExpr?.forEach {
            count += 1
            logCount(it, 1)
            count += visitExpression(it)
        }

        conditional.elseExpr?.let {
            count += 1
            logCount(it, 1)

            count += visitExpression(conditional.elseExpr)
        }

        // Decrease nesting level.
        currentNestingLevel--

        return count
    }

    override fun visitLoop(loop: Loop?): Int {
        return if (loop != null) {
            // Increment for any loop
            var count = (currentNestingLevel + 1)
            logCount(loop, count)

            count += (loop.evaluations.sumOf(this::visitExpression))

            currentNestingLevel++
            count += (visitBlockScope(loop.nestedScope))
            currentNestingLevel--

            count
        } else 0
    }

    override fun visitDeclaration(declaration: Declaration?): Int {
        return when (declaration?.value) {
            is Unit -> {
                currentNestingLevel++
                val count = (visitBlockScope((declaration.value as Unit).body))
                currentNestingLevel--

                count
            }

            is Property -> visitExpression((declaration.value as Property).value)
            else -> return 0
        }

    }

    override fun visitJump(jump: Jump?): Int {
        return if (jump?.label != null) 1 else 0
    }

    override fun visitReturnValue(returnValue: ReturnValue?): Int {
        return visitExpression(returnValue?.value)
    }

    override fun visitBinaryExpression(binaryExpression: BinaryExpression?): Int {
        return visitExpression(binaryExpression?.leftOperand) +
                visitExpression(binaryExpression?.rightOperand) +
                visitBlockScope(binaryExpression?.nestedScope)
    }

    override fun visitLambda(lambda: Lambda?): Int {
        return if (lambda != null) {
            currentNestingLevel++
            val count = (visitBlockScope(lambda.nestedScope))
            currentNestingLevel--

            count
        } else 0
    }

    override fun visitLogicalSequence(sequence: LogicalSequence?): Int {
        if (sequence == null) return 0

        var count = (1 + (sequence.operands?.sumOf(this::visitExpression) ?: 0))
        logCount(sequence, count)

        count += this.visitBlockScope(sequence.nestedScope)

        return count
    }

    override fun visitModuleScope(module: ModuleScope?): Int {
        // TODO Check if this ModuleScope should also increase the complexity?
        // According to the white paper: "Nested methods and method-like structures", which is not a module declaration.
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
            is Declaration -> this.visitDeclaration(expression)
            is BinaryExpression -> this.visitBinaryExpression(expression)
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