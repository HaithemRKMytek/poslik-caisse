package com.poslik.caisse.domain.usecase

import com.poslik.caisse.domain.fake.FakePrinter
import com.poslik.caisse.domain.fake.InMemorySaleRepository
import com.poslik.caisse.domain.fake.drainPrintQueue
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.printing.PrintSpooler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintUseCasesTest {
    private val repository = InMemorySaleRepository()
    private val printer = FakePrinter()

    @Test
    fun `startup reprints pending and failed tickets, never printed ones`() = runTest {
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        repository.seed(PrintStatus.PRINTED) // C01-000001
        repository.seed(PrintStatus.PENDING) // C01-000002
        repository.seed(PrintStatus.FAILED) // C01-000003
        repository.seed(PrintStatus.PRINTED) // C01-000004

        val queued = ResumePendingPrintsUseCase(repository, spooler)()
        drainPrintQueue()

        assertEquals(2, queued)
        assertEquals(listOf("C01-000002", "C01-000003"), printer.printed)
        assertTrue(repository.all.all { it.printStatus == PrintStatus.PRINTED })
    }

    @Test
    fun `startup with a broken printer leaves tickets failed for the next start`() = runTest {
        printer.failing = true
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val sale = repository.seed(PrintStatus.FAILED)

        ResumePendingPrintsUseCase(repository, spooler)()
        drainPrintQueue()

        val stored = repository.getSale(sale.saleId)!!
        assertEquals(PrintStatus.FAILED, stored.printStatus)
        assertEquals(2, stored.printAttempts)
    }

    @Test
    fun `manual reprint only applies to failed tickets`() = runTest {
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val printed = repository.seed(PrintStatus.PRINTED)
        val failed = repository.seed(PrintStatus.FAILED)
        val reprint = ReprintFailedTicketUseCase(repository, spooler)

        assertFalse(reprint(printed.saleId))
        assertTrue(reprint(failed.saleId))
        drainPrintQueue()

        assertEquals(listOf(failed.ticketNumber.label), printer.printed)
    }
}
