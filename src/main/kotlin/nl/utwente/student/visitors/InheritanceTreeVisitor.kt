package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.ModuleType
import nl.utwente.student.models.inheritance.InheritanceNode
import nl.utwente.student.models.inheritance.InheritanceTree
import nl.utwente.student.models.inheritance.ReferenceNode
import nl.utwente.student.utils.getUniqueName

class InheritanceTreeVisitor: MetamodelVisitor<ReferenceNode?>() {

    /**
     * @return a map of Inheritance Tree relationships, indexed by Module reference
     */
    fun visitProject(modules: List<ModuleRoot>): InheritanceTree {
        // Visit all modules to get the reference nodes which we can connect later.
        val nodeMap = mutableMapOf<String, ReferenceNode>()
        modules.mapNotNull(this::visitModuleRoot).forEach { nodeMap[it.name] = it }

        // Process the node references and output the mapping of inheritance nodes
        return processNodeReferences(nodeMap)
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?): ReferenceNode? {
        if (moduleRoot == null || moduleRoot.module.moduleType == ModuleType.INTERFACE) return null // TODO (Document that we are excluding interfaces)

        return ReferenceNode(
            moduleRoot.getUniqueName(false),
            moduleRoot.module.extensions.firstOrNull(),
            moduleRoot.componentName,
            moduleRoot.imports
        )
    }

    private fun processNodeReferences(refMap: Map<String, ReferenceNode>): InheritanceTree {
        val inheritanceTree: InheritanceTree = mutableMapOf()

        // First iteration, populate empty inheritance nodes in the map.
        refMap.forEach { (name, _) ->
            inheritanceTree[name] = InheritanceNode(name, null, mutableListOf())
        }

        // Second iteration, populate parent and child relations in inheritance nodes.
        refMap.forEach { (name, node) ->
            val current = inheritanceTree[name]!!
            current.parent = findParent(node, refMap, inheritanceTree)?.also { p -> p.children.add(current) }
        }

        return inheritanceTree
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
        curRef: ReferenceNode,
        refMap: Map<String, ReferenceNode>,
        nodeMap: Map<String, InheritanceNode>
    ): InheritanceNode? {
        // TODO(Document: We do not support inner class reference extensions: "A extends Class.Inner"
        if (curRef.parent == null) return null

        fun getComponent(moduleName: String): String {
            return moduleName.split(".").toMutableList().also { it.remove(it.last()) }.joinToString(".")
        }

        val curComponent = getComponent(curRef.name)

        val refMapWithoutCurrent = refMap.toMutableMap().also {
            it.remove(curRef.name)
        }

        // Finding the corresponding parent in the same component.
        refMapWithoutCurrent
            .filter {
                getComponent(it.key) == curComponent && it.key.split(".").last() == curRef.parent
            }
            .also {
                if (it.size > 1) // TODO(Document: It is possible to match multiple modules due to not considering the accessibility level
                    System.err.println("WARNING: We have conflicting dependencies by name: ${it.keys.joinToString(", ")}")
            }
            .forEach {
                return nodeMap[it.key] ?: InheritanceNode(it.key, null, mutableListOf())
            }

        // TODO(Document: Maybe there is some edge case here where the compiler did find a dependency but we can't)
        if (curRef.imports.isEmpty()) return null

        // Finding the corresponding parent via the imports
        curRef.imports
            .filter { it.split(".").last() == curRef.parent }
            .forEach {
                return nodeMap[it] ?: InheritanceNode(it, null, mutableListOf())
            }

        // Finding the corresponding parent via the import wildcards
        curRef.imports
            .filter { it.split(".").last() == "*" }
            .map {
                refMapWithoutCurrent.filterValues { v ->
                    v.component == it.removeSuffix(".*") && v.name.split(".").last() == curRef.parent
                }.values
            }
            .flatten()
            .forEach {
                return nodeMap[it.name] ?: InheritanceNode(it.name, null, mutableListOf())
            }

        return null
    }
}