package nl.utwente.student.models.inheritance

data class InheritanceNode(
    val name: String,
    var parent: InheritanceNode?,
    val children: MutableList<InheritanceNode>
) {
//    fun print(depth: Int = 0) {
//        println("\t".repeat(depth) + name)
//        children.forEach { it.print(depth + 1) }
//    }

    fun getDepthOfInheritanceTree(inheritanceTree: InheritanceNode = this): Int {
        return when {
            inheritanceTree.children.isEmpty() -> 1
            else -> inheritanceTree.children.maxOf { getDepthOfInheritanceTree(it) } + 1
        }
    }

    fun getNumberOfChildren(): Int {
        return this.children.size
    }
}

