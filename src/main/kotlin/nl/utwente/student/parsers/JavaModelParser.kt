package nl.utwente.student.parsers

import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.transformers.JavaTransformer
import nl.utwente.student.visitor.java.JavaLexer
import nl.utwente.student.visitor.java.JavaParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.io.FileInputStream

class JavaModelParser(input: String, output: String) : Parser(input, output) {

    override fun parseFile(file: File): List<Module> {
        return parseJavaFile(file)
    }

    override fun parseDirectory(directory: File): List<Module> {
        val modules = mutableListOf<Module>()
        directory.walk().forEach { modules.addAll(parseJavaFile(it)) }
        return modules
    }

    override fun isValidFile(file: File): Boolean {
        return !file.isDirectory && file.name.endsWith(".java")
    }

    private fun parseJavaFile(file: File): List<Module> {
        if (!isValidFile(file)) return emptyList()

        val parseTree: ParseTree = FileInputStream(file).use {
            val input = CharStreams.fromStream(it)
            val tokens = CommonTokenStream(JavaLexer(input))
            JavaParser(tokens).compilationUnit()
        }

        return JavaTransformer(file, parseTree).transform()
    }
}