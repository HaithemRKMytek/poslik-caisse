package com.poslik.caisse.data.local

import androidx.room.withTransaction
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SaleLine
import com.poslik.caisse.domain.model.sum
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.sync.SaleSyncStore
import com.poslik.caisse.domain.sync.SyncCandidate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomSaleRepository @Inject constructor(private val database: CaisseDatabase) :
    SaleRepository,
    SaleSyncStore {

    private val saleDao = database.saleDao()
    private val registerDao = database.registerDao()

    /**
     * Lecture du compteur, incrément, insertion de la vente et de ses lignes : une seule transaction
     * SQLite. Room sérialise les transactions, donc deux encaissements simultanés ne peuvent pas lire
     * le même compteur, et un échec (disque plein, crash) annule tout : ni numéro perdu, ni vente orpheline.
     */
    override suspend fun recordSale(lines: List<SaleLine>, createdAt: Long): Sale = database.withTransaction {
        val register = checkNotNull(registerDao.get()) { "Caisse non configurée" }
        val number = register.lastNumber + 1
        registerDao.updateLastNumber(number)

        val saleId = UUID.randomUUID().toString()
        saleDao.insertSale(
            SaleEntity(
                saleId = saleId,
                registerCode = register.registerCode,
                ticketNumber = number,
                totalMillimes = lines.map { it.subtotal }.sum().millimes,
                createdAt = createdAt,
                printStatus = PrintStatus.PENDING,
                printAttempts = 0,
                lastPrintError = null,
                version = 1,
                syncedVersion = 0,
                syncConflict = false,
            ),
        )
        saleDao.insertLines(lines.mapIndexed { index, line -> line.toEntity(saleId, index) })
        checkNotNull(saleDao.get(saleId)).toDomain()
    }

    override suspend fun getSale(saleId: String): Sale? = saleDao.get(saleId)?.toDomain()

    override fun observeSales(): Flow<List<Sale>> = saleDao.observeAll().map { sales -> sales.map { it.toDomain() } }

    override suspend fun salesToPrint(): List<Sale> = saleDao.salesToPrint().map { it.toDomain() }

    override suspend fun updatePrintStatus(saleId: String, status: PrintStatus, error: String?) {
        // Un retour en attente (réimpression demandée) n'est pas une tentative d'impression.
        val attemptIncrement = if (status == PrintStatus.PENDING) 0 else 1
        saleDao.updatePrintStatus(saleId, status, error, attemptIncrement)
    }

    override fun observeUnsyncedCount(): Flow<Int> = saleDao.observeUnsyncedCount()

    override suspend fun unsyncedSales(limit: Int): List<SyncCandidate> =
        saleDao.unsynced(limit).map { SyncCandidate(it.toDomain(), it.sale.version) }

    override suspend fun markSynced(saleId: String, version: Long): Boolean = saleDao.markSynced(saleId, version) == 1

    override suspend fun markConflict(saleId: String) = saleDao.markConflict(saleId)
}
