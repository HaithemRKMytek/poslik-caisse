package com.poslik.caisse.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poslik.caisse.domain.auth.AuthRepository
import com.poslik.caisse.domain.auth.AuthState
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.repository.RegisterRepository
import com.poslik.caisse.ui.history.HistoryScreen
import com.poslik.caisse.ui.login.LoginScreen
import com.poslik.caisse.ui.pos.PosScreen
import com.poslik.caisse.ui.setup.SetupScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface RootState {
    data object Loading : RootState

    data object SignIn : RootState

    data object Setup : RootState

    data class Ready(val code: RegisterCode) : RootState
}

@HiltViewModel
class AppViewModel @Inject constructor(authRepository: AuthRepository, registerRepository: RegisterRepository) : ViewModel() {
    val rootState: StateFlow<RootState> = combine(authRepository.authState, registerRepository.observeRegisterCode(), ::rootStateOf)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RootState.Loading)
}

/**
 * Connexion d'abord (la réservation du code caisse et la synchro exigent un compte), puis
 * configuration de la caisse, puis caisse. Sans Firebase, on passe directement à la configuration.
 */
internal fun rootStateOf(auth: AuthState, code: RegisterCode?): RootState = when {
    auth is AuthState.SignedOut -> RootState.SignIn
    code == null -> RootState.Setup
    else -> RootState.Ready(code)
}

/** Racine : connexion, configuration de la caisse au premier lancement, puis caisse et historique. */
@Composable
fun CaisseApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.rootState.collectAsStateWithLifecycle()
    when (val current = state) {
        RootState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        RootState.SignIn -> LoginScreen()
        RootState.Setup -> SetupScreen()
        is RootState.Ready -> MainNavigation(current.code)
    }
}

private object Routes {
    const val POS = "pos"
    const val HISTORY = "history"
}

@Composable
private fun MainNavigation(registerCode: RegisterCode) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.POS) {
        composable(Routes.POS) {
            PosScreen(registerCode = registerCode, onOpenHistory = { navController.navigate(Routes.HISTORY) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}

const val STOP_TIMEOUT_MILLIS = 5_000L
