package com.poslik.caisse.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Point d'accès unique à Firebase.
 *
 * Sans `google-services.json`, Firebase n'est pas initialisé : l'app reste utilisable hors ligne
 * (ventes, impression, historique) et la synchronisation attend simplement la configuration.
 */
@Singleton
class FirebaseAccess @Inject constructor(@ApplicationContext private val context: Context) {
    val isConfigured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /** Authentification anonyme : les règles refusent tout accès non authentifié. */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val user = checkNotNull(auth.signInAnonymously().await().user) { "Connexion anonyme refusée" }
        return user.uid
    }

    companion object {
        const val NOT_CONFIGURED = "Firebase non configuré (google-services.json manquant)"
        const val TIMEOUT_MILLIS = 20_000L
    }
}
