package com.poslik.caisse.domain.usecase

import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.printing.PrintSpooler
import com.poslik.caisse.domain.repository.SaleRepository
import javax.inject.Inject

/** Au démarrage : les tickets en attente ou en échec repartent à l'impression, jamais les imprimés. */
class ResumePendingPrintsUseCase @Inject constructor(private val repository: SaleRepository, private val spooler: PrintSpooler) {
    /** @return le nombre de tickets remis en file. */
    suspend operator fun invoke(): Int = repository.salesToPrint().count { spooler.enqueue(it.saleId) }
}

/** Réimpression manuelle depuis l'historique, réservée aux tickets en échec. */
class ReprintFailedTicketUseCase @Inject constructor(private val repository: SaleRepository, private val spooler: PrintSpooler) {
    suspend operator fun invoke(saleId: String): Boolean {
        val sale = repository.getSale(saleId) ?: return false
        if (sale.printStatus != PrintStatus.FAILED) return false
        repository.updatePrintStatus(saleId, PrintStatus.PENDING)
        return spooler.enqueue(saleId)
    }
}
