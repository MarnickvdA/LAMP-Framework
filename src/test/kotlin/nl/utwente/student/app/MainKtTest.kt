package nl.utwente.student.app

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream


internal class MainKtTest {

    private val outContent = ByteArrayOutputStream()
    private val errContent = ByteArrayOutputStream()
    private val originalOut = System.out
    private val originalErr = System.err

    @BeforeEach
    fun setUpStreams() {
        System.setOut(PrintStream(outContent))
        System.setErr(PrintStream(errContent))
    }

    @AfterEach
    fun restoreStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    private val main: Main = Main();

    @Test
    fun runMainWithoutArguments() {
        main.run()
        assertEquals("Hello World!\n", outContent.toString())
    }

    @Test
    fun runMainWithOneArgument() {
        main.run("Hey!")
        assertEquals("Hello World!\nProgram arguments: Hey!\n", outContent.toString())
    }
}