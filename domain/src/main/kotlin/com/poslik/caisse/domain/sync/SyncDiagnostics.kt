package com.poslik.caisse.domain.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État de la synchronisation, observé par l'interface : un envoi est-il en cours ?
 * Les causes techniques d'un échec (réseau, serveur) restent dans les journaux : le caissier
 * n'a rien à en faire, la synchronisation reprend toute seule.
 */
@Singleton
class SyncDiagnostics @Inject constructor() {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun started() {
        _isSyncing.value = true
    }

    fun finished() {
        _isSyncing.value = false
    }
}
