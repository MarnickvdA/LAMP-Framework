package nl.utwente.student.utils

import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.lang.Exception
import java.nio.file.Paths

fun ParseTree.getDepth(): Int {
    var depth = 0
    var parent: ParseTree? = this
    while (parent != null) {
        parent = parent.parent
        depth++
    }

    return depth
}

fun getFile(fileOrDir: String): File? {
    return try {
        Paths.get(System.getProperty("user.dir"), fileOrDir).toFile()
    } catch (ex: Exception) {
        try {
            Paths.get(fileOrDir).toFile()
        } catch (ex: Exception) {
            System.err.println("Invalid file path.")
            return null
        }
    }
}