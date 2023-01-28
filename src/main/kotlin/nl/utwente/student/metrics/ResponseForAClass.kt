package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Module
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.SourceElementFinder.findAllByExpressionType

class ResponseForAClass : ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String = "RFC"

    override fun visitModule(module: Module?) {
        if (module == null) return

        val methods = module.members.filterIsInstance<Unit>()
            .filter { !it.id.endsWith(".constructor") && !it.id.endsWith(".initializer") }

        val calls = methods
            .map { findAllByExpressionType<UnitCall>(it.body) { e -> e is UnitCall } }
            .flatten()
            .mapNotNull { it.declarableId }
            .toSet()

        // TODO(Document: How to handle records, should you see the primary constructor as a +1, should you include the 'generated' functions as methods? Should you include getters and setters on properties as methods?)

        result = mutableSetOf<String>().also {
            it.addAll(methods.map { m -> m.id })
            it.addAll(calls)
        }.size
    }
}