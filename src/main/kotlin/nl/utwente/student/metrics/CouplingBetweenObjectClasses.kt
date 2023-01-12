package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Call
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.ReferenceCall
import nl.utwente.student.models.metrics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree
import nl.utwente.student.visitors.SemanticHelper
import nl.utwente.student.visitors.SemanticHelper.findAllByExpressionType
import nl.utwente.student.visitors.SemanticHelper.findAllInModule

/**
 * Count of references in both directions between two classes (property access or unit calls (excluding constructor calls?))
 * Per module: A set of outgoing AND incoming calls and property access
 *
 * TODO Q: Should library references to be included or only classes written in the project?
 */
class CouplingBetweenObjectClasses : SemanticMetric {
    private lateinit var classCoupling: MutableMap<String, Int>

    override fun getTag(): String = "CBO"

    override fun getResult(): List<Pair<String, Int>> {
        return classCoupling.toList()
    }

    override fun visitProject(modules: List<ModuleRoot>) {
        classCoupling = mutableMapOf()
        val outgoingReferences = mutableMapOf<String, Set<String>>() // ReferenceCall, Set<Declarable>

        for (moduleRoot in modules) {
            val module = moduleRoot.module

            val callsInModule = findAllInModule<Call>(module) { it is Call }

            val outgoingCalls = callsInModule
                .filter { it.innerScope.firstOrNull() is ReferenceCall }
                .map {
                    // FIXME: Get Type Information from call.declarableId to find what type we are referencing to.
                    (it.innerScope.first() as ReferenceCall).declarableId
                }.toSet()


            outgoingReferences[module.id] = outgoingCalls
        }

        for (moduleId in outgoingReferences.keys) {
            if (!classCoupling.containsKey(moduleId))
                classCoupling[moduleId] = 0

            outgoingReferences[moduleId]?.forEach { reference ->
                classCoupling[moduleId] = classCoupling[moduleId]!! + 1

                classCoupling[reference] =
                    if (classCoupling.containsKey(reference)) classCoupling[reference]!! + 1 else 1
            }
        }

        classCoupling = classCoupling
            .filter { modules.map { m -> m.module.id }.contains(it.key) }
            .mapKeys { modules.find { m -> m.module.id == it.key }!!.componentName + ".${it.key}" }
            .toMutableMap()

        /**
         * val classCoupling = Map<String, Int>
         * val outgoingReferences = Map<Identifier, Set<Declarable>>
         *
         * For each module:
         *  outgoingCoupling = getReferences(module, semanticTree):
         *      Set<Identifier> references = Set()
         *      List identifierNames = module.getDeclarableIdentifiers()
         *
         *      for each declarable identifier:
         *          val type = semanticTree.getTypeOf(identifier)
         *          if type is defined (= reference type):
         *              references.add(type)
         *
         *      return references
         *
         *  if outgoingReferences does not contain module.id
         *      outgoingReferences.add(module.id, emptySet()) // for partial classes
         *
         *  outgoingReferences.add(module.id, outgoingCoupling)
         *
         *
         *  for each moduleId using outgoingReferences.keys:
         *      if classCoupling != contain moduleId
         *          classCoupling.add(moduleId, 0)
         *
         *      for each reference using outgoingReferences[moduleId]:
         *          if outgoingReferences != contain reference: continue // External dependency?
         *
         *         classCoupling[moduleId] += 1
         *
         *         if classCoupling contains reference:
         *              classCoupling[reference] += 1
         *         else
         *              classCoupling.add(reference, 1)
         *
         *  return classCoupling
         */
    }
}