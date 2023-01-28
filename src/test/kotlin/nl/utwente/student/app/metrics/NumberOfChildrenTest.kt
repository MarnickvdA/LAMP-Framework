package nl.utwente.student.app.metrics

import nl.utwente.student.io.TransformEngine
import nl.utwente.student.metrics.NumberOfChildren
import nl.utwente.student.visitors.SymbolVisitor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class NumberOfChildrenTest {
    var results: List<Pair<String, Int>>? = null

    @BeforeEach
    fun setup() {
        if (results == null) {
            val moduleRoots = TransformEngine.transform(File(DepthOfInheritanceTest::class.java.classLoader.getResource("java/dependencyTests")!!.file))
            val metric = NumberOfChildren()
            metric.visitProject(moduleRoots!!, SymbolVisitor().visitProject(moduleRoots))
            results = metric.getResult()
        }
    }

    @Test
    fun testNumberOfChildrenOfA() {
        val result = results!!.find { it.first.endsWith("A") }!!
        assertEquals(2, result.second)
    }

    @Test
    fun testNumberOfChildrenOfB() {
        val result = results!!.find { it.first.endsWith("B") }!!
        assertEquals(0, result.second)
    }

    @Test
    fun testNumberOfChildrenOfC() {
        val result = results!!.find { it.first.endsWith("C") }!!
        assertEquals(1, result.second)
    }

    @Test
    fun testNumberOfChildrenOfD() {
        val result = results!!.find { it.first.endsWith("D") }!!
        assertEquals(1, result.second)
    }

    @Test
    fun testNumberOfChildrenOfE() {
        val result = results!!.find { it.first.endsWith("E") }!!
        assertEquals(0, result.second)
    }
}