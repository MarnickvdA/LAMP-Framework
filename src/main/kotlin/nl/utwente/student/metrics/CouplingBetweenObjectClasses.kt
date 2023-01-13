package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.Call
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.ReferenceCall
import nl.utwente.student.metamodel.v3.UnitCall
import nl.utwente.student.models.symbol.SymbolTree
import nl.utwente.student.models.metrics.SymbolMetric
import nl.utwente.student.visitors.SourceElementFinder.findAllInModule

/**
 * Count of references in both directions between two classes (property access or unit calls (excluding constructor calls?))
 * Per module: A set of outgoing AND incoming calls and property access
 *
 * TODO Q: Should library references to be included or only classes written in the project?
 */
class CouplingBetweenObjectClasses : SymbolMetric {
    private lateinit var classCoupling: MutableMap<String, Int>

    override fun getTag(): String = "CBO"

    override fun getResult(): List<Pair<String, Int>> {
        return classCoupling.toList()
    }

    override fun visitProject(modules: List<ModuleRoot>, symbolTree: SymbolTree) {
        classCoupling = mutableMapOf()

//        symbolTree.print()

        val outgoingReferences = mutableMapOf<String, Set<Call>>() // ReferenceCall, Set<Declarable>

        for (moduleRoot in modules) {
            val module = moduleRoot.module

//            println("Module = ${module.id}")

            val callsInModule = findAllInModule<Call>(module) { it is Call }
//            println("[${module.id}] Calls in module: ${callsInModule.joinToString(", ") { it.referenceId }}")

            val outgoingCalls = callsInModule
                .filter {
                    val reference = it.innerScope.firstOrNull()
                    reference is ReferenceCall && reference.referenceId != module.id
                }
                .also {
//                    println("[${module.id}] (external) refs: ${it.joinToString(", ") { c -> (c.innerScope.firstOrNull() as ReferenceCall).referenceId }}")
                }.toSet()

            val indirectOutgoingCalls = callsInModule
                .filter { it.innerScope.firstOrNull() is UnitCall }
                .filter {
                    true // TODO Check the return type of the unit call to see if it is going to another module.
                }

//            println("[${module.id}] Direct outgoing calls: ${outgoingCalls.joinToString(", ")}")

//            *      for each declarable identifier:
//            *          val type = semanticTree.getTypeOf(identifier)
//            *          if type is defined (= reference type):
//            *              references.add(type)
//            *

            outgoingReferences[module.id] = outgoingCalls.map { it }.toSet()
        }

        for (moduleId in outgoingReferences.keys) {
            if (!classCoupling.containsKey(moduleId))
                classCoupling[moduleId] = 0

            outgoingReferences[moduleId]?.forEach { reference ->
                classCoupling[moduleId] = classCoupling[moduleId]!! + 1

                // TODO Check the referenceId, is it a class or a property? Need to find it in the scope using the symbol tree.
                val moduleRef = (reference.innerScope.first() as ReferenceCall).referenceId
                classCoupling[moduleRef] =
                    if (classCoupling.containsKey(moduleRef)) classCoupling[moduleRef]!! + 1 else 1
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