package com.poslik.caisse.domain.fake

import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SaleLine
import com.poslik.caisse.domain.model.SyncStatus
import com.poslik.caisse.domain.model.TicketNumber
import com.poslik.caisse.domain.model.sum
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.sync.SaleSyncStore
import com.poslik.caisse.domain.sync.SyncCandidate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Double de test qui reproduit les règles de la base Room (transaction, versions). */
class InMemorySaleRepository(private val registerCode: RegisterCode = RegisterCode("C01")) :
    SaleRepository,
    SaleSyncStore {

    private data class Row(val sale: Sale, val version: Long, val syncedVersion: Long, val conflict: Boolean)

    private val mutex = Mutex()
    private var lastNumber = 0L
    private val rows = MutableStateFlow<Map<String, Row>>(emptyMap())

    val all: List<Sale> get() = rows.value.values.map { it.toSale() }.sortedBy { it.ticketNumber.sequence }

    override suspend fun recordSale(lines: List<SaleLine>, createdAt: Long): Sale = mutex.withLock {
        val sale = Sale(
            saleId = UUID.randomUUID().toString(),
            ticketNumber = TicketNumber(registerCode, ++lastNumber),
            lines = lines,
            total = lines.map { it.subtotal }.sum(),
            createdAt = createdAt,
            printStatus = PrintStatus.PENDING,
            printAttempts = 0,
            lastPrintError = null,
            syncStatus = SyncStatus.PENDING,
        )
        rows.update { it + (sale.saleId to Row(sale, version = 1, syncedVersion = 0, conflict = false)) }
        sale
    }

    /** Insère une vente dans un état donné (préparation des tests). */
    suspend fun seed(status: PrintStatus): Sale {
        val sale = recordSale(emptyList(), createdAt = 0)
        if (status != PrintStatus.PENDING) updatePrintStatus(sale.saleId, status)
        return getSale(sale.saleId)!!
    }

    override suspend fun getSale(saleId: String): Sale? = rows.value[saleId]?.toSale()

    override fun observeSales(): Flow<List<Sale>> =
        rows.map { map -> map.values.map { it.toSale() }.sortedByDescending { it.ticketNumber.sequence } }

    override suspend fun salesToPrint(): List<Sale> = all.filter { it.printStatus != PrintStatus.PRINTED }

    override suspend fun updatePrintStatus(saleId: String, status: PrintStatus, error: String?): Boolean {
        var applied = false
        rows.update { map ->
            val row = map[saleId] ?: return@update map
            // Même garde que Room : un ticket déjà imprimé n'est plus jamais modifié.
            if (row.sale.printStatus == PrintStatus.PRINTED) return@update map
            applied = true
            val attempts = row.sale.printAttempts + if (status == PrintStatus.PENDING) 0 else 1
            val sale = row.sale.copy(printStatus = status, lastPrintError = error, printAttempts = attempts)
            map + (saleId to row.copy(sale = sale, version = row.version + 1))
        }
        return applied
    }

    override fun observePrintFailureCount(): Flow<Int> = rows.map { map -> map.values.count { it.sale.printStatus == PrintStatus.FAILED } }

    override fun observeUnsyncedCount(): Flow<Int> = rows.map { map -> map.values.count { it.syncedVersion < it.version } }

    override suspend fun unsyncedSales(limit: Int): List<SyncCandidate> = rows.value.values
        .filter { it.syncedVersion < it.version && !it.conflict }
        .sortedBy { it.sale.ticketNumber.sequence }
        .take(limit)
        .map { SyncCandidate(it.toSale(), it.version) }

    override suspend fun markSynced(saleId: String, version: Long): Boolean {
        var updated = false
        rows.update { map ->
            val row = map[saleId]
            if (row == null || row.version != version) {
                map
            } else {
                updated = true
                map + (saleId to row.copy(syncedVersion = version))
            }
        }
        return updated
    }

    override suspend fun markConflict(saleId: String) {
        rows.update { map -> map[saleId]?.let { map + (saleId to it.copy(conflict = true)) } ?: map }
    }

    private fun Row.toSale(): Sale = sale.copy(
        syncStatus = when {
            conflict -> SyncStatus.CONFLICT
            syncedVersion >= version -> SyncStatus.SYNCED
            else -> SyncStatus.PENDING
        },
    )
}
