package com.poslik.caisse.domain.auth

import kotlinx.coroutines.flow.Flow

sealed interface AuthState {
    /** Firebase n'est pas configuré (pas de google-services.json) : mode hors ligne de démonstration. */
    data object Unavailable : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val uid: String, val email: String?) : AuthState
}

enum class AuthError {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    WRONG_CREDENTIALS,
    EMAIL_IN_USE,
    NETWORK,
    TOO_MANY_ATTEMPTS,
    UNKNOWN,
}

sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(val error: AuthError) : AuthResult
}

/** Compte email/mot de passe de l'établissement. La session est conservée hors ligne. */
interface AuthRepository {
    val authState: Flow<AuthState>

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signUp(email: String, password: String): AuthResult

    suspend fun sendPasswordReset(email: String): AuthResult

    fun signOut()
}

/** Contrôles faits avant d'appeler Firebase, pour un message immédiat et sans réseau. */
object Credentials {
    const val MIN_PASSWORD_LENGTH = 6

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validate(email: String, password: String): AuthError? = when {
        !isValidEmail(email) -> AuthError.INVALID_EMAIL
        password.length < MIN_PASSWORD_LENGTH -> AuthError.WEAK_PASSWORD
        else -> null
    }

    fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())
}
