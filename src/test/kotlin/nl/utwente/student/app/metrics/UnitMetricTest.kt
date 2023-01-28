package nl.utwente.student.app.metrics

import nl.utwente.student.io.TransformEngine
import nl.utwente.student.metamodel.v3.Module
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.visitors.UnitVisitor
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

abstract class UnitMetricTest(private val metric: UnitVisitor, private val resourceFile: String) {
    var moduleRoot: ModuleRoot? = null
        set(value) {
            field = value
            metric.visitModuleRoot(moduleRoot)
        }


    @BeforeEach
    fun setup() {
        if (moduleRoot == null) {
            moduleRoot = TransformEngine.transform(File(UnitMetricTest::class.java.classLoader.getResource(resourceFile)!!.file))?.first()
        }
    }

    fun parseCode(code: String) {
        val tempFile: Path = kotlin.io.path.createTempFile(suffix = ".java")
        Files.write(tempFile, code.toByteArray(StandardCharsets.UTF_8))
        moduleRoot = TransformEngine.transform(tempFile.toFile())?.first()!!
    }

    protected fun testByReference(unitName: String): Pair<String, Int> {
        return metric.getResult().find { it.first.split("$").first().endsWith(unitName) }!!
    }
}