package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v2.Lambda
import nl.utwente.student.visitors.ModuleVisitor

class LambdaCount: ModuleVisitor() {
    override fun getTag(): String {
        return "LC"
    }

    override fun visitLambda(lambda: Lambda?) {
        result = result?.plus(1) ?: 1
        super.visitLambda(lambda)
    }
}