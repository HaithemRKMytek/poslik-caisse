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
 * Local Emulator Suite de la machine hôte (10.0.2.2) au lieu du vrai projet : le scénario de bout en
 * bout tourne sur une base vide, avec les vraies règles, sans toucher aux données réelles.
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
        private const val HOST = "10.0.2.2"
        private const val AUTH_PORT = 9099
        private const val DATABASE_PORT = 9000
    }
}
