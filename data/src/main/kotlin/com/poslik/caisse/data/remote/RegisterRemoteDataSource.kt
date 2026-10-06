package com.poslik.caisse.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.poslik.caisse.data.local.InstallationId
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.repository.ClaimResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

fun interface RegisterRemoteDataSource {
    suspend fun claim(code: RegisterCode): ClaimResult
}

/**
 * Réserve un code caisse par transaction Firebase : la première installation qui l'écrit le possède.
 * C'est ce qui garantit que deux tablettes n'ont jamais le même préfixe de numérotation, même
 * connectées au même compte.
 */
@Singleton
class FirebaseRegisterDataSource @Inject constructor(private val firebase: FirebaseAccess, private val installationId: InstallationId) :
    RegisterRemoteDataSource {

    override suspend fun claim(code: RegisterCode): ClaimResult {
        if (!firebase.isConfigured) return ClaimResult.Unavailable(FirebaseAccess.NOT_CONFIGURED)
        return try {
            val committed = withTimeout(FirebaseAccess.TIMEOUT_MILLIS) {
                val uid = firebase.requireUid()
                firebase.database.getReference(REGISTERS).child(code.value).claimFor(uid, installationId.value)
            }
            if (committed) ClaimResult.Success else ClaimResult.AlreadyTaken
        } catch (e: TimeoutCancellationException) {
            ClaimResult.Unavailable("Pas de connexion à Firebase")
        } catch (e: NotSignedInException) {
            ClaimResult.Unavailable(e.message.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ClaimResult.Unavailable(e.message ?: e::class.java.simpleName)
        }
    }

    private suspend fun DatabaseReference.claimFor(uid: String, deviceId: String): Boolean = suspendCancellableCoroutine { cont ->
        runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(current: MutableData): Transaction.Result {
                    val owner = current.child("claimedBy").getValue(String::class.java)
                    val device = current.child("deviceId").getValue(String::class.java)
                    return when {
                        current.value == null -> {
                            current.child("claimedBy").value = uid
                            current.child("deviceId").value = deviceId
                            current.child("claimedAt").value = System.currentTimeMillis()
                            Transaction.success(current)
                        }
                        // Même compte et même installation : reprise d'une réservation interrompue.
                        owner == uid && device == deviceId -> Transaction.success(current)
                        else -> Transaction.abort()
                    }
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!cont.isActive) return
                    if (error != null) cont.resumeWithException(error.toException()) else cont.resume(committed)
                }
            },
        )
    }

    companion object {
        const val REGISTERS = "registers"
    }
}
