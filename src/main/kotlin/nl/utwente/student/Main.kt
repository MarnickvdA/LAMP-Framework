package nl.utwente.student

import nl.utwente.student.app.App

fun main(args: Array<String>) {
    if (args[0] == "-tui") {
        while (true) {
            try {
                print("$ ")
                val userInput = readln()
                App.execute(userInput)
            } catch (ex: Exception) {
                println(ex.message)
            }
        }
    } else {
        App.execute(args.joinToString(" "))
    }
}