package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.metrics.SemanticMetric
import nl.utwente.student.models.semantics.SemanticTree

/**
 * Count of references in both directions between two classes (property access or unit calls (excluding constructor calls?))
 * Per module: A set of outgoing AND incoming calls and property access
 *
 * TODO Q: Should library references to be included or only classes written in the project?
 */
class CouplingBetweenObjectClasses : SemanticMetric {
    override fun getTag(): String = "CBO"

    override fun getResult(): List<Pair<String, Int>> {
        TODO("Not yet implemented")
    }

    override fun visitProject(modules: List<ModuleRoot>, semanticTree: SemanticTree) {
        TODO("Not yet implemented")

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