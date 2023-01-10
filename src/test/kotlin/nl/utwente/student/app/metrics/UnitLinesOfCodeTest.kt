package nl.utwente.student.app.metrics

import nl.utwente.student.metrics.UnitLinesOfCode
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals

class UnitLinesOfCodeTest: UnitMetricTest(UnitLinesOfCode(), "java/ComplexityTests.java") {
    @Test
    fun testLogicalSequenceExample1() {
        val result = testByReference("logicalSequenceExample1")
        assertEquals(8, result.second)
    }

    @Test
    @Ignore("UnitLinesOfCodeTest.testLogicalSequenceExample2(): Binary operator on a new line isn't covered by our metamodel, sadly, so ignoring this case.")
    fun testLogicalSequenceExample2() {
        val result = testByReference("logicalSequenceExample2")
        assertEquals(7, result.second)
    }

    @Test
    fun myMethod() {
        val result = testByReference("myMethod")
        assertEquals(13, result.second)
    }

    @Test
    fun myMethod2() {
        val result = testByReference("myMethod2")
        assertEquals(6, result.second)
    }

    @Test
    fun sumOfPrimes() {
        val result = testByReference("sumOfPrimes")
        assertEquals(13, result.second)
    }

    @Test
    fun getWords() {
        val result = testByReference("getWords")
        assertEquals(12, result.second)
    }

    @Test
    fun addVersion() {
        val result = testByReference("addVersion")
        assertEquals(44, result.second)
    }

    @Test
    fun overriddenSymbolFrom() {
        val result = testByReference("overriddenSymbolFrom")
        assertEquals(27, result.second)
    }

    @Test
    fun toRegexp() {
        val result = testByReference("toRegexp")
        assertEquals(36, result.second)
    }
}