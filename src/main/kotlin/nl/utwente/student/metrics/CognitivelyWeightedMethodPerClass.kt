package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.VisitorException

class CognitivelyWeightedMethodPerClass: ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String = "CWMC"

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        if (moduleRoot == null) throw VisitorException("Module is null")
        moduleName = moduleRoot.getUniqueName(false)

        val coco = CognitiveComplexity()

        moduleRoot.module.members
            .filterIsInstance<Unit>()
            .forEach { coco.visitUnit(it) }

        result = coco.getResult().sumOf { it.second }
    }
}