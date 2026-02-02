package com.github.ousmane_hamadou.domain.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TrustScoreTest {

    @Test
    fun `should create trust score when value is within range`() {
        val score = TrustScore(50)
        assertEquals(50, score.value)
    }

    @Test
    fun `should throw exception when value is out of bounds`() {
        assertFailsWith<IllegalArgumentException> {
            TrustScore(101)
        }
        assertFailsWith<IllegalArgumentException> {
            TrustScore(-1)
        }
    }

    @Test
    fun `should validate high reliability threshold`() {
        assertTrue(TrustScore(80).isHighReliability())
        assertFalse(TrustScore(79).isHighReliability())
    }
}