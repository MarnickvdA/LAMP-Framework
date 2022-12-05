package nl.utwente.student.app

fun main(args: Array<String>) {
    Main().run(*args)
}

internal class Main {
    fun run(vararg args: String) {
        println("Hello World!")

        // Try adding program arguments via Run/Debug configuration.
        // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
        if (args.isNotEmpty()) println("Program arguments: ${args.joinToString()}")
    }
}

class X