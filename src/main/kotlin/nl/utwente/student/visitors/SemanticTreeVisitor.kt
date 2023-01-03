package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.models.semantics.*
import nl.utwente.student.utils.getUniqueName

class SemanticTreeVisitor(
    projectName: String, modules: List<Module>, private val semanticTree: SemanticTree = SemanticTree(projectName)
) : MetamodelVisitor<kotlin.Unit, SemanticTree>() {

    private val unitCallsToEvaluate = mutableListOf<Triple<UnitCall, Module, SemanticDeclarable>>()
    private var currentComponent: SemanticComponent? = null
    private var currentSemanticModule: SemanticModule? = null

    private var currentModule: Module? = null
    private var currentScope: Scope? = null
    private var currentSemanticScope: SemanticDeclarable? = null

    init {
        modules.forEach(this::visitModule)

        unitCallsToEvaluate.forEach {
            it.third.addOutgoingUnitCall(
                SemanticUnitCall(
                    targetUnit = it.first.getUniqueName(it.second), // TODO Handle dependencies
                    origin = it.third,
                )
            )
        }
    }

    override fun getTag(): String = "SemanticTree"

    override fun getResult(): SemanticTree {
        return semanticTree
    }

    override fun visitModule(module: Module?) {
        if (module == null) return

        if (module.packageName != null) {
            currentComponent = semanticTree.addComponent(SemanticComponent(module.packageName))
        }

        val semanticModule = SemanticModule(module.getUniqueName(), currentComponent!!)

        when {
            currentSemanticScope != null -> currentSemanticScope!!.addModule(semanticModule)
            currentComponent != null -> currentComponent!!.addModule(semanticModule)
            else -> semanticTree.addModule(semanticModule)
        }

        val parentSemanticModule = currentSemanticModule
        currentSemanticModule = semanticModule

        val parentModule = currentModule
        currentModule = module

        val parentSemanticScope = currentSemanticScope
        currentSemanticScope = currentSemanticModule

        super.visitModule(module)

        currentSemanticModule = parentSemanticModule
        currentModule = parentModule
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

        currentSemanticScope?.addAssignment(SemanticAssignment(assignment.getUniqueName()))

        super.visitAssignment(assignment)
    }
}