package nl.utwente.student.io

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.SupportedLanguage
import nl.utwente.student.transformers.JavaTransformer
import nl.utwente.student.transformers.MetamodelTransformer
import java.io.File

object TransformEngine {

    fun transform(input: File): List<ModuleRoot>? {
        println("Transforming file(s) located in ${input.absolutePath}.")
        return when {
            input.isDirectory -> transformDirectory(input)
            isValidFile(input) -> transformFile(input)
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

    private fun transformFile(file: File): List<ModuleRoot>? {
        if (!isValidFile(file)) return emptyList()

        return when (file.extension) {
            SupportedLanguage.JAVA.fileExtension -> JavaTransformer(file)
            SupportedLanguage.METAMODEL.fileExtension -> MetamodelTransformer(file)
            else -> null
        }?.transform()
    }

    private fun transformDirectory(directory: File): List<ModuleRoot> {
        return directory.walk()
            .mapNotNull(this::transformFile)
            .flatten()
            .toList()
    }
}