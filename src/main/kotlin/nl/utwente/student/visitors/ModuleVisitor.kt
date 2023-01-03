package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.utils.getUniqueName

abstract class ModuleVisitor: MetamodelVisitor<Unit, Pair<String, Int>>() {

    abstract override fun getTag(): String

    protected var moduleName: String? = null
    protected var result: Int? = null

    override fun getResult(): Pair<String, Int> {
        if (moduleName == null || result == null) throw VisitorException("We haven't evaluated the module yet.")
        return Pair(moduleName!!, result!!)
    }

    override fun visitModule(module: Module?) {
        this.moduleName = module?.getUniqueName()
        super.visitModule(module)
    }
}