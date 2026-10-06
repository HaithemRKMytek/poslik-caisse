package com.poslik.caisse.data.remote

import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.sync.PushResult
import com.poslik.caisse.domain.sync.RemoteSaleDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Écrit chaque vente sous `/sales/{caisse}/{numéro}`.
 *
 * La clé est déterministe et `setValue` remplace le nœud entier : renvoyer la même vente
 * (relance après une coupure) ne crée jamais de doublon. Les règles refusent d'écraser un ticket
 * portant un autre `saleId`, ce qui transforme une collision en conflit visible.
 */
@Singleton
class FirebaseSaleDataSource @Inject constructor(private val firebase: FirebaseAccess) : RemoteSaleDataSource {

    override suspend fun push(sale: Sale): PushResult {
        if (!firebase.isConfigured) return PushResult.Failure(FirebaseAccess.NOT_CONFIGURED)
        val ref = firebase.database.getReference(SALES)
            .child(sale.ticketNumber.registerCode.value)
            .child(sale.ticketNumber.key)
        return try {
            withTimeout(FirebaseAccess.TIMEOUT_MILLIS) {
                firebase.requireUid()
                ref.setValue(sale.toFirebaseMap()).await()
            }
            PushResult.Success
        } catch (e: TimeoutCancellationException) {
            PushResult.Failure("Firebase injoignable (délai dépassé)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: DatabaseException) {
            if (e.isPermissionDenied() && isOwnedByAnotherSale(ref, sale)) {
                PushResult.Conflict
            } else {
                PushResult.Failure(e.message.orEmpty())
            }
        } catch (e: Exception) {
            PushResult.Failure(e.message ?: e::class.java.simpleName)
        }
    }

    private suspend fun isOwnedByAnotherSale(ref: DatabaseReference, sale: Sale): Boolean = try {
        val existing = withTimeout(FirebaseAccess.TIMEOUT_MILLIS) { ref.get().await() }
        existing.exists() && existing.child("saleId").getValue(String::class.java) != sale.saleId
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    private fun DatabaseException.isPermissionDenied() = message?.contains("Permission denied", ignoreCase = true) == true

    companion object {
        const val SALES = "sales"
    }
}

internal fun Sale.toFirebaseMap(): Map<String, Any?> = mapOf(
    "saleId" to saleId,
    "registerCode" to ticketNumber.registerCode.value,
    "ticketNumber" to ticketNumber.sequence,
    "ticketLabel" to ticketNumber.label,
    "totalMillimes" to total.millimes,
    "createdAt" to createdAt,
    "printStatus" to printStatus.name,
    "printAttempts" to printAttempts,
    "lines" to lines.associate { line ->
        line.productId to mapOf(
            "name" to line.productName,
            "unitPriceMillimes" to line.unitPrice.millimes,
            "quantity" to line.quantity,
        )
    },
    "syncedAt" to ServerValue.TIMESTAMP,
)
