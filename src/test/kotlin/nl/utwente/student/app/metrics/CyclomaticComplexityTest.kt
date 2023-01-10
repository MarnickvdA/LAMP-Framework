package nl.utwente.student.app.metrics

import nl.utwente.student.metrics.CyclomaticComplexity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class CyclomaticComplexityTest : UnitMetricTest(CyclomaticComplexity(),"java/ComplexityTests.java") {
    @Test
    fun testLogicalSequenceExample1() {
        val result = testByReference("logicalSequenceExample1")
        assertEquals(7, result.second)
    }

    @Test
    fun testLogicalSequenceExample2() {
        val result = testByReference("logicalSequenceExample2")
        assertEquals(4, result.second)
    }

    @Test
    fun myMethod() {
        val result = testByReference("myMethod")
        assertEquals(6, result.second)
    }

    @Test
    fun myMethod2() {
        val result = testByReference("myMethod2")
        assertEquals(2, result.second)
    }

    @Test
    fun sumOfPrimes() {
        val result = testByReference("sumOfPrimes")
        assertEquals(4, result.second)
    }

    @Test
    fun getWords() {
        val result = testByReference("getWords")
        assertEquals(4, result.second)
    }

    @Test
    fun addVersion() {
        val result = testByReference("addVersion")
        assertEquals(14, result.second)
    }

    @Test
    fun overriddenSymbolFrom() {
        val result = testByReference("overriddenSymbolFrom")
        assertEquals(10, result.second)
    }

    @Test
    fun toRegexp() {
        val result = testByReference("toRegexp")
        assertEquals(15, result.second)
    }

    @Test
    fun testCase1() {
        val result = testByReference(
            parseCode(
                """
            class ComplexFunction {
                int fn1(int n) {
                    final int k = 4;
                    var r = switch (n) {
                        case 1, 2, 3 + 3, k, C, SC1.C -> 3 + SC1.C;
                        case 20 -> 3 + 4 + C - k;
                        case 21 -> {
                            int ff = 222;
                            yield ff;
                        }
                        case 22 -> {
                            yield 33 + 3;
                        }
                        case 99 -> {
                            throw new RuntimeException("");
                        }
                        default -> 0;
                    };
                    return r;
                }
            }
        """.trimIndent()
            ), "fn1"
        )

        assertEquals(6, result.second)
    }
}