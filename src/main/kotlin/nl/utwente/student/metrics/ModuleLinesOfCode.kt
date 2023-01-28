package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.ModuleVisitor

class ModuleLinesOfCode: ModuleVisitor() {
    override var result: Int? = 0
    private var curLinesCovered = mutableSetOf<Int>()

    override fun getTag(): String = "MLOC"

    override fun getResult(): Pair<String, Int> {
        result = curLinesCovered.size
        return super.getResult()
    }

    private fun visitDeclarable(declarable: Declarable) {
        curLinesCovered.add(declarable.metadata.startLine.toInt())
        curLinesCovered.add(declarable.metadata.endLine.toInt())
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        curLinesCovered = mutableSetOf()
        super.visitModuleRoot(moduleRoot)
    }

    override fun visitModule(module: Module?) {
        module?.let { visitDeclarable(it) }
        super.visitModule(module)
    }

    override fun visitUnit(unit: Unit?) {
        unit?.let { visitDeclarable(it) }
        super.visitUnit(unit)
    }

    override fun visitProperty(property: Property?) {
        property?.let { visitDeclarable(it) }
        super.visitProperty(property)
    }

    override fun visitExpression(expression: Expression?) {
        if (expression == null) return

        curLinesCovered.add(expression.metadata.startLine.toInt())
        curLinesCovered.add(expression.metadata.endLine.toInt())

        super.visitExpression(expression)
    }

}