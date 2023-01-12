package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.SemanticHelper.findAllByExpressionType


class LackOfCohesionInMethods : ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String = "LCOM"

    override fun visitModule(module: Module?) {
        if (module == null || module.modifiers.contains(ModifierType.ABSTRACT)) return // TODO (Only LCOM for non-abstract classes)

        val unitProperties = mutableMapOf<String, MutableSet<String>>()
        module.members.filterIsInstance<Unit>()
            .filter { it.id != "constructor" && it.id != "initializer" }
            .forEach {
                val properties = mutableSetOf<String>()

                properties.addAll(
                    findAllByExpressionType<LocalDeclaration>(it.body) { e -> e is LocalDeclaration }
                        .filter { ld -> ld.declaration is Property }
                        .map { ld -> ld.declaration.id }
                )

                if (unitProperties.containsKey(it.id)) {
                    unitProperties[it.id]!!.union(properties)
                } else {
                    unitProperties[it.id] = properties
                }
            }

        var pairsWithoutSharedFieldVariables = 0
        var pairsWithSharedFieldVariables = 0

        val unitNames = unitProperties.keys.toList()
        for (i in 0 until unitNames.size - 1) {
            for (j in (i + 1) until unitNames.size) {
                val a = unitProperties[unitNames[i]]!!
                val b = unitProperties[unitNames[j]]!!

                if (a.intersect(b).any()) {
                    pairsWithSharedFieldVariables += 1
                } else {
                    pairsWithoutSharedFieldVariables += 1
                }
            }
        }

        result = maxOf(0, pairsWithoutSharedFieldVariables - pairsWithSharedFieldVariables)

        /**
         * = For Each Module =
         *
         * Map<String, Set<String>> of unitPropertyVariables
         *
         *  for each Unit declaration:
         *      variablesUsed = get all Identifier elements in Unit scope
         *      properties = empty list of type Property
         *
         *      for each Variable (Identifier):
         *           if Identifier references to Property:
         *              add property to properties list
         *
         *      if unit identifier is NOT in unitPropertyVariables map:
         *          create new map entry with empty Set for unitId.
         *
         *      add 'properties' to the unitPropertyVariables[unitId] Set.
         *
         * // Calculate the method cohesion pairs.
         *  list of unitNames = unitPropertyVariables.keys.toList
         *
         *  Int pairsWithoutSharedFieldVariables = 0
         *  Int pairsWithSharedFieldVariables = 0
         *
         *  for i = 0, i < unitNames.size - 1, i++:
         *      for j = i + 1, j < unitNames.size, j++:
         *          Set<String> a = unitPropertyVariables[unitNames[i]]
         *          Set<String> b = unitPropertyVariables[unitNames[j]]
         *
         *          if a.intersect(b) .Any????:
         *              pairsWithSharedFieldVariables++
         *          else pairsWithoutSharedFieldVariables++
         *
         *  return maxOf(0, pairsWithoutShared - pairsWithShared
         */
    }

    private fun getPropertiesPerUnit(units: List<Unit>): Map<Unit, Set<Property>> {
        /**
         * propertiesPerUnit: Map<Unit, Set<Property>>
         *     for each unit:
         *      Set<Property> properties = getUsedFields(unit):
         *          go over every reference: if it refers to a Property, add it to the set.
         *      propertiesPerUnit[unitId] = properties
         */
        val properties = mutableMapOf<Unit, MutableSet<Property>>()

        units.forEach {
//            properties[it] = setOf(findAllByExpressionType(it.body) {e -> e is ReferenceCall})
        }

        return properties
    }

    /**
     * get all units of class (excluding constructors and initializers)
     * propertiesPerUnit: Map<Unit, Set<Property>>
     *     for each unit:
     *      Set<Property> properties = getUsedFields(unit):
     *          go over every reference: if it refers to a Property, add it to the set.
     *      propertiesPerUnit[unitId] = properties
     *
     * linkedUnits: Map<String, Set<Unit>>
     *     for each unit:
     *      Set<Unit> usedUnits = getUsedUnits(unit):
     *          go over every unit call, if the reference is a Unit identifier, add it to the set
     *      linkedUnits[unitId] = usedUnits
     *
     *     for each unit:
     *      Set<Unit> linkedUnits = linkages[unitId]
     *      for each linkedUnit in linkedUnits:
     *          linkages[linkedUnitId].add[unit]
     *
     * calculateComponents:
     *  units = Set<Unit> all units in module.
     *  components: Set<Set<Unit>>
     *
     *  while(units.size > 0):
     *      component = Set<Unit>
     *      curUnit = units.iterator().next()
     *      units.remove(curUnit)
     *      component.add(curUnit)
     *
     *      Set<Property> propertiesUsed = setOf(propertiesPerUnit[curUnit])
     *
     *      while(true):
     *          Set<Unit> unitsToAdd = setOf()
     *          for each unit in units:
     *              if propertiesPerUnit[unit].retainAll(propertiesUsed).isNotEmpty()
     *                  ||
     *                 linkedUnits[unit].retainAll(component).isNotEmpty():
     *                 unitsToAdd.add(unit)
     *                 propertiesUsed.addAll(propertiesPerUnit[unit])
     *
     *          if unitsToAdd.size == 0: break;
     *
     *          units.removeAll(unitsToAdd)
     *          component.addAll(unitsToAdd)
     *
     *      components.add(component)
     *
     * LCOM = components.size
     */
}