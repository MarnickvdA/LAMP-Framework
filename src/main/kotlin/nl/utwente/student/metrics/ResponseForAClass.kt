package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Module
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.SemanticHelper.findAllByExpressionType

class ResponseForAClass : ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String = "RFC"

    override fun visitModule(module: Module?) {
        if (module == null) return

        val units = module.members.filterIsInstance<Unit>()
        val unitCallReferences = units
            .map { findAllByExpressionType<UnitCall>(it.body) { e -> e is UnitCall } }
            .flatten()
            .mapNotNull { it.declarableId } // FIXME Type information required to implement this correctly (right?)

        val constructorCalls = unitCallReferences.filter { it == "constructor" }
        val unitCalls = unitCallReferences
            .filter { it != "constructor" && units.map { u -> u.id }.contains(it) }
            .toSet()

        // TODO(Document: How to handle records, should you see the primary constructor as a +1, should you include the 'generated' functions as methods? Should you include getters and setters on properties as methods?)

        result = units.size + constructorCalls.size + unitCalls.size
    }
}