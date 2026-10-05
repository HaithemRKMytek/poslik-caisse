package com.poslik.caisse.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketNumberTest {

    @Test
    fun `label and key are zero padded`() {
        val number = TicketNumber(RegisterCode("C01"), 42)

        assertEquals("C01-000042", number.label)
        assertEquals("000042", number.key)
    }

    @Test
    fun `keys sort in numeric order`() {
        val keys = listOf(10L, 9L, 100L).map { TicketNumber(RegisterCode("C01"), it).key }

        assertEquals(listOf("000009", "000010", "000100"), keys.sorted())
    }

    @Test
    fun `two registers never produce the same label`() {
        val a = TicketNumber(RegisterCode("C01"), 7)
        val b = TicketNumber(RegisterCode("C02"), 7)

        assertTrue(a.label != b.label)
    }

    @Test
    fun `register code input is normalized or rejected`() {
        assertEquals(RegisterCode("C01"), RegisterCode.parse(" c01 "))
        assertNull(RegisterCode.parse("C"))
        assertNull(RegisterCode.parse("C-01"))
        assertNull(RegisterCode.parse("CAISSE01"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sequence must be positive`() {
        TicketNumber(RegisterCode("C01"), 0)
    }
}
