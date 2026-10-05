package com.poslik.caisse.domain.model

enum class PrintStatus {
    /** Envoyé à l'impression, pas encore confirmé. */
    PENDING,

    /** Imprimé : ne repart jamais à l'impression automatiquement. */
    PRINTED,

    /** L'imprimante a échoué : repart au prochain démarrage ou sur demande. */
    FAILED,
}

enum class SyncStatus {
    /** Modifié localement depuis le dernier envoi réussi. */
    PENDING,

    /** La dernière version locale est dans Firebase. */
    SYNCED,

    /** Firebase contient un autre ticket sous ce numéro : signalé, jamais écrasé. */
    CONFLICT,
}

data class SaleLine(val productId: String, val productName: String, val unitPrice: Money, val quantity: Int) {
    val subtotal: Money get() = unitPrice * quantity
}

data class Sale(
    /** Identifiant technique (UUID) généré localement : clé d'idempotence. */
    val saleId: String,
    val ticketNumber: TicketNumber,
    val lines: List<SaleLine>,
    val total: Money,
    /** Horodatage local (epoch ms) ; sert à l'affichage, jamais à l'unicité. */
    val createdAt: Long,
    val printStatus: PrintStatus,
    val printAttempts: Int,
    val lastPrintError: String?,
    val syncStatus: SyncStatus,
)
