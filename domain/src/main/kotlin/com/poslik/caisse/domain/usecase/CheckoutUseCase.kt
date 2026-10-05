package com.poslik.caisse.domain.usecase

import com.poslik.caisse.domain.model.Cart
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.printing.PrintSpooler
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.sync.SyncScheduler
import javax.inject.Inject

fun interface Clock {
    fun now(): Long
}

/**
 * Le geste « Encaisser » :
 * 1. numéro attribué et vente enregistrée dans une seule transaction locale (durable, hors ligne) ;
 * 2. ticket mis en file d'impression (non bloquant) ;
 * 3. synchronisation demandée (exécutée quand le réseau est là).
 *
 * L'appelant vide le panier dès que la vente est renvoyée.
 */
class CheckoutUseCase @Inject constructor(
    private val repository: SaleRepository,
    private val spooler: PrintSpooler,
    private val syncScheduler: SyncScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(cart: Cart): Sale {
        require(!cart.isEmpty) { "Le panier est vide" }
        val sale = repository.recordSale(cart.toSaleLines(), clock.now())
        spooler.enqueue(sale.saleId)
        syncScheduler.requestSync()
        return sale
    }
}
