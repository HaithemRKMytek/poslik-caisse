package com.poslik.caisse.domain.repository

import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SaleLine
import kotlinx.coroutines.flow.Flow

interface SaleRepository {

    /**
     * Attribue le prochain numéro de ticket et enregistre la vente en une seule transaction :
     * soit les deux sont écrits, soit aucun. La vente est créée en [PrintStatus.PENDING].
     *
     * @throws IllegalStateException si la caisse n'est pas encore configurée.
     */
    suspend fun recordSale(lines: List<SaleLine>, createdAt: Long): Sale

    suspend fun getSale(saleId: String): Sale?

    /** Toutes les ventes, du ticket le plus récent au plus ancien. */
    fun observeSales(): Flow<List<Sale>>

    /** Tickets à (ré)imprimer au démarrage : en attente ou en échec, par numéro croissant. */
    suspend fun salesToPrint(): List<Sale>

    /**
     * Change l'état d'impression ; toute modification redemande une synchronisation.
     * @return false si le ticket était déjà imprimé (transition ignorée, jamais de double impression).
     */
    suspend fun updatePrintStatus(saleId: String, status: PrintStatus, error: String? = null): Boolean

    /** Nombre de ventes pas encore (ou plus) synchronisées. */
    fun observeUnsyncedCount(): Flow<Int>
}
