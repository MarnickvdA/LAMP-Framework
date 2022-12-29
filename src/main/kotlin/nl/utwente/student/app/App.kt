package nl.utwente.student.app

import nl.utwente.student.io.GitClient
import nl.utwente.student.io.XMLWriter
import nl.utwente.student.parsers.JavaModelParser
import nl.utwente.student.parsers.MetamodelParser
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Text-based User Interface
 */
class App {
    var isActive = true

    fun start() {
        while (isActive) {
            print("$ ")
            handleInput()
        }
    }

    private fun handleInput() {
        val userInput = readln()
        val arguments = userInput.split(" ")

        if (arguments.isEmpty()) {
            printWarning("Invalid Input.")
            printHelp()
            return
        }

        val url: String? = getArgument(arguments, "-url")
        val lang: String? = getArgument(arguments, "-lang") ?: "java"
        val input: String? = getArgument(arguments, "-in")
        val output: String? = getArgument(arguments, "-out")

        when(arguments[0]) {
            "clone" -> cloneRepository(url, output)
            "transform" -> transform(lang, input, output)
            "evaluate" -> evaluate(input, output)
            "exit", "q", "quit" -> this.isActive = false
            else -> {
                printWarning("Unknown command '${arguments[0]}'.")
                printHelp()
            }
        }
    }

    private fun cloneRepository(url: String?, output: String?): File? {
        if (url == null || !url.endsWith(".git")) {
            printWarning("You must specify a valid Git repository url.")
            return null
        }

        val file = GitClient.cloneRepository(url, output)

        return file?.also {
            println("Project located at ${it.absolutePath}")
        }
    }

    private fun transform(lang: String?, input: String?, output: String?): File? {
        if (input == null || output == null || lang == null) {
            System.err.println("Invalid arguments for command.")
            return null
        }

        val modules = when(lang) {
            "java", "Java" -> JavaModelParser(input, output).parse()
            else -> {
                printWarning("Unsupported programming language.")
                null
            }
        }

        val file = modules?.let { XMLWriter.writeModules(it, output) }

        return file?.also {
            println("Transformed ${modules.size} .$lang file(s), now located in ${it.absolutePath}")
        }
    }

    private fun evaluate(input: String?, output: String?): File? {
        if (input == null) {
            System.err.println("You must provide an input.")
            return null
        }

        val dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        val now = LocalDateTime.now()

        val modules = MetamodelParser(input, output ?: "results/${dtf.format(now)}").parse()

        if (modules == null) {
            System.err.println("No modules to evaluate.")
            return null
        }

        println("Starting evaluation of ${modules.size} module(s).")

        return MetricRunner(modules).run(output)?.let { file ->
            println("Transformed ${modules.size} file(s), now located in ${file.absolutePath}")
            file
        }
    }

    private fun getArgument(arguments: List<String>, tag: String): String? {
        for ((index, arg) in arguments.withIndex()) {
            if (arg == tag && arguments.size > index + 1) {
                return arguments[index + 1]
            }
        }

        return null
    }

    private fun printWarning(warning: String) {
        System.err.println(warning)
    }

    private fun printHelp() {
        println("""
            Available commands:
            $ clone             -url    [gitUrl] 
                                -out    [outputDir]
            $ transform         -lang   ["java"]
                                -in     [inputFile/inputDir]
                                -out    [outputDir]
            $ evaluate          -in     [inputFile/inputDir]
                                -out    [outputDir]
        """.trimIndent())
    }
}