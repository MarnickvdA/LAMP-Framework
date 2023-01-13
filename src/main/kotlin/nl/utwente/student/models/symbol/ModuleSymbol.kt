package nl.utwente.student.models.symbol

import nl.utwente.student.metamodel.v3.ModuleRoot

data class ModuleSymbol(val moduleRoot: ModuleRoot) : RootSymbol(
    component = moduleRoot.componentName,
    moduleRoot.module.id,
    parentRef = moduleRoot.module.extensions.firstOrNull(), // TODO If we include interfaces, we have a problem here because it can extend multiple.
    sourceModule = moduleRoot.module
) {
    val name = "${moduleRoot.componentName}.${moduleRoot.module.id}"

    override fun toString(): String = name
}