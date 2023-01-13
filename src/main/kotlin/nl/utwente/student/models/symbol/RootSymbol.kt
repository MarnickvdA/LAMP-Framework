package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.Module

abstract class RootSymbol(val component: String?, val declarableId: String? = null, val parentRef: String? = null, sourceModule: Module? = null) :
    SourceSymbol(id = "${component?.let { "$it." }}${declarableId?.let { "$declarableId" }}", sourceElement = sourceModule) {
    val subModules = mutableListOf<RootSymbol>()
}