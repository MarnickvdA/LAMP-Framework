package nl.utwente.student.io

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.SupportedLanguage
import nl.utwente.student.transformers.JavaTransformer
import nl.utwente.student.transformers.MetamodelTransformer
import java.io.File

object ParserEngine {

    fun parse(input: File): List<ModuleRoot>? {
        println("Parsing file(s) located in ${input.absolutePath}.")
        return when {
            input.isDirectory -> parseDirectory(input)
            isValidFile(input) -> parseFile(input)
            else -> null
        }
    }

    private fun isValidFile(file: File): Boolean {
        return when (file.extension) {
            // Add more languages
            SupportedLanguage.JAVA.fileExtension,
            SupportedLanguage.METAMODEL.fileExtension -> true
            else -> false
        }
    }

    private fun parseFile(file: File): List<ModuleRoot>? {
        if (!isValidFile(file)) return emptyList()

        return when (file.extension) {
            SupportedLanguage.JAVA.fileExtension -> JavaTransformer(file)
            SupportedLanguage.METAMODEL.fileExtension -> MetamodelTransformer(file)
            else -> null
        }?.transform()
    }

    private fun parseDirectory(directory: File): List<ModuleRoot> {
        return directory.walk()
            .mapNotNull(this::parseFile)
            .flatten()
            .toList()
    }
}