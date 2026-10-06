package com.poslik.caisse.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Compte email/mot de passe connecté. Firebase garde la session sur l'appareil : elle reste
     * valable hors ligne et après un redémarrage, seule une déconnexion explicite la ferme.
     */
    fun requireUid(): String = auth.currentUser?.uid ?: throw NotSignedInException()

    companion object {
        const val NOT_CONFIGURED = "Firebase non configuré (google-services.json manquant)"
        const val TIMEOUT_MILLIS = 20_000L
    }
}

class NotSignedInException : IllegalStateException("Aucun compte connecté")
