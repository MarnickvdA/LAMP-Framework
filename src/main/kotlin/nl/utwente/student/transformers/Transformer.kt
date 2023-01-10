package nl.utwente.student.transformers

import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.SupportedLanguage
import java.io.File

interface Transformer {
    val inputFile: File
    val language: SupportedLanguage

    fun transform(): List<ModuleRoot>
}