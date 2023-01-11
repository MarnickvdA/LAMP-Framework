package nl.utwente.student.app

import nl.utwente.student.io.GitEngine
import nl.utwente.student.io.MetricsEngine
import nl.utwente.student.io.ParserEngine
import nl.utwente.student.io.WriterEngine
import nl.utwente.student.utils.getFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Central part of the application. It executes orders given from the CLI or TUI. This class is
 * mainly responsible for validating the import and propagating orders to one of the engines.
 */
object App {

    fun execute(userInput: String? = null) {
        val arguments = userInput?.split(" ")

        if (arguments.isNullOrEmpty()) {
            printWarning("Invalid Input.")
            printHelp()
            return
        }

        val url: String? = getArgument(arguments, "-url")
        val input: String? = getArgument(arguments, "-in")
        val output: String? = getArgument(arguments, "-out")

        when(arguments[0]) {
            "clone" -> cloneRepository(url, output)
            "transform" -> transform(input, output)
            "evaluate" -> evaluate(input, output)
            "exit", "q", "quit" -> throw Exception("Thank you for using LAMP.")
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

        val file = GitEngine.clone(url, output)

        return file?.also {
            println("Project located at ${it.absolutePath}")
        }
    }

    private fun transform(input: String?, output: String?): File? {
        if (input == null || output == null) {
            System.err.println("Invalid arguments for command.")
            return null
        }

        val inputFile: File? = getFile(input)
        val outputFile: File? = getFile(output)

        if (inputFile == null || outputFile == null) {
            System.err.println("Invalid file path.")
            return null
        }

        return ParserEngine.parse(inputFile)?.let { WriterEngine.write(it, outputFile) }
    }

    private fun evaluate(input: String?, output: String?): File? {
        if (input == null) {
            System.err.println("You must provide an input.")
            return null
        }

        val dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        val now = LocalDateTime.now()

        val inputFile: File? = getFile(input)
        var outputFile: File? = getFile(output)

        if (outputFile == null) {
            outputFile = getFile("results/${dtf.format(now)}")
        }

        if (inputFile == null || outputFile == null) {
            System.err.println("Invalid file path.")
            return null
        }

        val modules = ParserEngine.parse(inputFile)

        if (modules == null) {
            printWarning("No modules to evaluate.")
            return null
        }

        println("Starting evaluation of ${modules.size} module(s).")

        return MetricsEngine.run(modules, output)?.let { file ->
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
            $ transform         -in     [inputFile/inputDir]
                                -out    [outputDir]
            $ evaluate          -in     [inputFile/inputDir]
                                -out    [outputDir]
        """.trimIndent())
    }
}