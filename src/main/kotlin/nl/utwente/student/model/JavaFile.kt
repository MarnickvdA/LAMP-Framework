package nl.utwente.student.model

import nl.utwente.student.transformers.JavaTransformerV2
import nl.utwente.student.visitor.java.JavaLexer
import nl.utwente.student.visitor.java.JavaParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.io.FileInputStream
import nl.utwente.student.metamodel.v2.Module

class JavaFile private constructor(val file: File, val parseTree: ParseTree) {

    companion object {
        fun parse(file: File): JavaFile {
            println("Parsing ${file.name}.")
            FileInputStream(file).use {
                val input = CharStreams.fromStream(it)

                // Phase 1: Run the lexer
                val tokens = CommonTokenStream(JavaLexer(input))

                // Phase 2: Run the parser
                val parseTree = JavaParser(tokens).compilationUnit()

                return JavaFile(file, parseTree)
            }
        }
    }

    fun extractModulesFromAST(): List<Module> {
        return JavaTransformerV2(this).transform()
    }
}
