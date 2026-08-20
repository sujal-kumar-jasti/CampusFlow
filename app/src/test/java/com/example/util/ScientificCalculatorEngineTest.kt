package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ScientificCalculatorEngineTest {

    @Test
    fun testCombinations() {
        // 2c2 = 1
        assertEquals("1", ScientificCalculatorEngine.evaluate("2c2"))
        // 5c2 = 10
        assertEquals("10", ScientificCalculatorEngine.evaluate("5c2"))
        // case insensitive
        assertEquals("1", ScientificCalculatorEngine.evaluate("2C2"))
    }

    @Test
    fun testPermutations() {
        // 2p2 = 2
        assertEquals("2", ScientificCalculatorEngine.evaluate("2p2"))
        // 5p2 = 20
        assertEquals("20", ScientificCalculatorEngine.evaluate("5p2"))
        // 32p2 = 992
        assertEquals("992", ScientificCalculatorEngine.evaluate("32p2"))
        // case insensitive
        assertEquals("2", ScientificCalculatorEngine.evaluate("2P2"))
    }

    @Test
    fun testParentheses() {
        // (5)c(2) = 10
        assertEquals("10", ScientificCalculatorEngine.evaluate("(5)c(2)"))
        // (2+3)c2 = 10
        assertEquals("10", ScientificCalculatorEngine.evaluate("(2+3)c2"))
    }

    @Test
    fun testMixedExpressions() {
        // 2c2 + 1 = 2
        assertEquals("2", ScientificCalculatorEngine.evaluate("2c2 + 1"))
        // 2 * 3c2 = 6
        assertEquals("6", ScientificCalculatorEngine.evaluate("2 * 3c2"))
    }
}
