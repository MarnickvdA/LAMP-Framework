package nl.utwente.student.parsers

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.utils.getFile
import java.io.File

abstract class Parser(private val input: String, private val output: String) {

    fun parse(): List<Module>? {
        val file: File? = getFile(input)
        val out: File? = getFile(output)

        return when {
            file == null || out == null -> return null
            file.isDirectory -> parseDirectory(file)
            isValidFile(file) -> parseFile(file)
            else -> null
        }
    }

    protected abstract fun parseFile(file: File): List<Module>

    protected abstract fun parseDirectory(directory: File): List<Module>

    protected abstract fun isValidFile(file: File): Boolean
}