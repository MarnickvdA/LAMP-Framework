package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.utils.getUniqueName

class SymbolTree {
    val modules: MutableMap<String, ModuleSymbol> = mutableMapOf()

    fun print() {
        modules.values.forEach(ModuleSymbol::print)
    }

    fun add(module: ModuleSymbol) {
        this.modules[module.name] = module
    }

    fun findModuleInScope(moduleRoot: ModuleRoot, moduleId: String?, moduleComponent: String?): ModuleSymbol? {
        val currentModule = modules[moduleRoot.getUniqueName(false)]!!

        // TODO(Document: We do not support inner class reference extensions: "A extends Class.Inner"
        val refMapWithoutCurrent = modules.toMutableMap().also { it.remove(currentModule.name) }

        // Finding the corresponding parent in the same component.
        refMapWithoutCurrent.values
            .filter {
                if (moduleComponent == null && moduleId != null) {
                    it.declarableId == moduleId
                } else {
                    it.declarableId == moduleId && it.component == moduleComponent
                }
            }
            .forEach { return it }

        // Finding the corresponding parent via the imports
        currentModule.moduleRoot.imports
            .filter { !it.endsWith(".*") }
            .filter {
                if (moduleComponent == null && moduleId != null) {
                    it.split(".").last() == moduleId
                } else if (moduleComponent != null && moduleId != null) {
                    it == "$moduleComponent.$moduleId"
                } else false
            }
            .forEach {
                return modules[it]
            }

        // TODO We do not cover imports that are referenced as 'nl.student.utwente.Class' within the code instead of the imports. Look how we could fix this.
        // Finding the corresponding parent via the import wildcards
        currentModule.moduleRoot.imports
            .filter { it.endsWith(".*") && moduleComponent?.let { c -> it.removeSuffix(".*") == c } ?: true }
            .map {
                refMapWithoutCurrent.values.filter { v ->
                    v.declarableId == moduleId
                }
            }
            .flatten()
            .forEach {
                return modules[it.id]
            }

        return null
    }

    /**
     * Finding the parent's full name by iterating through the dependencies of this class.
     *
     * Possible hits:
     * Any module on the same component level
     * Any module in the import list
     * - By Name
     * - By wildcard "a.component.level.*"
     */


    private fun findParent(
        ofModule: ModuleSymbol,
    ): RootSymbol? {
        // TODO(Document: We do not support inner class reference extensions: "A extends Class.Inner"
        val refMapWithoutCurrent = modules.toMutableMap().also { it.remove(ofModule.name) }

        // Finding the corresponding parent in the same component.
        refMapWithoutCurrent
            .filter { (_, module) ->
                module.component == ofModule.component && ofModule.parentRef == module.declarableId
            }
            .also {
                if (it.size > 1) // TODO(Document: It is possible (?) to match multiple modules due to not considering the accessibility level
                    System.err.println("WARNING: We have conflicting dependencies by name: ${it.keys.joinToString(", ")}")
            }
            .forEach { (name, module) ->
                return modules[name] ?: DependencySymbol(module.component, module.id)
            }

        // Finding the corresponding parent via the imports
        ofModule.moduleRoot.imports
            .filter { it.split(".").last() == ofModule.parentRef?.split(".")?.last() }
            .forEach {
                return modules[it] ?: DependencySymbol(
                    it.split(".").dropLast(1).joinToString("."),
                    it.split(".").last()
                )
            }

        // TODO We do not cover imports that are referenced as 'nl.student.utwente.Class' within the code instead of the imports. Look how we could fix this.
        // Finding the corresponding parent via the import wildcards
        ofModule.moduleRoot.imports
            .filter { it.split(".").last() == "*" }
            .map { import ->
                refMapWithoutCurrent.filterValues { v ->
                    v.component == import.removeSuffix(".*") && v.declarableId == ofModule.parentRef
                }.values
            }
            .flatten()
            .forEach {
                return modules[it.id] ?: DependencySymbol(it.component, it.id)
            }

        return null
    }

    fun connectDependencies() {
        modules.values.forEach {
            it.parent = findParent(it)?.also { p -> p.subModules.add(it) }

            // TODO References of Calls?
        }
    }
}