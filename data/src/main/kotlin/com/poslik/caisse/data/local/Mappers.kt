package com.poslik.caisse.data.local

import com.poslik.caisse.domain.model.Money
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SaleLine
import com.poslik.caisse.domain.model.SyncStatus
import com.poslik.caisse.domain.model.TicketNumber

internal fun SaleWithLines.toDomain(): Sale = Sale(
    saleId = sale.saleId,
    ticketNumber = TicketNumber(RegisterCode(sale.registerCode), sale.ticketNumber),
    lines = lines.sortedBy { it.position }.map { it.toDomain() },
    total = Money(sale.totalMillimes),
    createdAt = sale.createdAt,
    printStatus = sale.printStatus,
    printAttempts = sale.printAttempts,
    lastPrintError = sale.lastPrintError,
    syncStatus = when {
        sale.syncConflict -> SyncStatus.CONFLICT
        sale.syncedVersion >= sale.version -> SyncStatus.SYNCED
        else -> SyncStatus.PENDING
    },
)

private fun SaleLineEntity.toDomain() = SaleLine(
    productId = productId,
    productName = productName,
    unitPrice = Money(unitPriceMillimes),
    quantity = quantity,
)

internal fun SaleLine.toEntity(saleId: String, position: Int) = SaleLineEntity(
    saleId = saleId,
    productId = productId,
    position = position,
    productName = productName,
    unitPriceMillimes = unitPrice.millimes,
    quantity = quantity,
)
