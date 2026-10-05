package com.poslik.caisse.domain.printing

import com.poslik.caisse.domain.model.Sale

/** Mise en page texte d'un ticket (32 colonnes, largeur d'une imprimante 58 mm). */
object TicketFormatter {
    const val WIDTH = 32

    fun format(sale: Sale, dateText: String): String = buildString {
        appendLine(center("POSLIK CAISSE"))
        appendLine(center("Ticket ${sale.ticketNumber.label}"))
        appendLine(center(dateText))
        appendLine("-".repeat(WIDTH))
        sale.lines.forEach { line ->
            appendLine(line.productName.take(WIDTH))
            appendLine(justify("  ${line.quantity} x ${line.unitPrice.format()}", line.subtotal.format()))
        }
        appendLine("-".repeat(WIDTH))
        appendLine(justify("TOTAL", sale.total.format()))
    }

    private fun center(text: String): String {
        val padding = ((WIDTH - text.length) / 2).coerceAtLeast(0)
        return " ".repeat(padding) + text
    }

    private fun justify(left: String, right: String): String {
        val spaces = (WIDTH - left.length - right.length).coerceAtLeast(1)
        return left + " ".repeat(spaces) + right
    }
}
