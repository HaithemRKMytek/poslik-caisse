package com.poslik.caisse.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poslik.caisse.R
import com.poslik.caisse.domain.auth.AuthError
import com.poslik.caisse.domain.auth.Credentials
import com.poslik.caisse.ui.theme.CaisseTheme
import com.poslik.caisse.ui.theme.StatusColors

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginContent(
        state = state,
        actions = LoginActions(
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::submit,
            onToggleMode = viewModel::toggleMode,
            onForgotPassword = viewModel::sendPasswordReset,
        ),
    )
}

data class LoginActions(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onToggleMode: () -> Unit = {},
    val onForgotPassword: () -> Unit = {},
)

@Composable
fun LoginContent(state: LoginUiState, actions: LoginActions) {
    val isSignUp = state.mode == LoginMode.SIGN_UP
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(if (isSignUp) R.string.login_title_sign_up else R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(stringResource(R.string.login_explanation), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = state.email,
                onValueChange = actions.onEmailChange,
                label = { Text(stringResource(R.string.login_email)) },
                singleLine = true,
                isError = state.error == AuthError.INVALID_EMAIL,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("login-email"),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = actions.onPasswordChange,
                label = { Text(stringResource(R.string.login_password)) },
                singleLine = true,
                isError = state.error == AuthError.WEAK_PASSWORD,
                supportingText = if (isSignUp) {
                    @Composable { Text(stringResource(R.string.login_password_hint, Credentials.MIN_PASSWORD_LENGTH)) }
                } else {
                    null
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { actions.onSubmit() }),
                modifier = Modifier.fillMaxWidth().testTag("login-password"),
            )
            state.error?.let {
                Text(it.message(), color = StatusColors.Error, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.resetEmailSent) {
                Text(stringResource(R.string.login_reset_sent), color = StatusColors.Success, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = actions.onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("login-submit"),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(if (isSignUp) R.string.login_submit_sign_up else R.string.login_submit))
                }
            }
            TextButton(onClick = actions.onToggleMode, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isSignUp) R.string.login_switch_to_sign_in else R.string.login_switch_to_sign_up))
            }
            if (!isSignUp) {
                TextButton(onClick = actions.onForgotPassword, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.login_forgot_password))
                }
            }
        }
    }
}

@Composable
private fun AuthError.message(): String = stringResource(
    when (this) {
        AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
        AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
        AuthError.WRONG_CREDENTIALS -> R.string.auth_error_wrong_credentials
        AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
        AuthError.NETWORK -> R.string.auth_error_network
        AuthError.TOO_MANY_ATTEMPTS -> R.string.auth_error_too_many_attempts
        AuthError.UNKNOWN -> R.string.auth_error_unknown
    },
)

@Preview(widthDp = 800, heightDp = 600)
@Composable
private fun LoginPreview() {
    CaisseTheme {
        LoginContent(LoginUiState(email = "caisse@poslik.tn", error = AuthError.WRONG_CREDENTIALS), LoginActions())
    }
}
