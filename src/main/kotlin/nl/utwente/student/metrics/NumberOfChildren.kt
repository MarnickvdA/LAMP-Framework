package nl.utwente.student.metrics

import nl.utwente.student.visitors.ModuleVisitor

class NumberOfChildren: ModuleVisitor() {
    override fun getTag(): String = "NOC"
}