package nl.utwente.student.metrics

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.ModuleVisitor
import nl.utwente.student.visitors.SourceElementFinder.findAllByExpressionType


class LackOfCohesionInMethods : ModuleVisitor() {
    override var result: Int? = 0
    override fun getTag(): String = "LCOM"

    override fun visitModule(module: Module?) {
        if (module == null || module.modifiers.contains(ModifierType.ABSTRACT)) return // TODO (Only LCOM for non-abstract classes)

        // FIXME: Check on return type to create 'method pairs'

        val units = module.members.filterIsInstance<Unit>().toMutableList()
        units.addAll(module.members.filterIsInstance<Property>().map { listOf(it.getter, it.setter) }.flatten())

        val propertiesPerUnit = getPropertiesPerUnit(units)
        val linkedUnits = getLinkedUnits(units)
        result = calculateComponents(units, propertiesPerUnit, linkedUnits).size
    }

    private fun getPropertiesPerUnit(units: List<Unit>): Map<String, Set<String>> {
        /**
         * propertiesPerUnit: Map<Unit, Set<Property>>
         *     for each unit:
         *      Set<Property> properties = getUsedFields(unit):
         *          go over every reference: if it refers to a Property, add it to the set.
         *      propertiesPerUnit[unitId] = properties
         */
        val properties = mutableMapOf<String, Set<String>>()

        units.forEach {
            properties[it.id] =
                findAllByExpressionType<ReferenceCall>(it.body) { e -> e is ReferenceCall }.map { c -> c.referenceId }
                    .toSet()
        }

        return properties
    }

    private fun getLinkedUnits(units: List<Unit>): Map<String, Set<String>> {
        /**
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
         */

        val linkages = mutableMapOf<String, MutableSet<String>>()

        units.forEach {
            linkages[it.id] =
                findAllByExpressionType<UnitCall>(it.body) { e -> e is UnitCall }.map { c -> c.referenceId }
                    .toMutableSet()
        }

        units.forEach {
            linkages[it.id]?.forEach { linkedUnitId ->
                val link = linkages[linkedUnitId]
                link?.add(linkedUnitId) // TODO Check if we can leave out linked units that we cannot find.
            }
        }

        return linkages
    }

    private fun calculateComponents(
        units: List<Unit>,
        propertiesPerUnit: Map<String, Set<String>>,
        linkedUnits: Map<String, Set<String>>
    ): Set<Set<String>> {
        /**
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

        val availableUnits = units.map { it.id }.toMutableSet()
        val components = mutableSetOf<MutableSet<String>>()

        while (availableUnits.size > 0) {
            val component = mutableSetOf<String>()
            val curUnit = availableUnits.iterator().next()
            availableUnits.remove(curUnit)
            component.add(curUnit)

            val propertiesUsed = propertiesPerUnit[curUnit]!!.toMutableSet()

            while (true) {
                val unitsToAdd = mutableSetOf<String>()
                availableUnits.forEach {
                    if (propertiesPerUnit[it]?.intersect(propertiesUsed)?.isNotEmpty() == true
                        || linkedUnits[it]?.intersect(component)?.isNotEmpty() == true) {
                        unitsToAdd.add(it)
                        propertiesUsed.addAll(propertiesPerUnit[it]!!)
                    }
                }

                if (unitsToAdd.size == 0) break

                availableUnits.removeAll(unitsToAdd)
                component.addAll(unitsToAdd)
            }

            components.add(component)
        }

        return components
    }

    private fun calculateLCOM(module: Module) {
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

        val unitProperties = mutableMapOf<String, MutableSet<String>>()
        module.members.filterIsInstance<Unit>()
            .filter { it.id.endsWith(".constructor") && it.id.endsWith("initializer") }
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
    }
}