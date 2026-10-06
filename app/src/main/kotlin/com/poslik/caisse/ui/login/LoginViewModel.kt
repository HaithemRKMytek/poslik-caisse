package com.poslik.caisse.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poslik.caisse.domain.auth.AuthError
import com.poslik.caisse.domain.auth.AuthRepository
import com.poslik.caisse.domain.auth.AuthResult
import com.poslik.caisse.domain.auth.Credentials
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginMode { SIGN_IN, SIGN_UP }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val mode: LoginMode = LoginMode.SIGN_IN,
    val isSubmitting: Boolean = false,
    val error: AuthError? = null,
    val resetEmailSent: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null, resetEmailSent = false) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun toggleMode() = _state.update {
        it.copy(mode = if (it.mode == LoginMode.SIGN_IN) LoginMode.SIGN_UP else LoginMode.SIGN_IN, error = null)
    }

    /** En cas de succès, la racine observe la session et passe à l'écran suivant. */
    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        Credentials.validate(current.email, current.password)?.let { error ->
            _state.update { it.copy(error = error) }
            return
        }
        launchRequest {
            when (current.mode) {
                LoginMode.SIGN_IN -> authRepository.signIn(current.email, current.password)
                LoginMode.SIGN_UP -> authRepository.signUp(current.email, current.password)
            }
        }
    }

    fun sendPasswordReset() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!Credentials.isValidEmail(current.email)) {
            _state.update { it.copy(error = AuthError.INVALID_EMAIL) }
            return
        }
        launchRequest(onSuccess = { it.copy(resetEmailSent = true) }) { authRepository.sendPasswordReset(current.email) }
    }

    private fun launchRequest(onSuccess: (LoginUiState) -> LoginUiState = { it }, request: suspend () -> AuthResult) {
        _state.update { it.copy(isSubmitting = true, error = null, resetEmailSent = false) }
        viewModelScope.launch {
            val result = request()
            _state.update {
                val done = it.copy(isSubmitting = false)
                when (result) {
                    AuthResult.Success -> onSuccess(done)
                    is AuthResult.Failure -> done.copy(error = result.error)
                }
            }
        }
    }
}
