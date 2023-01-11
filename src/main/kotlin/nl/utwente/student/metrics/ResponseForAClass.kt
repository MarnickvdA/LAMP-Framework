package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Identifier
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.models.metrics.SemanticMetric
import nl.utwente.student.models.semantics.*
import nl.utwente.student.utils.getUniqueName

class ResponseForAClass : SemanticMetric {
    private var moduleResults = mutableListOf<Pair<String, Int>>()

    override fun getTag(): String = "RFC"
    override fun getResult(): List<Pair<String, Int>> {
        return moduleResults
    }

    override fun visitProject(semanticTree: SemanticTree) {
        moduleResults = mutableListOf()

        // Get all top level modules
        val modules = mutableListOf<SemanticModule>()
        modules.addAll(semanticTree.children.values.filterIsInstance<SemanticModule>())
        modules.addAll(semanticTree.children.values.filterIsInstance<SemanticComponent>()
            .flatMap { it.children.values }.filterIsInstance<SemanticModule>())

        modules.forEach {
            val result = getCount(it)

            moduleResults.add(Pair(it.sourceElement.getUniqueName(it.parent?.name, false), result))
        }
    }

    private fun getCount(semanticModule: SemanticModule): Int {
        val methodDeclarations = semanticModule.children.values.filterIsInstance<SemanticUnit>().size
        val methodInvocations = semanticModule.findAllInChildren { it is SemanticUnitCall }
            .mapNotNull { ((it.sourceElement as UnitCall).reference as? Identifier)?.value }
            .toSet().also { print(it.joinToString(", ")) }.size

        return methodDeclarations + methodInvocations
    }
}