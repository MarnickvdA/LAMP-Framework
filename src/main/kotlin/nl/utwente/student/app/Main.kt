package nl.utwente.student.app

import nl.utwente.student.parsers.JavaProjectParserV2

fun main(args: Array<String>) {
    Main().run(*args)
}

internal class Main {
    fun run(vararg args: String) {
        if (args.size < 2) {
            return println("Program terminated without results. Try --path or --projectUrl as program arguments.")
        }

//        JavaProjectParserV1().executeWithArgs(args)
        JavaProjectParserV2().executeWithArgs(args)
    }
}
