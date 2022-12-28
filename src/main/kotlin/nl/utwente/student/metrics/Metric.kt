package nl.utwente.student.metrics

import nl.utwente.student.visitor.BaseVisitor

abstract class Metric<T>: BaseVisitor<T, MetricException>() {
    open fun getTag(): String = ""
}