package com.poslik.caisse.domain.sync

import com.poslik.caisse.domain.model.Sale

/** Une vente à envoyer, avec la version locale qui sera marquée comme synchronisée. */
data class SyncCandidate(val sale: Sale, val version: Long)

/** Côté local de la synchronisation (implémenté par Room). */
interface SaleSyncStore {
    /** Ventes dont la version locale n'a pas encore été envoyée, hors conflits, par numéro croissant. */
    suspend fun unsyncedSales(limit: Int): List<SyncCandidate>

    /**
     * Marque la vente synchronisée **seulement si** sa version n'a pas changé pendant l'envoi.
     * @return false si la vente a été modifiée entre-temps (elle repartira au prochain passage).
     */
    suspend fun markSynced(saleId: String, version: Long): Boolean

    suspend fun markConflict(saleId: String)
}

sealed interface PushResult {
    data object Success : PushResult

    /** Un autre ticket (autre saleId) existe déjà sous ce numéro dans Firebase. */
    data object Conflict : PushResult

    data class Failure(val reason: String) : PushResult
}

/** Côté distant (implémenté par Firebase). L'écriture doit être idempotente. */
fun interface RemoteSaleDataSource {
    suspend fun push(sale: Sale): PushResult
}

/** Demande une synchronisation dès que le réseau est disponible (implémenté par WorkManager). */
fun interface SyncScheduler {
    fun requestSync()
}

/**
 * Lance une synchronisation immédiate, en ignorant le délai de reprise après échec
 * (bouton « Synchroniser » et retour du réseau). Implémenté par WorkManager.
 */
fun interface SyncNowRequester {
    fun syncNow()
}
