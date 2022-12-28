package nl.utwente.student.app

import nl.utwente.student.parsers.JavaProjectParserV2
import nl.utwente.student.parsers.MetamodelProjectParser

fun main(args: Array<String>) {
    Main().run(*args)
}

internal class Main {
    fun run(vararg args: String) {
        if (args.size < 2) {
            return println("Program terminated without results. Try --path or --projectUrl as program arguments.")
        }

        JavaProjectParserV2().executeWithArgs(args)

        val modules = MetamodelProjectParser().readProjectFromDirectory("out/input")
        MetricRunner(modules).run()
    }
}
