package com.poslik.caisse.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.poslik.caisse.domain.sync.SyncNowRequester
import com.poslik.caisse.domain.sync.SyncScheduler
import com.poslik.caisse.domain.usecase.SyncSalesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Envoie les ventes en attente ; WorkManager le relance avec backoff tant qu'il en reste. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncSales: SyncSalesUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val report = syncSales()
        Log.i(TAG, "Synchro : ${report.pushed} envoyée(s), ${report.conflicts} conflit(s), échec = ${report.failure}")
        return if (report.shouldRetry) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "sales-sync"
        private const val TAG = "SyncWorker"
    }
}

/**
 * Planifie la synchronisation dès que le réseau est disponible.
 * Les demandes s'enchaînent (APPEND_OR_REPLACE) : une vente créée pendant un envoi en cours
 * déclenche un nouveau passage au lieu d'être ignorée. Survit au kill de l'app et au redémarrage.
 */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    SyncScheduler,
    SyncNowRequester {

    override fun requestSync() = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)

    /**
     * REPLACE repart d'une demande neuve : le délai de reprise exponentiel d'un échec précédent
     * est oublié. L'envoi est idempotent, interrompre un passage en cours est donc sans risque.
     */
    override fun syncNow() = enqueue(ExistingWorkPolicy.REPLACE)

    private fun enqueue(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SyncWorker.UNIQUE_NAME, policy, request)
    }

    private companion object {
        const val BACKOFF_SECONDS = 10L
    }
}
