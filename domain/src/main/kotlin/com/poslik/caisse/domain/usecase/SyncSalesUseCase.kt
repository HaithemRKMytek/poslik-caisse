package com.poslik.caisse.domain.usecase

import com.poslik.caisse.domain.sync.PushResult
import com.poslik.caisse.domain.sync.RemoteSaleDataSource
import com.poslik.caisse.domain.sync.SaleSyncStore
import com.poslik.caisse.domain.sync.SyncDiagnostics
import javax.inject.Inject

data class SyncReport(val pushed: Int, val conflicts: Int, val failure: String?) {
    val shouldRetry: Boolean get() = failure != null
}

/**
 * Envoie vers Firebase toutes les ventes modifiées localement.
 *
 * - **Sans perte** : une vente n'est marquée synchronisée qu'après l'acquittement du serveur.
 * - **Sans doublon** : la clé distante est le numéro de ticket et l'écriture la remplace en entier ;
 *   renvoyer une vente déjà reçue est sans effet.
 * - **Sans mise à jour perdue** : si la vente change pendant l'envoi (ticket imprimé entre-temps),
 *   elle n'est pas marquée et repart au passage suivant.
 */
class SyncSalesUseCase @Inject constructor(
    private val store: SaleSyncStore,
    private val remote: RemoteSaleDataSource,
    private val diagnostics: SyncDiagnostics = SyncDiagnostics(),
) {
    suspend operator fun invoke(batchSize: Int = DEFAULT_BATCH_SIZE): SyncReport {
        diagnostics.started()
        try {
            return push(batchSize)
        } finally {
            // Aussi en cas d'annulation (relance immédiate) : l'indicateur ne doit jamais rester bloqué.
            diagnostics.finished()
        }
    }

    private suspend fun push(batchSize: Int): SyncReport {
        var pushed = 0
        var conflicts = 0
        repeat(MAX_ROUNDS) {
            val batch = store.unsyncedSales(batchSize)
            if (batch.isEmpty()) return SyncReport(pushed, conflicts, failure = null)
            for (candidate in batch) {
                when (val result = remote.push(candidate.sale)) {
                    PushResult.Success -> if (store.markSynced(candidate.sale.saleId, candidate.version)) pushed++
                    PushResult.Conflict -> {
                        store.markConflict(candidate.sale.saleId)
                        conflicts++
                    }
                    // Réseau coupé ou serveur injoignable : inutile d'insister, on réessaiera plus tard.
                    is PushResult.Failure -> return SyncReport(pushed, conflicts, result.reason)
                }
            }
        }
        // Des ventes changent plus vite qu'on ne les envoie : on laisse la main et on repasse.
        return SyncReport(pushed, conflicts, failure = "Synchronisation incomplète, nouvel essai planifié")
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
        private const val MAX_ROUNDS = 20
    }
}
