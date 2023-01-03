package nl.utwente.student.metrics

import nl.utwente.student.visitors.ModuleVisitor

class DepthOfInheritanceTree: ModuleVisitor() {
    override fun getTag(): String = "DIT"
}