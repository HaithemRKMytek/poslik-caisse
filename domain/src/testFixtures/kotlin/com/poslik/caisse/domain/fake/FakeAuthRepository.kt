package com.poslik.caisse.domain.fake

import com.poslik.caisse.domain.auth.AuthError
import com.poslik.caisse.domain.auth.AuthRepository
import com.poslik.caisse.domain.auth.AuthResult
import com.poslik.caisse.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow

/** Comptes en mémoire : `accounts` associe un email à son mot de passe. */
class FakeAuthRepository(initial: AuthState = AuthState.SignedOut) : AuthRepository {
    override val authState = MutableStateFlow(initial)
    val accounts = mutableMapOf<String, String>()
    var nextFailure: AuthError? = null
    val resetRequests = mutableListOf<String>()

    override suspend fun signIn(email: String, password: String): AuthResult = respond {
        if (accounts[email.trim()] != password) return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)
        authState.value = AuthState.SignedIn(uid = "uid-$email", email = email.trim())
    }

    override suspend fun signUp(email: String, password: String): AuthResult = respond {
        if (email.trim() in accounts) return AuthResult.Failure(AuthError.EMAIL_IN_USE)
        accounts[email.trim()] = password
        authState.value = AuthState.SignedIn(uid = "uid-$email", email = email.trim())
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = respond { resetRequests += email.trim() }

    override fun signOut() {
        authState.value = AuthState.SignedOut
    }

    private inline fun respond(block: () -> Unit): AuthResult {
        nextFailure?.let {
            nextFailure = null
            return AuthResult.Failure(it)
        }
        block()
        return AuthResult.Success
    }
}
