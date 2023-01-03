package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.visitors.VisitorException
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.utils.getUniqueName

class CognitivelyWeightedMethodPerClass: ModuleVisitor() {

    override fun getTag(): String = "CWMC"

    override fun visitModule(module: Module?) {
        if (module == null) throw VisitorException("Module is null")
        moduleName = module.getUniqueName()

        val coco = CognitiveComplexity()

        module.moduleScope.members
            .filterIsInstance<Unit>()
            .forEach { coco.visitUnit(it) }

        result = coco.getResult().sumOf { it.second }
    }
}