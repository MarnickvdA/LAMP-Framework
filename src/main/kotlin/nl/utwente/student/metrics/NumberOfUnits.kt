package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.ModuleVisitor

class NumberOfUnits : ModuleVisitor() {
    override var result: Int? = 0

    override fun getTag(): String {
        return "NOU"
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        this.moduleName = moduleRoot?.getUniqueName(false)
        this.result = moduleRoot?.module?.members?.filterIsInstance<Unit>()?.size ?: 0
    }
}