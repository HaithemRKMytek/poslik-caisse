package com.poslik.caisse.domain.printing

import com.poslik.caisse.domain.model.Sale

sealed interface PrintResult {
    data object Success : PrintResult

    /**
     * @param reason détail technique, pour les journaux ; jamais affiché tel quel au caissier.
     * @param kind cause connue, traduite en message clair par l'interface.
     */
    data class Failure(val reason: String, val kind: PrintFailureKind = PrintFailureKind.UNKNOWN) : PrintResult {
        /** Valeur enregistrée avec le ticket : le code de la cause, ou le détail si elle est inconnue. */
        val storedError: String get() = if (kind == PrintFailureKind.UNKNOWN) reason else kind.name
    }
}

/** Abstraction de l'imprimante : une imprimante simulée aujourd'hui, un pilote ESC/POS demain. */
fun interface TicketPrinter {
    suspend fun print(sale: Sale): PrintResult
}
