package com.poslik.caisse.data.di

import android.content.Context
import androidx.room.Room
import com.poslik.caisse.data.local.CaisseDatabase
import com.poslik.caisse.data.local.RegisterDao
import com.poslik.caisse.data.local.RoomRegisterRepository
import com.poslik.caisse.data.local.RoomSaleRepository
import com.poslik.caisse.data.network.NetworkMonitor
import com.poslik.caisse.data.printing.FakeTicketPrinter
import com.poslik.caisse.data.remote.FirebaseAuthRepository
import com.poslik.caisse.data.remote.FirebaseRegisterDataSource
import com.poslik.caisse.data.remote.FirebaseSaleDataSource
import com.poslik.caisse.data.remote.RegisterRemoteDataSource
import com.poslik.caisse.data.sync.WorkManagerSyncScheduler
import com.poslik.caisse.domain.auth.AuthRepository
import com.poslik.caisse.domain.network.NetworkStatus
import com.poslik.caisse.domain.printing.PrintSpooler
import com.poslik.caisse.domain.printing.TicketPrinter
import com.poslik.caisse.domain.repository.RegisterRepository
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.sync.RemoteSaleDataSource
import com.poslik.caisse.domain.sync.SaleSyncStore
import com.poslik.caisse.domain.sync.SyncScheduler
import com.poslik.caisse.domain.usecase.Clock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Scope de l'application : le travail lancé ici survit à la fermeture d'un écran. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): CaisseDatabase =
        Room.databaseBuilder(context, CaisseDatabase::class.java, CaisseDatabase.NAME).build()

    @Provides
    fun registerDao(database: CaisseDatabase): RegisterDao = database.registerDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun printSpooler(
        printer: TicketPrinter,
        repository: SaleRepository,
        syncScheduler: SyncScheduler,
        @ApplicationScope scope: CoroutineScope,
    ): PrintSpooler = PrintSpooler(printer, repository, scope, onStatusChanged = syncScheduler::requestSync)

    @Provides
    fun clock(): Clock = Clock { System.currentTimeMillis() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds
    abstract fun saleRepository(impl: RoomSaleRepository): SaleRepository

    @Binds
    abstract fun saleSyncStore(impl: RoomSaleRepository): SaleSyncStore

    @Binds
    abstract fun registerRepository(impl: RoomRegisterRepository): RegisterRepository

    @Binds
    abstract fun authRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun registerRemote(impl: FirebaseRegisterDataSource): RegisterRemoteDataSource

    @Binds
    abstract fun remoteSales(impl: FirebaseSaleDataSource): RemoteSaleDataSource

    @Binds
    abstract fun printer(impl: FakeTicketPrinter): TicketPrinter

    @Binds
    abstract fun syncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    abstract fun networkStatus(impl: NetworkMonitor): NetworkStatus
}
