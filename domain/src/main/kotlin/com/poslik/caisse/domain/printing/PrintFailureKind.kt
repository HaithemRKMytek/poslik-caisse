package com.poslik.caisse.domain.printing

/**
 * Cause d'un échec d'impression, assez précise pour dire au caissier quoi faire.
 * Le nom de la valeur est enregistré avec le ticket ; un texte inconnu (ancien ticket, pilote
 * qui ne sait pas classer l'erreur) retombe sur [UNKNOWN].
 */
enum class PrintFailureKind {
    PRINTER_OFFLINE,
    OUT_OF_PAPER,
    TIMEOUT,
    UNKNOWN,
    ;

    companion object {
        fun fromCode(code: String?): PrintFailureKind = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}
