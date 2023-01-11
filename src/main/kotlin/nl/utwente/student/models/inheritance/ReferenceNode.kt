package nl.utwente.student.models.inheritance

data class ReferenceNode(
    val name: String,
    val parent: String?,
    val component: String?,
    val imports: List<String>
)