package com.poslik.caisse.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.poslik.caisse.domain.auth.AuthError
import com.poslik.caisse.domain.auth.AuthRepository
import com.poslik.caisse.domain.auth.AuthResult
import com.poslik.caisse.domain.auth.AuthState
import com.poslik.caisse.domain.sync.SyncScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/** Firebase Authentication, fournisseur email/mot de passe. */
@Singleton
class FirebaseAuthRepository @Inject constructor(private val firebase: FirebaseAccess, private val syncScheduler: SyncScheduler) :
    AuthRepository {

    override val authState: Flow<AuthState> = flow {
        if (!firebase.isConfigured) {
            emit(AuthState.Unavailable)
        } else {
            emitAll(observeUser(firebase.auth))
        }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): AuthResult = attempt {
        firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signUp(email: String, password: String): AuthResult = attempt {
        firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = attempt {
        firebase.auth.sendPasswordResetEmail(email.trim()).await()
    }

    override fun signOut() {
        if (firebase.isConfigured) firebase.auth.signOut()
    }

    private fun observeUser(auth: FirebaseAuth): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toState()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private suspend fun attempt(block: suspend () -> Unit): AuthResult {
        if (!firebase.isConfigured) return AuthResult.Failure(AuthError.UNKNOWN)
        return try {
            block()
            // Des ventes ont pu être faites avant la connexion : on les envoie maintenant.
            syncScheduler.requestSync()
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthError())
        }
    }
}

private fun FirebaseUser?.toState(): AuthState = if (this == null) AuthState.SignedOut else AuthState.SignedIn(uid, email)

internal fun Exception.toAuthError(): AuthError = when (this) {
    // Sous-classe de FirebaseAuthInvalidCredentialsException : à tester en premier.
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.WRONG_CREDENTIALS
    is FirebaseAuthInvalidUserException -> AuthError.WRONG_CREDENTIALS
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_ATTEMPTS
    else -> AuthError.UNKNOWN
}
