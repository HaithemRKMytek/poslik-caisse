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

        val stored = repository.getSale(sale.saleId)!!
        assertEquals(PrintStatus.FAILED, stored.printStatus)
        assertEquals(PrintFailureKind.TIMEOUT, PrintFailureKind.fromCode(stored.lastPrintError))
    }

    @Test
    fun `a classified failure is stored as its code, an unclassified one keeps its detail`() = runTest {
        var next: PrintResult = PrintResult.Failure("Bourrage papier", PrintFailureKind.OUT_OF_PAPER)
        val spooler = PrintSpooler({ next }, repository, backgroundScope)
        val classified = repository.seed(PrintStatus.PENDING)
        val unclassified = repository.seed(PrintStatus.PENDING)

        spooler.enqueue(classified.saleId)
        drainPrintQueue()
        next = PrintResult.Failure("Erreur inconnue 0x42")
        spooler.enqueue(unclassified.saleId)
        drainPrintQueue()

        assertEquals("OUT_OF_PAPER", repository.getSale(classified.saleId)!!.lastPrintError)
        assertEquals("Erreur inconnue 0x42", repository.getSale(unclassified.saleId)!!.lastPrintError)
    }

    @Test
    fun `stored codes map back to a kind and anything else falls back to unknown`() {
        assertEquals(PrintFailureKind.OUT_OF_PAPER, PrintFailureKind.fromCode("OUT_OF_PAPER"))
        assertEquals(PrintFailureKind.UNKNOWN, PrintFailureKind.fromCode("Plus de papier"))
        assertEquals(PrintFailureKind.UNKNOWN, PrintFailureKind.fromCode(null))
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
    fun `a ticket raced by two spoolers is only ever counted as printed once`() = runTest {
        // Simule deux processus qui se chevauchent (l'ancien pas encore tué, le nouveau déjà
        // démarré) : chacun a sa propre file en mémoire mais partage la même base.
        val spoolerA = PrintSpooler(printer, repository, backgroundScope)
        val spoolerB = PrintSpooler(printer, repository, backgroundScope)
        val sale = repository.seed(PrintStatus.PENDING)

        spoolerA.enqueue(sale.saleId)
        spoolerB.enqueue(sale.saleId)
        drainPrintQueue()

        assertEquals(1, repository.getSale(sale.saleId)!!.printAttempts)
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
