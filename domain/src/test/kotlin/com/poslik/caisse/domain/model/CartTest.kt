package com.poslik.caisse.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTest {
    private val espresso = Catalog.byId("espresso")!!
    private val croissant = Catalog.byId("croissant")!!

    @Test
    fun `catalog has eight products with unique ids`() {
        assertEquals(8, Catalog.products.size)
        assertEquals(8, Catalog.products.map { it.id }.toSet().size)
    }

    @Test
    fun `adding the same product increments its quantity and keeps order`() {
        val cart = Cart().add(espresso).add(croissant).add(espresso)

        assertEquals(listOf("espresso", "croissant"), cart.lines.map { it.product.id })
        assertEquals(2, cart.lines.first().quantity)
        assertEquals(3, cart.itemCount)
        assertEquals(Money(2 * 2_500 + 2_200), cart.total)
    }

    @Test
    fun `a line quantity is capped`() {
        val cart = (1..Cart.MAX_LINE_QUANTITY + 5).fold(Cart()) { acc, _ -> acc.add(espresso) }

        assertEquals(Cart.MAX_LINE_QUANTITY, cart.lines.single().quantity)
    }

    @Test
    fun `removing the last unit removes the line`() {
        val cart = Cart().add(espresso).add(espresso).removeOne("espresso").removeOne("espresso")

        assertTrue(cart.isEmpty)
        assertEquals(Money.ZERO, cart.total)
    }

    @Test
    fun `sale lines copy name and price at the time of sale`() {
        val lines = Cart().add(croissant).add(croissant).toSaleLines()

        assertEquals(listOf(SaleLine("croissant", "Croissant", Money(2_200), 2)), lines)
    }
}
