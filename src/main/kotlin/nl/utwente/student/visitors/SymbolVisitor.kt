package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.models.symbol.*
import nl.utwente.student.utils.getUniquePosition

class SymbolVisitor : MetamodelVisitor<kotlin.Unit>() {
    private var symbolTree = SymbolTree()
    private var currentSymbol: SourceSymbol? = null

    /**
     * @return a map of Inheritance Tree relationships, indexed by Module reference
     */
    fun visitProject(modules: List<ModuleRoot>): SymbolTree {
        // Visit all modules to get the reference nodes which we can connect later.
        symbolTree = SymbolTree()
        modules.forEach(this::visitModuleRoot)

        // Process the node references and output the mapping of inheritance nodes
        symbolTree.connectDependencies()

        return symbolTree
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        if (moduleRoot == null) return // TODO (Document that we are excluding interfaces)
        currentSymbol = ModuleSymbol(moduleRoot)
        symbolTree.add(currentSymbol as ModuleSymbol)

        moduleRoot.module.members.forEach(this::visitDeclarable)
    }

    private fun visitDeclarable(declarable: Declarable?) {
        if (declarable == null) return

        val symbol = SourceSymbol(declarable.id, currentSymbol, declarable)
        currentSymbol!!.add(symbol)
        currentSymbol = symbol

        when (declarable) {
            is Module -> super.visitModule(declarable)
            is Unit -> super.visitUnit(declarable)
            is Property -> super.visitProperty(declarable)
        }

        currentSymbol = currentSymbol!!.parent
    }

    override fun visitModule(module: Module?) {
        this.visitDeclarable(module)
    }

    override fun visitUnit(unit: Unit?) {
        this.visitDeclarable(unit)
    }

    override fun visitProperty(property: Property?) {
        this.visitDeclarable(property)
    }


    private fun Expression.getName(): String {
        return "$context${getUniquePosition(this)}"
    }

    override fun visitExpression(expression: Expression?) {
        if (expression == null) return

        when(expression) {
            is Access -> {
                super.visitExpression(expression)
            }
            else -> {
                val symbol = SourceSymbol(expression.getName(), currentSymbol, expression)
                currentSymbol!!.add(symbol)
                currentSymbol = symbol

                super.visitExpression(expression)

                currentSymbol = currentSymbol!!.parent
            }
        }
    }

    /**
     * The way we implemented this, we must return the SourceSymbol that we create for every SUB class of Expression.
     */

    override fun visitAssignment(assignment: Assignment?) {
        if (assignment == null) return
        val symbol = ReferenceAccessSymbol(assignment.declarableId, currentSymbol!!, assignment)
        currentSymbol!!.add(symbol)
        currentSymbol = symbol

        super.visitAssignment(assignment)

        currentSymbol = currentSymbol!!.parent
    }

    override fun visitReferenceAccess(referenceAccess: ReferenceAccess?) {
        if (referenceAccess == null) return
        val symbol = AccessSymbol(referenceAccess, currentSymbol!!)
        currentSymbol!!.add(symbol)
        currentSymbol = symbol

        super.visitReferenceAccess(referenceAccess)

        currentSymbol = currentSymbol!!.parent
    }

    override fun visitUnitCall(unitCall: UnitCall?) {
        if (unitCall == null) return
        val symbol = AccessSymbol(unitCall, currentSymbol!!)
        currentSymbol!!.add(symbol)
        currentSymbol = symbol

        super.visitUnitCall(unitCall)

        currentSymbol = currentSymbol!!.parent
    }
}