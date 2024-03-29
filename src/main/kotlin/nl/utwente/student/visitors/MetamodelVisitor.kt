package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitor.BaseVisitor

abstract class MetamodelVisitor<T>: BaseVisitor<T, VisitorException>() {

    override fun visitExpression(expression: Expression?): T {
        if (expression == null) return super.visitExpression(expression)

        return when (expression) {
            is Loop -> this.visitLoop(expression)
            is Conditional -> this.visitConditional(expression)
            is LogicalSequence -> this.visitLogicalSequence(expression)
            is Jump -> this.visitJump(expression)
            is Lambda -> this.visitLambda(expression)
            is UnitCall -> this.visitUnitCall(expression)
            is ReferenceAccess -> this.visitReferenceAccess(expression)
            is Assignment -> this.visitAssignment(expression)
            is Catch -> this.visitCatch(expression)
            is Switch -> this.visitSwitch(expression)
            is SwitchCase -> this.visitSwitchCase(expression)
            else -> {
                this.visitInnerScope(expression.innerScope)
                super.visitExpression(expression)
            }
        }
    }

    protected fun visitInnerScope(innerScope: List<SourceElement?>?) {
        innerScope?.forEach {
            when(it) {
                is Expression -> this.visitExpression(it)
                is Unit -> visitUnit(it)
                is Property -> visitProperty(it)
                is Module -> visitModule(it)
            }
        }
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?): T {
        return this.visitModule(moduleRoot?.module)
    }

    override fun visitModule(module: Module?): T {
        module?.members?.forEach {
            when(it) {
                is Module -> this.visitModule(it)
                is Unit -> this.visitUnit(it)
                is Property -> this.visitProperty(it)
            }
        }

        return super.visitModule(module)
    }

    override fun visitUnit(unit: Unit?): T {
        unit?.parameters?.forEach(this::visitProperty)
        this.visitExpression(unit?.body)

        return super.visitUnit(unit)
    }

    override fun visitProperty(property: Property?): T {
        property?.initializer?.also { this.visitExpression(it) }

        return super.visitProperty(property)
    }


    override fun visitAssignment(assignment: Assignment?): T {
        this.visitExpression(assignment?.value)

        return super.visitAssignment(assignment)
    }

    override fun visitCatch(catch: Catch?): T {
        this.visitProperty(catch?.exception)
        this.visitInnerScope(catch?.innerScope)

        return super.visitCatch(catch)
    }

    override fun visitConditional(conditional: Conditional?): T {
        this.visitExpression(conditional?.ifExpr)
        conditional?.elseIfExpr?.forEach(this::visitExpression)
        this.visitExpression(conditional?.elseExpr)

        return super.visitConditional(conditional)
    }

    override fun visitLoop(loop: Loop?): T {
        loop?.evaluations?.forEach(this::visitExpression)
        this.visitInnerScope(loop?.innerScope)

        return super.visitLoop(loop)
    }

    override fun visitJump(jump: Jump?): T {
        return super.visitJump(jump)
    }

    override fun visitLambda(lambda: Lambda?): T {
        this.visitUnit(lambda?.unit)

        return super.visitLambda(lambda)
    }

    override fun visitLogicalSequence(sequence: LogicalSequence?): T {
        sequence?.operands?.forEach(this::visitExpression)
        this.visitInnerScope(sequence?.innerScope)

        return super.visitLogicalSequence(sequence)
    }

    override fun visitUnitCall(unitCall: UnitCall?): T {
        unitCall?.arguments?.forEach(this::visitExpression)
        this.visitInnerScope(unitCall?.innerScope)
        return super.visitUnitCall(unitCall)
    }

    override fun visitReferenceAccess(referenceAccess: ReferenceAccess?): T {
        this.visitInnerScope(referenceAccess?.innerScope)
        return super.visitReferenceAccess(referenceAccess)
    }

    override fun visitMetadata(metadata: Metadata?): T {
        return super.visitMetadata(metadata)
    }

    override fun visitSwitch(switch: Switch?): T {
        this.visitExpression(switch?.subject)
        switch?.cases?.forEach(this::visitExpression)

        return super.visitSwitch(switch)
    }

    override fun visitSwitchCase(switchCase: SwitchCase?): T {
        this.visitExpression(switchCase?.pattern)
        this.visitInnerScope(switchCase?.innerScope)

        return super.visitSwitchCase(switchCase)
    }
}