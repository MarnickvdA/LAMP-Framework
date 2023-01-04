package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.models.semantics.*
import nl.utwente.student.utils.getUniqueName

class SemanticTreeVisitor(
    private val projectName: String, private val modules: List<Module>
) : MetamodelVisitor<kotlin.Unit, SemanticTree>() {

    private lateinit var semanticTree: SemanticTree
    private val unitCallsToEvaluate = mutableListOf<Triple<UnitCall, Module, SemanticDeclarable>>()
    private var currentComponent: SemanticComponent? = null
    private var currentSemanticModule: SemanticModule? = null

    private var currentModule: Module? = null
    private var currentScope: Scope? = null
    private var currentSemanticScope: SemanticDeclarable? = null

    override fun getTag(): String = "SemanticTree"

    override fun getResult(): SemanticTree {
        semanticTree = SemanticTree(projectName)

        modules.forEach(this::visitModule)

        unitCallsToEvaluate.forEach {
            it.third.addOutgoingUnitCall(
                SemanticUnitCall(
                    name = it.first.getUniqueName(it.second), // TODO Handle dependencies
                    origin = it.third,
                )
            )
        }

        return semanticTree
    }

    override fun visitModule(module: Module?) {
        if (module == null) return

        if (module.packageName != null) {
            currentComponent = semanticTree.addComponent(SemanticComponent(module.packageName))
        }

        currentModule = module
        this.visitModuleScope(module.moduleScope)
    }

    override fun visitModuleScope(module: ModuleScope?) {
        if (module == null) return
        val semanticModule = SemanticModule(module.getUniqueName(currentComponent?.name), currentComponent)

        when {
            currentSemanticScope != null -> currentSemanticScope!!.addModule(semanticModule)
            currentComponent != null -> currentComponent!!.addModule(semanticModule)
            else -> semanticTree.addModule(semanticModule)
        }

        val parentSemanticModule = currentSemanticModule
        currentSemanticModule = semanticModule

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = currentSemanticModule

        super.visitModuleScope(module)

        currentSemanticModule = parentSemanticModule
        currentSemanticScope = parentSemanticScope
    }

    private fun hasSemanticObjects(scope: BlockScope?): Boolean {
        return scope == null || scope.expressions.none {
            it is Loop
                    || it is Conditional
                    || it is LogicalSequence
                    || it is Jump
                    || it is Declaration
                    || it is Assignment
                    || it is Lambda
                    || it is UnitCall
                    || it is Catch
                    || hasSemanticObjects(it.nestedScope)
        }
    }

    override fun visitBlockScope(scope: BlockScope?) {
        if (!hasSemanticObjects(scope)) return

        // FIXME How are we handling the semantic tree, doesn't work atm.

        val parentSemanticScope = currentSemanticScope
        val currentScope = SemanticScope(parentSemanticScope!!.name)
        currentSemanticScope = currentScope
        parentSemanticScope.add(currentScope)

        super.visitBlockScope(scope)

        currentSemanticScope = parentSemanticScope
    }

    override fun visitProperty(property: Property?) {
        if (property == null) return

        val semanticProperty = SemanticProperty(property.getUniqueName(currentModule), currentSemanticModule!!)
        currentSemanticScope?.addProperty(semanticProperty)

        val parentScope = currentScope
        currentScope = property

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = semanticProperty

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

        val semanticUnit = SemanticUnit(unit.getUniqueName(currentModule), currentSemanticModule!!)
        currentSemanticScope?.addUnit(semanticUnit)

        val parentScope = currentScope
        currentScope = unit

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = semanticUnit

        super.visitUnit(unit)

        currentScope = parentScope
        currentSemanticScope = parentSemanticScope
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

        currentSemanticScope?.addAssignment(SemanticAssignment(assignment.getUniqueName(currentModule)))

        super.visitAssignment(assignment)
    }

    override fun visitCatch(catch: Catch?) {
        val parentScope = currentSemanticScope
        val currentScope = SemanticScope(parentScope!!.name)
        currentSemanticScope = currentScope
        parentScope.add(currentScope)

        super.visitCatch(catch)

        currentSemanticScope = parentScope
    }

    override fun visitConditional(conditional: Conditional?) {
        val parentScope = currentSemanticScope
        val currentScope = SemanticScope(parentScope!!.name)
        currentSemanticScope = currentScope
        parentScope.add(currentScope)

        super.visitConditional(conditional)

        currentSemanticScope = parentScope
    }

    override fun visitLoop(loop: Loop?) {
        val parentScope = currentSemanticScope
        val currentScope = SemanticScope(parentScope!!.name)
        currentSemanticScope = currentScope
        parentScope.add(currentScope)

        super.visitLoop(loop)

        currentSemanticScope = parentScope
    }

    override fun visitLambda(lambda: Lambda?) {
        val parentScope = currentSemanticScope
        val currentScope = SemanticScope(parentScope!!.name)
        currentSemanticScope = currentScope
        parentScope.add(currentScope)

        super.visitLambda(lambda)

        currentSemanticScope = parentScope
    }
}