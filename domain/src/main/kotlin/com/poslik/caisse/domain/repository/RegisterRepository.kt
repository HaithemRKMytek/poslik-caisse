package com.poslik.caisse.domain.repository

import com.poslik.caisse.domain.model.RegisterCode
import kotlinx.coroutines.flow.Flow

sealed interface ClaimResult {
    data object Success : ClaimResult

    /** Le code est déjà utilisé par une autre tablette. */
    data object AlreadyTaken : ClaimResult

    /** Pas de réseau ou Firebase injoignable : réessayer plus tard. */
    data class Unavailable(val reason: String) : ClaimResult
}

interface RegisterRepository {

    /** Code de cette caisse, ou null tant qu'elle n'est pas configurée. */
    fun observeRegisterCode(): Flow<RegisterCode?>

    /**
     * Réserve le code dans Firebase (nécessite le réseau, une seule fois) puis l'enregistre
     * localement avec un compteur à zéro.
     */
    suspend fun claim(code: RegisterCode): ClaimResult
}
