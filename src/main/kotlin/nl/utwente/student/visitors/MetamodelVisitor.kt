package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.visitor.BaseVisitor

abstract class MetamodelVisitor<T, R>: BaseVisitor<T, VisitorException>() {

    abstract fun getTag(): String

    abstract fun getResult(): R

    override fun visitExpression(expression: Expression?): T {
        return when (expression) {
            is Loop -> this.visitLoop(expression)
            is Conditional -> this.visitConditional(expression)
            is LogicalSequence -> this.visitLogicalSequence(expression)
            is Jump -> this.visitJump(expression)
            is Declaration -> this.visitDeclaration(expression)
            is Assignment -> this.visitAssignment(expression)
            is Lambda -> this.visitLambda(expression)
            is UnitCall -> this.visitUnitCall(expression)
            is Catch -> this.visitCatch(expression)
            else -> this.visitBlockScope(expression?.nestedScope)
        }
    }

    override fun visitModule(module: Module?): T {
        return this.visitModuleScope(module?.moduleScope)
    }

    override fun visitUnit(unit: Unit?): T {
        this.visitBlockScope(unit?.body)

        return super.visitUnit(unit)
    }

    override fun visitAssignment(assignment: Assignment?): T {
        this.visitExpression(assignment?.reference)
        this.visitBlockScope(assignment?.nestedScope)

        return super.visitAssignment(assignment)
    }

    override fun visitCatch(catch: Catch?): T {
        this.visitBlockScope(catch?.nestedScope)

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
        this.visitBlockScope(loop?.nestedScope)

        return super.visitLoop(loop)
    }

    override fun visitDeclaration(declaration: Declaration?): T {
        return when (declaration?.value) {
            is Unit -> visitBlockScope((declaration.value as Unit).body)
            is Property -> visitExpression((declaration.value as Property).value)
            is ModuleScope -> visitModuleScope(declaration.value as ModuleScope)
            else -> super.visitDeclaration(declaration)
        }
    }

    override fun visitJump(jump: Jump?): T {
        return super.visitJump(jump)
    }

    override fun visitLambda(lambda: Lambda?): T {
        this.visitBlockScope(lambda?.nestedScope)

        return super.visitLambda(lambda)
    }

    override fun visitLogicalSequence(sequence: LogicalSequence?): T {
        sequence?.operands?.forEach(this::visitExpression)
        this.visitBlockScope(sequence?.nestedScope)

        return super.visitLogicalSequence(sequence)
    }

    override fun visitModuleScope(module: ModuleScope?): T {
        module?.members?.forEach(this::visitScope)

        return super.visitModuleScope(module)
    }

    override fun visitProperty(property: Property?): T {
        this.visitExpression(property?.value)

        return super.visitProperty(property)
    }

    override fun visitUnitCall(unitCall: UnitCall?): T {
        unitCall?.arguments?.forEach(this::visitExpression)
        this.visitBlockScope(unitCall?.nestedScope)

        return super.visitUnitCall(unitCall)
    }

    override fun visitBlockScope(scope: BlockScope?): T {
        scope?.expressions?.forEach(this::visitExpression)

        return super.visitBlockScope(scope)
    }

    override fun visitIdentifier(identifier: Identifier?): T {
        return super.visitIdentifier(identifier)
    }

    override fun visitMetadata(metadata: Metadata?): T {
        return super.visitMetadata(metadata)
    }

    override fun visitScope(scope: Scope?): T {
        return when (scope) {
            is ModuleScope -> this.visitModuleScope(scope)
            is Unit -> this.visitUnit(scope)
            is Property -> this.visitProperty(scope)
            is BlockScope -> this.visitBlockScope(scope)
            else -> super.visitScope(scope)
        }
    }
}