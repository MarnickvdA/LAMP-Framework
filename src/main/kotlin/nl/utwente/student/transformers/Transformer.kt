package nl.utwente.student.transformers

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.models.SupportedLanguage

interface Transformer {
    val language: SupportedLanguage

    fun transform(): List<Module>
}