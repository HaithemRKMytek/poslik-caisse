package com.poslik.caisse.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.usecase.ReprintFailedTicketUseCase
import com.poslik.caisse.ui.STOP_TIMEOUT_MILLIS
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(val isLoading: Boolean = true, val sales: List<Sale> = emptyList(), val reprintFailed: Boolean = false)

@HiltViewModel
class HistoryViewModel @Inject constructor(saleRepository: SaleRepository, private val reprintFailedTicket: ReprintFailedTicketUseCase) :
    ViewModel() {

    /** L'historique est lu dans Room : il est complet et à jour, avec ou sans réseau. */
    private val reprintFailed = MutableStateFlow(false)

    val state: StateFlow<HistoryUiState> = combine(saleRepository.observeSales(), reprintFailed) { sales, failed ->
        HistoryUiState(isLoading = false, sales = sales, reprintFailed = failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    fun reprint(saleId: String) {
        viewModelScope.launch {
            try {
                reprintFailedTicket(saleId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reprintFailed.value = true
            }
        }
    }

    fun onReprintErrorShown() {
        reprintFailed.value = false
    }
}
