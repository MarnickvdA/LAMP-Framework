package nl.utwente.student.app.metrics

import nl.utwente.student.io.TransformEngine
import nl.utwente.student.metrics.DepthOfInheritanceTree
import nl.utwente.student.visitors.SymbolVisitor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class DepthOfInheritanceTest {
    var results: List<Pair<String, Int>>? = null

    @BeforeEach
    fun setup() {
        if (results == null) {
            val moduleRoots = TransformEngine.transform(File(DepthOfInheritanceTest::class.java.classLoader.getResource("java/dependencyTests")!!.file))
            val metric = DepthOfInheritanceTree()
            metric.visitProject(moduleRoots!!, SymbolVisitor().visitProject(moduleRoots))
            results = metric.getResult()
        }
    }

    @Test
    fun testDepthOfA() {
        val result = results!!.find { it.first.endsWith("A") }!!
        assertEquals(0, result.second)
    }

    @Test
    fun testDepthOfB() {
        val result = results!!.find { it.first.endsWith("B") }!!
        assertEquals(1, result.second)
    }

    @Test
    fun testDepthOfC() {
        val result = results!!.find { it.first.endsWith("C") }!!
        assertEquals(1, result.second)
    }

    @Test
    fun testDepthOfD() {
        val result = results!!.find { it.first.endsWith("D") }!!
        assertEquals(2, result.second)
    }

    @Test
    fun testDepthOfE() {
        val result = results!!.find { it.first.endsWith("E") }!!
        assertEquals(3, result.second)
    }
}