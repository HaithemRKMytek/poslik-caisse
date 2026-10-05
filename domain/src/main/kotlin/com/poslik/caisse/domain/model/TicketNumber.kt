package com.poslik.caisse.domain.model

/**
 * Numéro de ticket : code de caisse + compteur local de cette caisse.
 *
 * Le code de caisse est unique (réservé dans Firebase au premier lancement) et le compteur
 * est incrémenté dans la même transaction que l'enregistrement de la vente. Le couple est
 * donc unique sur l'ensemble des caisses, même hors ligne.
 */
data class TicketNumber(val registerCode: RegisterCode, val sequence: Long) : Comparable<TicketNumber> {

    init {
        require(sequence in 1..MAX_SEQUENCE) { "Numéro de séquence hors limites : $sequence" }
    }

    /** Clé stable, triable lexicographiquement, utilisée comme clé Firebase. */
    val key: String get() = sequence.toString().padStart(DIGITS, '0')

    /** Libellé imprimé sur le ticket, par exemple `C01-000042`. */
    val label: String get() = "${registerCode.value}-$key"

    override fun compareTo(other: TicketNumber): Int = compareValuesBy(this, other, { it.registerCode.value }, { it.sequence })

    override fun toString(): String = label

    companion object {
        const val DIGITS = 6
        const val MAX_SEQUENCE = 999_999L
    }
}

@JvmInline
value class RegisterCode(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Code caisse invalide : « $value » (2 à 6 lettres majuscules ou chiffres)" }
    }

    companion object {
        val PATTERN = Regex("^[A-Z0-9]{2,6}$")

        /** Normalise une saisie utilisateur (` c01 ` → `C01`) ; renvoie null si invalide. */
        fun parse(input: String): RegisterCode? {
            val normalized = input.trim().uppercase()
            return if (PATTERN.matches(normalized)) RegisterCode(normalized) else null
        }
    }
}
