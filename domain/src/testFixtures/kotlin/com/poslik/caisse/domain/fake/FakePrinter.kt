package com.poslik.caisse.domain.fake

import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.printing.PrintResult
import com.poslik.caisse.domain.printing.TicketPrinter
import kotlinx.coroutines.delay

class FakePrinter(var failing: Boolean = false, var delayMillis: Long = 100) : TicketPrinter {
    val printed = mutableListOf<String>()

    override suspend fun print(sale: Sale): PrintResult {
        delay(delayMillis)
        if (failing) return PrintResult.Failure("Plus de papier")
        printed += sale.ticketNumber.label
        return PrintResult.Success
    }
}
