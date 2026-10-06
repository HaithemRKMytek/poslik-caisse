package com.poslik.caisse.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poslik.caisse.data.di.ApplicationScope
import com.poslik.caisse.data.printing.PrinterSettings
import com.poslik.caisse.domain.auth.AuthRepository
import com.poslik.caisse.domain.auth.AuthState
import com.poslik.caisse.domain.model.Cart
import com.poslik.caisse.domain.model.Catalog
import com.poslik.caisse.domain.model.Product
import com.poslik.caisse.domain.network.NetworkStatus
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.sync.SyncDiagnostics
import com.poslik.caisse.domain.sync.SyncNowRequester
import com.poslik.caisse.domain.usecase.CheckoutUseCase
import com.poslik.caisse.ui.STOP_TIMEOUT_MILLIS
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosUiState(
    val products: List<Product> = Catalog.products,
    val cart: Cart = Cart(),
    val isCheckingOut: Boolean = false,
    val lastTicket: String? = null,
    val checkoutError: String? = null,
    val isOnline: Boolean = true,
    val unsyncedCount: Int = 0,
    val printerFailureMode: Boolean = false,
    /** Null en mode hors ligne de démonstration (Firebase non configuré) : pas de compte. */
    val accountEmail: String? = null,
    /** Tickets dont l'impression a échoué : le caissier doit les réimprimer depuis l'historique. */
    val failedPrintCount: Int = 0,
    val isSyncing: Boolean = false,
) {
    val canCheckout: Boolean get() = !cart.isEmpty && !isCheckingOut

    /** Quantité par produit, pour les pastilles de la grille. */
    val quantities: Map<String, Int> get() = cart.lines.associate { it.product.id to it.quantity }

    /**
     * Se déconnecter hors ligne ou avec des ventes non envoyées bloquerait la caisse (reconnexion
     * impossible) ou la synchronisation (plus de compte pour écrire) : on l'interdit dans ces cas.
     */
    val canSignOut: Boolean get() = isOnline && unsyncedCount == 0
}

@HiltViewModel
class PosViewModel @Inject constructor(
    private val checkoutUseCase: CheckoutUseCase,
    private val printerSettings: PrinterSettings,
    private val authRepository: AuthRepository,
    private val syncNowRequester: SyncNowRequester,
    @ApplicationScope private val applicationScope: CoroutineScope,
    saleRepository: SaleRepository,
    networkStatus: NetworkStatus,
    syncDiagnostics: SyncDiagnostics,
) : ViewModel() {

    private data class LocalState(
        val cart: Cart = Cart(),
        val isCheckingOut: Boolean = false,
        val lastTicket: String? = null,
        val checkoutError: String? = null,
    )

    private val local = MutableStateFlow(LocalState())

    private data class SyncInfo(val isOnline: Boolean, val unsynced: Int, val isSyncing: Boolean)

    private val syncInfo = combine(
        networkStatus.isOnline,
        saleRepository.observeUnsyncedCount(),
        syncDiagnostics.isSyncing,
        ::SyncInfo,
    )

    val state: StateFlow<PosUiState> = combine(
        local,
        syncInfo,
        printerSettings.failureMode,
        authRepository.authState,
        saleRepository.observePrintFailureCount(),
    ) { local, sync, failureMode, auth, failedPrints ->
        PosUiState(
            cart = local.cart,
            isCheckingOut = local.isCheckingOut,
            lastTicket = local.lastTicket,
            checkoutError = local.checkoutError,
            isOnline = sync.isOnline,
            unsyncedCount = sync.unsynced,
            printerFailureMode = failureMode,
            accountEmail = (auth as? AuthState.SignedIn)?.let { it.email ?: it.uid },
            failedPrintCount = failedPrints,
            isSyncing = sync.isSyncing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PosUiState())

    fun addProduct(product: Product) = editCart { it.add(product) }

    fun removeOne(productId: String) = editCart { it.removeOne(productId) }

    fun syncNow() = syncNowRequester.syncNow()

    fun clearCart() = editCart { Cart() }

    fun setPrinterFailureMode(enabled: Boolean) = printerSettings.setFailureMode(enabled)

    fun signOut() {
        if (state.value.canSignOut) authRepository.signOut()
    }

    /**
     * Le panier est figé pendant l'écriture (quelques millisecondes) pour qu'un double appui
     * ne crée pas deux ventes. L'écriture tourne dans le scope de l'application : elle va au bout
     * même si l'écran est fermé entre-temps. L'impression et la synchro suivent en arrière-plan.
     */
    fun checkout() {
        val snapshot = local.value
        if (snapshot.cart.isEmpty || snapshot.isCheckingOut) return
        local.update { it.copy(isCheckingOut = true, checkoutError = null) }
        applicationScope.launch {
            try {
                val sale = checkoutUseCase(snapshot.cart)
                local.update { it.copy(cart = Cart(), isCheckingOut = false, lastTicket = sale.ticketNumber.label) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Le panier est conservé : rien n'a été enregistré, le caissier peut réessayer.
                local.update { it.copy(isCheckingOut = false, checkoutError = e.message ?: e::class.java.simpleName) }
            }
        }
    }

    private fun editCart(transform: (Cart) -> Cart) {
        local.update { if (it.isCheckingOut) it else it.copy(cart = transform(it.cart), checkoutError = null) }
    }
}
