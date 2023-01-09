package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.models.semantics.*
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.utils.getUniquePosition

class SemanticTreeVisitor(
    private val projectName: String, private val modules: List<Module>
) : MetamodelVisitor<kotlin.Unit, SemanticTree>() {

    private lateinit var semanticTree: SemanticTree
    private val unitCallsToEvaluate = mutableListOf<Triple<UnitCall, Module, SemanticElement>>()

    private var currentModule: Module? = null
    private var currentScope: Scope? = null
    private var currentSemanticScope: SemanticElement? = null

    override fun getTag(): String = "SemanticTree"

    override fun getResult(): SemanticTree {
        semanticTree = SemanticTree(projectName)

        modules.forEach(this::visitModule)

        unitCallsToEvaluate.forEach {
            it.third.add(
                SemanticUnitCall(
                    name = it.first.getUniqueName(it.second), // TODO Handle dependencies
                    parent = it.third,
                )
            )
        }

        return semanticTree
    }

    override fun visitModule(module: Module?) {
        if (module == null) return

        if (module.componentName != null) {
            currentSemanticScope = semanticTree.addComponent(SemanticComponent(module.componentName))
        }

        currentModule = module
        this.visitModuleScope(module.moduleScope)
    }

    override fun visitModuleScope(module: ModuleScope?) {
        if (module == null) return

        val parentSemanticScope = currentSemanticScope
        val semanticModule = SemanticModule(module.getUniqueName(currentSemanticScope?.name), parentSemanticScope)

        if (currentSemanticScope == null) {
            semanticTree.addModule(semanticModule)
        }

        currentSemanticScope = semanticModule

        super.visitModuleScope(module)

        currentSemanticScope = parentSemanticScope
    }

    override fun visitBlockScope(scope: BlockScope?) {
        if (scope == null) return

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = SemanticScope(scope.hashCode().toString(), parentSemanticScope) // TODO Add metadata to blockscope.

        super.visitBlockScope(scope)

        currentSemanticScope = parentSemanticScope
    }

    override fun visitProperty(property: Property?) {
        if (property == null) return

        val parentScope = currentScope
        currentScope = property

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = SemanticProperty(
            property.getUniqueName(currentModule),
            parentSemanticScope
        ) // Might want to change this to currentSemanticModule instead. (not in some cases, where we use property as a parameter.

        // If the property has a value, we must treat it as an assignment TODO(maybe change the 'value' type to Assignment in the metamodel)
        property.value?.also {
            val assignment = Assignment().also { a ->
                a.reference = property.identifier
                a.metadata = property.value.metadata
                a.nestedScope = BlockScope().also { b -> b.expressions.add(property.value) }
            }

            this.visitAssignment(assignment)
        }

        property.getter?.also { this.visitUnit(it) }
        property.setter?.also { this.visitUnit(it) }

        currentScope = parentScope
        currentSemanticScope = parentSemanticScope
    }

    override fun visitUnit(unit: Unit?) {
        if (unit == null) return

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = SemanticUnit(unit.getUniqueName(currentModule), parentSemanticScope)

        val parentScope = currentScope
        currentScope = unit

        super.visitUnit(unit)

        currentSemanticScope = parentSemanticScope
        currentScope = parentScope
    }

    override fun visitDeclaration(declaration: Declaration?) {
        if (declaration == null) return

        when (declaration.value) {
            is Unit -> visitUnit((declaration.value as Unit))
            is Property -> visitProperty((declaration.value as Property))
            is ModuleScope -> visitModuleScope(declaration.value as ModuleScope)
        }
    }

    override fun visitUnitCall(unitCall: UnitCall?) {
        if (unitCall == null) return

        unitCallsToEvaluate.add(Triple(unitCall, currentModule!!, currentSemanticScope!!))

        super.visitUnitCall(unitCall)
    }

    override fun visitAssignment(assignment: Assignment?) {
        if (assignment == null) return

        // TODO(Look if I do not have to do anything with the scoping here?)
        SemanticAssignment(assignment.getUniqueName(currentModule), currentSemanticScope)

        super.visitAssignment(assignment)
    }

    override fun visitCatch(catch: Catch?) {
        if (catch == null) return

        val parentScope = currentSemanticScope
        currentSemanticScope = SemanticScope(getUniquePosition(catch.metadata), parentScope)

        super.visitCatch(catch)

        currentSemanticScope = parentScope
    }

    override fun visitConditional(conditional: Conditional?) {
        if (conditional == null) return

        val parentScope = currentSemanticScope
        currentSemanticScope = SemanticScope(getUniquePosition(conditional.metadata), parentScope)

        super.visitConditional(conditional)

        currentSemanticScope = parentScope
    }

    override fun visitLoop(loop: Loop?) {
        if (loop == null) return

        val parentScope = currentSemanticScope
        currentSemanticScope = SemanticScope(getUniquePosition(loop.metadata), parentScope)

        super.visitLoop(loop)

        currentSemanticScope = parentScope
    }

    override fun visitLambda(lambda: Lambda?) {
        if (lambda == null) return

        val parentScope = currentSemanticScope
        currentSemanticScope = SemanticScope(getUniquePosition(lambda.metadata), parentScope)

        super.visitLambda(lambda)

        currentSemanticScope = parentScope
    }

    override fun visitIdentifier(identifier: Identifier?) {
        if (identifier == null) return

        SemanticAccess(identifier.value, currentSemanticScope)
    }
}