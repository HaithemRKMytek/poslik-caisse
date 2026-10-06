package com.poslik.caisse.domain.sync

import com.poslik.caisse.domain.network.NetworkStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * Relance la synchronisation dès que le réseau revient, sans attendre la fin du délai de reprise
 * exponentiel (qui peut atteindre plusieurs heures après des échecs répétés).
 * Le premier état n'est pas un retour : le démarrage planifie déjà sa propre synchronisation.
 */
class SyncOnReconnect @Inject constructor(private val network: NetworkStatus, private val syncNow: SyncNowRequester) {
    suspend fun run() {
        network.isOnline
            .distinctUntilChanged()
            .drop(1)
            .filter { it }
            .collect { syncNow.syncNow() }
    }
}
