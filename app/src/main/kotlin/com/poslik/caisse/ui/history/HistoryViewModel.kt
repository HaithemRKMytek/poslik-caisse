package com.poslik.caisse.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.repository.SaleRepository
import com.poslik.caisse.domain.usecase.ReprintFailedTicketUseCase
import com.poslik.caisse.ui.STOP_TIMEOUT_MILLIS
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(val isLoading: Boolean = true, val sales: List<Sale> = emptyList())

@HiltViewModel
class HistoryViewModel @Inject constructor(saleRepository: SaleRepository, private val reprintFailedTicket: ReprintFailedTicketUseCase) :
    ViewModel() {

    /** L'historique est lu dans Room : il est complet et à jour, avec ou sans réseau. */
    val state: StateFlow<HistoryUiState> = saleRepository.observeSales()
        .map { HistoryUiState(isLoading = false, sales = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    fun reprint(saleId: String) {
        viewModelScope.launch { reprintFailedTicket(saleId) }
    }
}
