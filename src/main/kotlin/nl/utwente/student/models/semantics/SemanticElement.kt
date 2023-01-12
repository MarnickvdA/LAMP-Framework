package nl.utwente.student.models.semantics

import nl.utwente.student.metamodel.v3.SourceElement
import nl.utwente.student.metamodel.v3.Module

interface SemanticElement {
    val name: String
    val sourceElement: SourceElement?
    val parent: SemanticElement?
    val children: MutableMap<String, SemanticElement>

    fun add(element: SemanticElement): SemanticElement {
        if (children[element.name] == null)
            children[element.name] = element

        return children[element.name]!!
    }

    fun print(depth: Int = 0) {
        println("\t".repeat(depth) + toString())
        children.values.forEach { it.print(depth + 1) }
    }

    /**
     * Find an element in the tree
     * TODO Document how we traverse the semantic tree.
     */
    fun findByName(name: String): SemanticElement? {
        return when {
            this.name == name -> this
            this.children.values.isEmpty() -> null
            else -> this.children.values.find { it.findByName(name) != null }
        }
    }

    fun findClosestModule(): Module? {
        return when (parent) {
            null -> null
//            is SemanticModule -> (parent as SemanticModule).sourceElement
            else -> parent!!.findClosestModule()
        }
    }

    fun findAllInChildren(typeCheck: (element: SemanticElement) -> Boolean): List<SemanticElement> {
        val found = mutableListOf<SemanticElement>()
        if(typeCheck(this)) {
            found.add(this)
        }

        val children = this.children.map { it.value.findAllInChildren(typeCheck) }.flatten()

        found.addAll(children)

        return found
    }

    /**
     * Find an element in the scope of this element, only looking up for references.
     * If no element is found, the reference is located in a dependency.
     * TODO Document how we find within the scope.
     */
    fun findDeclarableInScope(name: String): SemanticElement? {
        return when (parent) {
            null -> null
            else -> {
                val element = this.parent!!.children.values.find { it.name == name }

                if (element != null) {
                    return element
                }

                return this.parent!!.findDeclarableInScope(name)
            }
        }
    }

    override fun toString(): String
}