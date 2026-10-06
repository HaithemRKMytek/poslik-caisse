package com.poslik.caisse

import android.app.Application
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger

/**
 * Lanceur de tests instrumentés. Avec l'argument `firebaseEmulator=true`, l'app parle à la Firebase
 * Local Emulator Suite au lieu du vrai projet : le scénario de bout en bout tourne sur une base vide,
 * avec les vraies règles, sans toucher aux données réelles.
 *
 * Les ports sont redirigés vers l'hôte par `adb reverse` (voir `.github/scripts/e2e.sh`) : l'émulateur
 * Realtime Database annonce l'adresse 127.0.0.1, que le SDK réutilise à chaque reconnexion.
 */
class FirebaseEmulatorRunner : AndroidJUnitRunner() {
    private var useEmulator = false

    override fun onCreate(arguments: Bundle) {
        useEmulator = arguments.getString(ARG_EMULATOR) == "true"
        super.onCreate(arguments)
    }

    // Appelé avant Application.onCreate : aucun service Firebase n'a encore été utilisé.
    override fun callApplicationOnCreate(app: Application) {
        if (useEmulator && FirebaseApp.getApps(app).isNotEmpty()) {
            FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT)
            FirebaseDatabase.getInstance().apply {
                useEmulator(HOST, DATABASE_PORT)
                setLogLevel(Logger.Level.DEBUG)
            }
        }
        super.callApplicationOnCreate(app)
    }

    companion object {
        const val ARG_EMULATOR = "firebaseEmulator"
        const val HOST = "127.0.0.1"
        private const val AUTH_PORT = 9099
        private const val DATABASE_PORT = 9000
    }
}
