package com.poslik.caisse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.poslik.caisse.data.di.ApplicationScope
import com.poslik.caisse.domain.sync.SyncOnReconnect
import com.poslik.caisse.domain.sync.SyncScheduler
import com.poslik.caisse.domain.usecase.ResumePendingPrintsUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class CaisseApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var resumePendingPrints: ResumePendingPrintsUseCase

    @Inject lateinit var syncScheduler: SyncScheduler

    @Inject lateinit var syncOnReconnect: SyncOnReconnect

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Exigence n°4 : au démarrage, les tickets en attente ou en échec repartent à l'impression.
        applicationScope.launch { resumePendingPrints() }
        // Rattrape une synchronisation interrompue (app tuée, tablette redémarrée).
        syncScheduler.requestSync()
        // Au retour du réseau, on repart tout de suite au lieu d'attendre le délai de reprise.
        applicationScope.launch { syncOnReconnect.run() }
    }
}
