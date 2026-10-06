package com.poslik.caisse.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifiant aléatoire de cette installation, créé au premier appel.
 *
 * Avec un compte email partagé par plusieurs tablettes, l'uid ne distingue plus les appareils :
 * c'est cet identifiant qui empêche deux tablettes du même compte de réserver le même code caisse.
 */
@Singleton
class InstallationId @Inject constructor(@ApplicationContext private val context: Context) {
    val value: String by lazy {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY, it).commit() }
    }

    private companion object {
        const val PREFS = "installation"
        const val KEY = "id"
    }
}
