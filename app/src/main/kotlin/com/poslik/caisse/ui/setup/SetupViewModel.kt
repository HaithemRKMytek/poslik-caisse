package com.poslik.caisse.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.repository.ClaimResult
import com.poslik.caisse.domain.repository.RegisterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SetupError {
    data object Invalid : SetupError

    data object Taken : SetupError

    data class Unavailable(val reason: String) : SetupError
}

data class SetupUiState(val code: String = "C01", val isSubmitting: Boolean = false, val error: SetupError? = null)

@HiltViewModel
class SetupViewModel @Inject constructor(private val registerRepository: RegisterRepository) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value.uppercase().take(MAX_LENGTH), error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val code = RegisterCode.parse(current.code)
        if (code == null) {
            _state.update { it.copy(error = SetupError.Invalid) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            // En cas de succès, la racine observe le code enregistré et affiche la caisse.
            val error = when (val result = registerRepository.claim(code)) {
                ClaimResult.Success -> null
                ClaimResult.AlreadyTaken -> SetupError.Taken
                is ClaimResult.Unavailable -> SetupError.Unavailable(result.reason)
            }
            _state.update { it.copy(isSubmitting = false, error = error) }
        }
    }

    private companion object {
        const val MAX_LENGTH = 6
    }
}
