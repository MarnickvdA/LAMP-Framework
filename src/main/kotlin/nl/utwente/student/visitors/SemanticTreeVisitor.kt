package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.models.semantics.*
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.utils.getUniquePosition

/**
 * Map all semantic objects correctly.
 * - UnitCall always references a Unit
 * - Access always references a Property
 * - Assignment always references a Property
 */
class SemanticTreeVisitor: MetamodelVisitor<kotlin.Unit>() {
    private lateinit var semanticTree: SemanticTree
    private lateinit var curSemanticElement: SemanticElement

    fun visitProject(modules: List<ModuleRoot>): SemanticTree {
        semanticTree = SemanticTree()
        curSemanticElement = semanticTree

        modules.forEach(this::visitModuleRoot)

        return semanticTree
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        if (moduleRoot == null) return

        if (moduleRoot.componentName != null) {
            curSemanticElement = semanticTree.add(SemanticComponent(moduleRoot.componentName))
        }

        this.visitModule(moduleRoot.module)
    }

    override fun visitModule(module: Module?) {
        if (module == null) return

        curSemanticElement = curSemanticElement.add(
            SemanticModule(
                module.getUniqueName(curSemanticElement.name),
                module,
                curSemanticElement
            )
        )
        super.visitModule(module)
        curSemanticElement = curSemanticElement.parent!!
    }

    override fun visitProperty(property: Property?) {
        if (property == null) return

        curSemanticElement = curSemanticElement.add(SemanticProperty(property, curSemanticElement))
        super.visitProperty(property)
        curSemanticElement = curSemanticElement.parent!!
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null) return

        curSemanticElement = curSemanticElement.add(SemanticUnit(unit, curSemanticElement))
        super.visitUnit(unit)
        curSemanticElement = curSemanticElement.parent!!
    }

    /**
     * Helper method to check if an expression, or its children contains at least 1 useful semantic object that we must
     * keep track of. Otherwise, we can get rid of the expression within our semantic model.
     */
    private fun containsSemanticInformation(expression: Expression?): Boolean {
        // Match by expressions that contain semantic information FOR SURE.
        when (expression) {
            null -> return false
            is Assignment, is UnitCall, is LocalDeclaration, is Identifier, is Catch -> return true
            is Lambda -> if (expression.parameters.isNotEmpty()) return true
            is SwitchCase -> if (containsSemanticInformation(expression.pattern)) return true
            is Switch -> if (containsSemanticInformation(expression.subject)) return true
            is Loop -> if (expression.evaluations.map { containsSemanticInformation(it) }.contains(true)) return true
            is Conditional -> {
                if (containsSemanticInformation(expression.ifExpr)) {
                    return true
                } else if (expression.elseIfExpr.map { containsSemanticInformation(it) }.contains(true)) {
                    return true
                } else if (containsSemanticInformation(expression.elseExpr)) {
                    return true
                }
            }

            is LogicalSequence -> if (expression.operands.map { containsSemanticInformation(it) }
                    .contains(true)) return true
        }

        return when {
            expression?.innerScope == null -> false
            expression.innerScope.isEmpty() -> false
            else -> expression.innerScope.map { containsSemanticInformation(it) }.contains(true)
        }
    }

    override fun visitExpression(expression: Expression?) {
        if (!containsSemanticInformation(expression)) return

        when (expression) {
            null -> return
            is UnitCall, // Do not introduce a scope for unit calls
            is Assignment, // Do not introduce a scope for assignments
            is LocalDeclaration, // Introduces its own scope
            is Identifier, // Do not introduce a scope for identifiers
            is LogicalSequence // Do not introduce a scope for logical sequence
            -> super.visitExpression(expression)

            else -> {
                // Unknown Expression, we have to introduce a scope.
                curSemanticElement = curSemanticElement.add(SemanticExpression(
                    "${expression.context}${getUniquePosition(expression)}",
                    expression,
                    curSemanticElement
                ))
                super.visitExpression(expression)
                curSemanticElement = curSemanticElement.parent!!
            }
        }
    }

    override fun visitUnitCall(unitCall: UnitCall?) {
        if (unitCall == null) return

        curSemanticElement = curSemanticElement.add(SemanticUnitCall(unitCall, curSemanticElement))
        unitCall.arguments?.forEach(this::visitExpression)
        this.visitInnerScope(unitCall.innerScope)
        curSemanticElement = curSemanticElement.parent!!
    }

    override fun visitAssignment(assignment: Assignment?) {
        if (assignment == null) return

        curSemanticElement = curSemanticElement.add(SemanticAssignment(assignment, curSemanticElement))
        super.visitAssignment(assignment)
        curSemanticElement = curSemanticElement.parent!!
    }

    override fun visitIdentifier(identifier: Identifier?) {
        if (identifier == null) return

        curSemanticElement.add(SemanticAccess(identifier, curSemanticElement))
    }
}