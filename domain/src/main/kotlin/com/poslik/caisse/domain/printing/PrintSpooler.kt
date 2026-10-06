package com.poslik.caisse.domain.printing

import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.repository.SaleRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * File d'impression : un seul consommateur traite les tickets dans l'ordre d'arrivée,
 * comme une vraie imprimante qui n'imprime qu'un ticket à la fois.
 *
 * - [enqueue] ne bloque jamais : l'encaissement rend la main immédiatement.
 * - Un ticket déjà dans la file n'y est pas ajouté une seconde fois (démarrage + réimpression).
 * - L'état est relu en base avant impression : un ticket déjà imprimé n'est jamais réimprimé.
 * - Le résultat (imprimé / échec) est écrit en base, source de vérité de l'historique, puis
 *   [onStatusChanged] est appelé pour que Firebase reçoive le nouvel état.
 */
class PrintSpooler(
    private val printer: TicketPrinter,
    private val repository: SaleRepository,
    scope: CoroutineScope,
    private val printTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val onStatusChanged: () -> Unit = {},
) {
    private val queue = Channel<String>(Channel.UNLIMITED)
    private val inQueue = ConcurrentHashMap.newKeySet<String>()

    init {
        scope.launch {
            for (saleId in queue) {
                try {
                    process(saleId)
                } finally {
                    inQueue.remove(saleId)
                }
            }
        }
    }

    /** @return false si le ticket était déjà en file. */
    fun enqueue(saleId: String): Boolean {
        if (!inQueue.add(saleId)) return false
        queue.trySend(saleId)
        return true
    }

    private suspend fun process(saleId: String) {
        val sale = repository.getSale(saleId) ?: return
        if (sale.printStatus == PrintStatus.PRINTED) return

        val result = try {
            withTimeout(printTimeoutMillis) { printer.print(sale) }
        } catch (e: TimeoutCancellationException) {
            PrintResult.Failure("Délai d'impression dépassé")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PrintResult.Failure(e.message ?: e::class.simpleName.orEmpty())
        }

        when (result) {
            PrintResult.Success -> repository.updatePrintStatus(saleId, PrintStatus.PRINTED)
            is PrintResult.Failure -> repository.updatePrintStatus(saleId, PrintStatus.FAILED, result.reason)
        }
        onStatusChanged()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
    }
}
