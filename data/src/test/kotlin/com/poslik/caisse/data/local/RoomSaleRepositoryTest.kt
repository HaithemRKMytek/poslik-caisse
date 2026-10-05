package com.poslik.caisse.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.poslik.caisse.domain.model.Money
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.SaleLine
import com.poslik.caisse.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoomSaleRepositoryTest {
    private lateinit var database: CaisseDatabase
    private lateinit var repository: RoomSaleRepository

    private val espresso = SaleLine("espresso", "Café express", Money(2_500), 2)
    private val croissant = SaleLine("croissant", "Croissant", Money(2_200), 1)

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaisseDatabase::class.java).build()
        database.registerDao().insert(RegisterConfigEntity(registerCode = "C01", lastNumber = 0))
        repository = RoomSaleRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `records the sale with its lines, total and the next number`() = runTest {
        val sale = repository.recordSale(listOf(espresso, croissant), createdAt = 1_000)

        assertEquals("C01-000001", sale.ticketNumber.label)
        assertEquals(Money(7_200), sale.total)
        assertEquals(listOf(espresso, croissant), sale.lines)
        assertEquals(PrintStatus.PENDING, sale.printStatus)
        assertEquals(SyncStatus.PENDING, sale.syncStatus)
    }

    @Test
    fun `100 concurrent checkouts get exactly the numbers 1 to 100`() = runTest {
        val numbers = (1..100).map {
            async(Dispatchers.IO) { repository.recordSale(listOf(espresso), createdAt = 0).ticketNumber.sequence }
        }.awaitAll()

        assertEquals((1L..100L).toList(), numbers.sorted())
        assertEquals(100L, database.registerDao().get()!!.lastNumber)
    }

    @Test
    fun `a failed insert rolls back the counter, so no number is lost`() = runTest {
        repository.recordSale(listOf(espresso), createdAt = 0)

        try {
            // Deux lignes pour le même produit violent la clé primaire de sale_lines.
            repository.recordSale(listOf(espresso, espresso), createdAt = 0)
            fail("L'insertion aurait dû échouer")
        } catch (expected: SQLiteConstraintException) {
            // attendu
        }
        val next = repository.recordSale(listOf(croissant), createdAt = 0)

        assertEquals(2L, next.ticketNumber.sequence)
        assertEquals(2, repository.observeSales().first().size)
    }

    @Test
    fun `sales to print are pending and failed ones only, in ticket order`() = runTest {
        val printed = repository.recordSale(listOf(espresso), 0)
        val failed = repository.recordSale(listOf(espresso), 0)
        val pending = repository.recordSale(listOf(espresso), 0)
        repository.updatePrintStatus(printed.saleId, PrintStatus.PRINTED)
        repository.updatePrintStatus(failed.saleId, PrintStatus.FAILED, "Plus de papier")

        val toPrint = repository.salesToPrint()

        assertEquals(listOf(failed.saleId, pending.saleId), toPrint.map { it.saleId })
        assertEquals("Plus de papier", toPrint.first().lastPrintError)
        assertEquals(1, toPrint.first().printAttempts)
    }

    @Test
    fun `marking synced fails when the sale changed during the push`() = runTest {
        val sale = repository.recordSale(listOf(espresso), 0)
        val candidate = repository.unsyncedSales(limit = 10).single()

        repository.updatePrintStatus(sale.saleId, PrintStatus.PRINTED)

        assertFalse(repository.markSynced(sale.saleId, candidate.version))
        val retry = repository.unsyncedSales(limit = 10).single()
        assertEquals(PrintStatus.PRINTED, retry.sale.printStatus)
        assertTrue(repository.markSynced(sale.saleId, retry.version))
        assertTrue(repository.unsyncedSales(limit = 10).isEmpty())
        assertEquals(0, repository.observeUnsyncedCount().first())
    }

    @Test
    fun `conflicting sales are left out of the sync queue`() = runTest {
        val sale = repository.recordSale(listOf(espresso), 0)

        repository.markConflict(sale.saleId)

        assertTrue(repository.unsyncedSales(limit = 10).isEmpty())
        assertEquals(SyncStatus.CONFLICT, repository.getSale(sale.saleId)!!.syncStatus)
    }

    @Test
    fun `recording without a configured register is refused`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val empty = Room.inMemoryDatabaseBuilder(context, CaisseDatabase::class.java).build()
        try {
            RoomSaleRepository(empty).recordSale(listOf(espresso), 0)
            fail("La vente aurait dû être refusée")
        } catch (expected: IllegalStateException) {
            assertNull(empty.registerDao().get())
        } finally {
            empty.close()
        }
    }
}
