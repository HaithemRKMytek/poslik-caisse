package com.poslik.caisse.domain.printing

import com.poslik.caisse.domain.fake.FakePrinter
import com.poslik.caisse.domain.fake.InMemorySaleRepository
import com.poslik.caisse.domain.fake.drainPrintQueue
import com.poslik.caisse.domain.model.PrintStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintSpoolerTest {
    private val repository = InMemorySaleRepository()
    private val printer = FakePrinter()

    @Test
    fun `prints in order and marks tickets printed`() = runTest {
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val sales = List(3) { repository.seed(PrintStatus.PENDING) }

        sales.forEach { spooler.enqueue(it.saleId) }
        drainPrintQueue()

        assertEquals(listOf("C01-000001", "C01-000002", "C01-000003"), printer.printed)
        assertTrue(repository.all.all { it.printStatus == PrintStatus.PRINTED })
    }

    @Test
    fun `printer failure marks the ticket failed with its reason`() = runTest {
        printer.failing = true
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val sale = repository.seed(PrintStatus.PENDING)

        spooler.enqueue(sale.saleId)
        drainPrintQueue()

        val stored = repository.getSale(sale.saleId)!!
        assertEquals(PrintStatus.FAILED, stored.printStatus)
        assertEquals("Plus de papier", stored.lastPrintError)
        assertEquals(1, stored.printAttempts)
    }

    @Test
    fun `printer that hangs times out as a failure`() = runTest {
        printer.delayMillis = 60_000
        val spooler = PrintSpooler(printer, repository, backgroundScope, printTimeoutMillis = 5_000)
        val sale = repository.seed(PrintStatus.PENDING)

        spooler.enqueue(sale.saleId)
        drainPrintQueue()

        assertEquals(PrintStatus.FAILED, repository.getSale(sale.saleId)!!.printStatus)
    }

    @Test
    fun `a ticket already queued is not queued twice`() = runTest {
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val sale = repository.seed(PrintStatus.PENDING)

        assertTrue(spooler.enqueue(sale.saleId))
        assertFalse(spooler.enqueue(sale.saleId))
        drainPrintQueue()

        assertEquals(1, printer.printed.size)
    }

    @Test
    fun `a printed ticket is never printed again`() = runTest {
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val sale = repository.seed(PrintStatus.PRINTED)

        spooler.enqueue(sale.saleId)
        drainPrintQueue()

        assertTrue(printer.printed.isEmpty())
    }

    @Test
    fun `every print result asks for a sync so Firebase gets the new state`() = runTest {
        var syncRequests = 0
        val spooler = PrintSpooler(printer, repository, backgroundScope, onStatusChanged = { syncRequests++ })
        val printed = repository.seed(PrintStatus.PENDING)
        spooler.enqueue(printed.saleId)
        drainPrintQueue()

        printer.failing = true
        val failed = repository.seed(PrintStatus.PENDING)
        spooler.enqueue(failed.saleId)
        drainPrintQueue()

        assertEquals(2, syncRequests)
    }
}
