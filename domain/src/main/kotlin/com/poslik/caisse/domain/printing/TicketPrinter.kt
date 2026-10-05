package com.poslik.caisse.domain.printing

import com.poslik.caisse.domain.model.Sale

sealed interface PrintResult {
    data object Success : PrintResult

    data class Failure(val reason: String) : PrintResult
}

/** Abstraction de l'imprimante : une imprimante simulée aujourd'hui, un pilote ESC/POS demain. */
fun interface TicketPrinter {
    suspend fun print(sale: Sale): PrintResult
}
