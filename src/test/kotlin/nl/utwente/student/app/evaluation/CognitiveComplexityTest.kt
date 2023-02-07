package nl.utwente.student.app.evaluation

import nl.utwente.student.app.metrics.UnitMetricTest
import nl.utwente.student.metrics.CognitiveComplexity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CognitiveComplexityTest : UnitMetricTest(CognitiveComplexity(), "java/ShapeStrokeParser.java") {
    @Test
    fun testConstructor() {
        val result = testByReference("ShapeStrokeParser.constructor")
        assertEquals(0, result.second)
    }

    @Test
    fun testParse() {
        val result = testByReference("parse")
        assertEquals(23, result.second)
    }
}