package nl.utwente.student.models.semantics

interface SemanticDeclarable: SemanticElement {
    fun addModule(module: SemanticModule) = add(module)
    fun addUnit(unit: SemanticUnit) = add(unit)
    fun addProperty(property: SemanticProperty) = add(property)
    fun addOutgoingUnitCall(unitCall: SemanticUnitCall) = add(unitCall)
    fun addAssignment(assignment: SemanticAssignment) = add(assignment)
}