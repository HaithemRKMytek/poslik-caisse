package com.poslik.caisse.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats with three decimals and a comma`() {
        assertEquals("12,500 DT", Money(12_500).format())
        assertEquals("0,050 DT", Money(50).format())
        assertEquals("0,000 DT", Money.ZERO.format())
        assertEquals("-1,200 DT", Money(-1_200).format())
    }

    @Test
    fun `adds and multiplies exactly`() {
        assertEquals(Money(7_500), Money(2_500) * 3)
        assertEquals(Money(4_000), listOf(Money(1_500), Money(2_500)).sum())
    }

    @Test(expected = ArithmeticException::class)
    fun `overflow is an error, not a wrong amount`() {
        Money(Long.MAX_VALUE) + Money(1)
    }
}
