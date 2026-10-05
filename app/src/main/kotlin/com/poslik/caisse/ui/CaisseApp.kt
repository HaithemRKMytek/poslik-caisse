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
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.repository.RegisterRepository
import com.poslik.caisse.ui.history.HistoryScreen
import com.poslik.caisse.ui.pos.PosScreen
import com.poslik.caisse.ui.setup.SetupScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface RegisterState {
    data object Loading : RegisterState

    data object NotConfigured : RegisterState

    data class Configured(val code: RegisterCode) : RegisterState
}

@HiltViewModel
class AppViewModel @Inject constructor(registerRepository: RegisterRepository) : ViewModel() {
    val registerState: StateFlow<RegisterState> = registerRepository.observeRegisterCode()
        .map { code -> if (code == null) RegisterState.NotConfigured else RegisterState.Configured(code) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RegisterState.Loading)
}

/** Racine : configuration de la caisse au premier lancement, puis caisse et historique. */
@Composable
fun CaisseApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.registerState.collectAsStateWithLifecycle()
    when (val current = state) {
        RegisterState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        RegisterState.NotConfigured -> SetupScreen()
        is RegisterState.Configured -> MainNavigation(current.code)
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
