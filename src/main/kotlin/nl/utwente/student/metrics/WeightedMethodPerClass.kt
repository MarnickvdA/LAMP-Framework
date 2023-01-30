package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.VisitorException
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.utils.getUniqueName

class WeightedMethodPerClass : ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String = "WMC"

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        if (moduleRoot == null) throw VisitorException("Module is null")
        moduleName = moduleRoot.getUniqueName(false)

        val cc = CyclomaticComplexity()
        cc.visitModuleRoot(moduleRoot)

        result = cc.getResult().sumOf { it.second }
    }
}