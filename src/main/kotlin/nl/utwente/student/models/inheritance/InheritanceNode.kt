package nl.utwente.student.models.inheritance

typealias InheritanceTree = MutableMap<String, InheritanceNode>

data class InheritanceNode(
    val name: String,
    var parent: InheritanceNode?,
    val children: MutableList<InheritanceNode>
)

