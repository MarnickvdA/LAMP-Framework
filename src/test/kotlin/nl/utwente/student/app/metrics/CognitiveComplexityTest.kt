package nl.utwente.student.app.metrics

import nl.utwente.student.metrics.CognitiveComplexity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CognitiveComplexityTest : UnitMetricTest(CognitiveComplexity(), "java/ComplexityTests.java") {
    @Test
    fun testLogicalSequenceExample1() {
        val result = testByReference("logicalSequenceExample1")
        assertEquals(4, result.second)
    }

    @Test
    fun testLogicalSequenceExample2() {
        val result = testByReference("logicalSequenceExample2")
        assertEquals(3, result.second)
    }

    @Test
    fun myMethod() {
        val result = testByReference("myMethod")
        assertEquals(9, result.second)
    }

    @Test
    fun myMethod2() {
        val result = testByReference("myMethod2")
        assertEquals(2, result.second)
    }

    @Test
    fun sumOfPrimes() {
        val result = testByReference("sumOfPrimes")
        assertEquals(7, result.second)
    }

    @Test
    fun getWords() {
        val result = testByReference("getWords")
        assertEquals(1, result.second)
    }

    @Test
    fun addVersion() {
        val result = testByReference("addVersion")
        assertEquals(35, result.second)
    }

    @Test
    fun overriddenSymbolFrom() {
        val result = testByReference("overriddenSymbolFrom")
        assertEquals(19, result.second)
    }

    @Test
    fun toRegexp() {
        val result = testByReference("toRegexp")
        assertEquals(20, result.second)
    }
}