package com.poslik.caisse.domain.model

/**
 * Montant en millimes (1 DT = 1 000 millimes).
 *
 * Le dinar tunisien a trois décimales : on stocke des entiers pour éviter
 * toute erreur d'arrondi liée aux nombres à virgule flottante.
 */
@JvmInline
value class Money(val millimes: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(Math.addExact(millimes, other.millimes))

    operator fun times(quantity: Int): Money = Money(Math.multiplyExact(millimes, quantity.toLong()))

    override fun compareTo(other: Money): Int = millimes.compareTo(other.millimes)

    /** Format d'affichage tunisien : `12,500 DT`. */
    fun format(): String {
        val sign = if (millimes < 0) "-" else ""
        val abs = kotlin.math.abs(millimes)
        val dinars = abs / MILLIMES_PER_DINAR
        val rest = (abs % MILLIMES_PER_DINAR).toString().padStart(3, '0')
        return "$sign$dinars,$rest DT"
    }

    override fun toString(): String = format()

    companion object {
        const val MILLIMES_PER_DINAR = 1_000L
        val ZERO = Money(0)
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { acc, money -> acc + money }
