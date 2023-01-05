package nl.utwente.student.io

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Unmarshaller
import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.models.SupportedLanguage
import nl.utwente.student.transformers.JavaTransformer
import nl.utwente.student.visitor.java.JavaLexer
import nl.utwente.student.visitor.java.JavaParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.io.FileInputStream

object ParserEngine {
    private val jaxbMarshaller: Unmarshaller by lazy {
        JAXBContext.newInstance(Module::class.java.packageName).createUnmarshaller()
    }

    fun read(input: File): List<Module>? {
        return when {
            input.isDirectory -> parseDirectory(input)
            isValidFile(input) -> parseFile(input)
            else -> null
        }
    }

    private fun isValidFile(file: File): Boolean {
        return when (file.extension) {
            SupportedLanguage.JAVA.fileExtension -> true
            SupportedLanguage.METAMODEL.fileExtension -> true
            // Add more languages
            else -> false
        }
    }

    private fun parseFile(file: File): List<Module> {
        if (!isValidFile(file)) return emptyList()

        return when (file.extension) {
            SupportedLanguage.JAVA.fileExtension -> parseJavaFile(file)
            SupportedLanguage.METAMODEL.fileExtension -> parseLampFile(file)
            else -> emptyList()
        }
    }

    private fun parseLampFile(file: File): List<Module> {
        return try {
            (jaxbMarshaller.unmarshal(file) as? Module)?.let { listOf(it) }
        } catch (e: JAXBException) {
            System.err.println("Reading from ${file.name} failed!")
            e.printStackTrace()
            null
        } ?: listOf()
    }

    private fun parseDirectory(directory: File): List<Module> {
        return directory.walk().mapNotNull { parseFile(it) }.flatten().toList()
    }

    private fun parseJavaFile(file: File): List<Module> {
        val parseTree: ParseTree = FileInputStream(file).use {
            val input = CharStreams.fromStream(it)
            val tokens = CommonTokenStream(JavaLexer(input))
            JavaParser(tokens).compilationUnit()
        }

        return JavaTransformer(file, parseTree).transform()
    }
}