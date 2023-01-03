package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.visitors.VisitorException
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.utils.getUniqueName

class WeightedMethodPerClass : ModuleVisitor() {
    override fun getTag(): String = "WMC"

    override fun visitModule(module: Module?) {
        if (module == null) throw VisitorException("Module is null")
        moduleName = module.getUniqueName()

        val coco = CyclomaticComplexity()

        module.moduleScope.members
            .filterIsInstance<Unit>()
            .forEach { coco.visitUnit(it) }

        result = coco.getResult().sumOf { it.second }
    }
}