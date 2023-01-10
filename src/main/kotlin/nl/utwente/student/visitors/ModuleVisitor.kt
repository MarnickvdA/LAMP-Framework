package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.metrics.ModuleMetric
import nl.utwente.student.utils.getUniqueName

abstract class ModuleVisitor: MetamodelVisitor<Unit>(), ModuleMetric {

    abstract override fun getTag(): String

    protected var moduleName: String? = null
    abstract var result: Int?

    override fun getResult(): Pair<String, Int> {
        if (moduleName == null) throw VisitorException("We haven't evaluated the module yet.")
        if (result == null) throw VisitorException("We haven't added any metric results yet.")
        return Pair(moduleName!!, result!!)
    }

    override fun visitModuleRoot(moduleRoot: ModuleRoot?) {
        this.moduleName = moduleRoot?.getUniqueName(false)
        super.visitModuleRoot(moduleRoot)
    }
}