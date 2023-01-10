package nl.utwente.student.app.metrics

import nl.utwente.student.io.ParserEngine
import nl.utwente.student.metamodel.v3.Module
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

abstract class UnitMetricTest(private val metric: UnitVisitor, private val resourceFile: String) {
    protected var module: Module? = null

    @BeforeEach
    fun setup() {
        if (module == null)
            module = ParserEngine.parse(File(UnitMetricTest::class.java.classLoader.getResource(resourceFile)!!.file))?.get(0)?.module
    }

    fun parseCode(code: String): Module {
        val tempFile: Path = kotlin.io.path.createTempFile(suffix = ".java")
        Files.write(tempFile, code.toByteArray(StandardCharsets.UTF_8))
        return ParserEngine.parse(tempFile.toFile())?.get(0)?.module!!
    }

    protected fun testByReference(unitName: String): Pair<String, Int> {
        return testByReference(module!!, unitName)
    }

    protected fun testByReference(module: Module, unitName: String): Pair<String, Int> {
        val unitUnderTest = module.members.find { it.identifier.value == unitName } as Unit

        metric.visitUnit(unitUnderTest)
        val result = metric.getResult().first()
        metric.reset()
        return result
    }
}