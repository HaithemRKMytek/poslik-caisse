package com.poslik.caisse.domain.network

import kotlinx.coroutines.flow.Flow

/** État du réseau, affiché au caissier. L'encaissement n'en dépend jamais. */
interface NetworkStatus {
    val isOnline: Flow<Boolean>
}
