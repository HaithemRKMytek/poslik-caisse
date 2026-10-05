package com.poslik.caisse.domain.usecase

import com.poslik.caisse.domain.fake.FakePrinter
import com.poslik.caisse.domain.fake.InMemorySaleRepository
import com.poslik.caisse.domain.fake.drainPrintQueue
import com.poslik.caisse.domain.model.Cart
import com.poslik.caisse.domain.model.Catalog
import com.poslik.caisse.domain.model.Money
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.printing.PrintSpooler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutUseCaseTest {
    private val repository = InMemorySaleRepository()
    private val printer = FakePrinter()
    private var syncRequests = 0
    private val cart = Cart().add(Catalog.products[0]).add(Catalog.products[0]).add(Catalog.products[5])

    @Test
    fun `records the sale with the next number, prints it and requests a sync`() = runTest {
        val checkout = checkout(PrintSpooler(printer, repository, backgroundScope))

        val sale = checkout(cart)

        assertEquals("C01-000001", sale.ticketNumber.label)
        assertEquals(Money(2 * 2_500 + 2_200), sale.total)
        assertEquals(PrintStatus.PENDING, sale.printStatus)
        assertEquals(1_000L, sale.createdAt)
        assertEquals(1, syncRequests)

        drainPrintQueue()
        assertEquals(listOf("C01-000001"), printer.printed)
        assertEquals(PrintStatus.PRINTED, repository.getSale(sale.saleId)!!.printStatus)
    }

    @Test
    fun `returns before the printer has finished`() = runTest {
        printer.delayMillis = 30_000
        val checkout = checkout(PrintSpooler(printer, repository, backgroundScope))

        val sale = checkout(cart)

        assertEquals(PrintStatus.PENDING, repository.getSale(sale.saleId)!!.printStatus)
        assertEquals(0, testScheduler.currentTime)
    }

    @Test
    fun `concurrent checkouts get unique consecutive numbers`() = runTest {
        val checkout = checkout(PrintSpooler(printer, repository, backgroundScope))

        val numbers = List(100) { async { checkout(cart).ticketNumber.sequence } }.awaitAll()

        assertEquals((1L..100L).toList(), numbers.sorted())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty cart is refused`() = runTest {
        checkout(PrintSpooler(printer, repository, backgroundScope))(Cart())
    }

    private fun checkout(spooler: PrintSpooler) = CheckoutUseCase(repository, spooler, { syncRequests++ }, { 1_000L })
}
