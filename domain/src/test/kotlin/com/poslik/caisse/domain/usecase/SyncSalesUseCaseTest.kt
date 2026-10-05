package com.poslik.caisse.domain.usecase

import com.poslik.caisse.domain.fake.InMemorySaleRepository
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SyncStatus
import com.poslik.caisse.domain.sync.PushResult
import com.poslik.caisse.domain.sync.RemoteSaleDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSalesUseCaseTest {
    private val repository = InMemorySaleRepository()

    /** Simule Firebase : un nœud par numéro de ticket, écriture = remplacement. */
    private class FakeRemote : RemoteSaleDataSource {
        val nodes = mutableMapOf<String, Sale>()
        var online = true
        var writes = 0
        var beforeAck: (suspend (Sale) -> Unit)? = null

        override suspend fun push(sale: Sale): PushResult {
            if (!online) return PushResult.Failure("Hors ligne")
            val existing = nodes[sale.ticketNumber.label]
            if (existing != null && existing.saleId != sale.saleId) return PushResult.Conflict
            nodes[sale.ticketNumber.label] = sale
            writes++
            beforeAck?.invoke(sale)
            return PushResult.Success
        }
    }

    private val remote = FakeRemote()
    private val sync = SyncSalesUseCase(repository, remote)

    @Test
    fun `offline sales are kept and pushed once the network is back`() = runTest {
        remote.online = false
        repeat(3) { repository.seed(PrintStatus.PENDING) }

        val offline = sync()
        assertTrue(offline.shouldRetry)
        assertEquals(3, repository.observeUnsyncedCount().first())

        remote.online = true
        val online = sync()

        assertNull(online.failure)
        assertEquals(3, online.pushed)
        assertEquals(setOf("C01-000001", "C01-000002", "C01-000003"), remote.nodes.keys)
        assertEquals(0, repository.observeUnsyncedCount().first())
    }

    @Test
    fun `syncing again never creates duplicates`() = runTest {
        repeat(2) { repository.seed(PrintStatus.PENDING) }

        sync()
        sync()
        sync()

        assertEquals(2, remote.nodes.size)
        assertEquals(2, remote.writes)
    }

    @Test
    fun `a retry after a lost acknowledgement rewrites the same node`() = runTest {
        val sale = repository.seed(PrintStatus.PENDING)
        // Firebase a reçu la vente mais l'app n'a pas pu la marquer (process tué avant l'acquittement).
        remote.nodes[sale.ticketNumber.label] = sale

        sync()

        assertEquals(1, remote.nodes.size)
        assertEquals(SyncStatus.SYNCED, repository.getSale(sale.saleId)!!.syncStatus)
    }

    @Test
    fun `a print status change during the push is sent on the next pass`() = runTest {
        val sale = repository.seed(PrintStatus.PENDING)
        remote.beforeAck = {
            remote.beforeAck = null // l'impression se termine pendant le premier envoi seulement
            repository.updatePrintStatus(it.saleId, PrintStatus.PRINTED)
        }

        sync()

        assertEquals(PrintStatus.PRINTED, remote.nodes.getValue(sale.ticketNumber.label).printStatus)
        assertEquals(SyncStatus.SYNCED, repository.getSale(sale.saleId)!!.syncStatus)
        assertEquals(2, remote.writes)
    }

    @Test
    fun `a different sale under the same number is flagged, never overwritten`() = runTest {
        val sale = repository.seed(PrintStatus.PENDING)
        val intruder = sale.copy(saleId = "autre-tablette")
        remote.nodes[sale.ticketNumber.label] = intruder

        val report = sync()

        assertEquals(1, report.conflicts)
        assertEquals("autre-tablette", remote.nodes.getValue(sale.ticketNumber.label).saleId)
        assertEquals(SyncStatus.CONFLICT, repository.getSale(sale.saleId)!!.syncStatus)
    }
}
