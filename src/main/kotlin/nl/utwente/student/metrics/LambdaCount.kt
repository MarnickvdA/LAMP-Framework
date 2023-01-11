package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Lambda
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.visitors.ModuleVisitor

class LambdaCount : ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String {
        return "LC"
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        result = 0
        super.visitModuleRoot(moduleRoot)
    }

    override fun visitLambda(lambda: Lambda?) {
        result = result?.plus(1) ?: 1
        super.visitLambda(lambda)
    }
}