package nl.utwente.student.utils

import org.antlr.v4.runtime.tree.ParseTree

fun ParseTree.getDepth(): Int {
    var depth = 0
    var parent: ParseTree? = this
    while (parent != null) {
        parent = parent.parent
        depth++
    }

    return depth
}