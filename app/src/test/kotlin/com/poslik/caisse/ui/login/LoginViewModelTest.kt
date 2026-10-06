package com.poslik.caisse.ui.login

import com.poslik.caisse.domain.auth.AuthError
import com.poslik.caisse.domain.auth.AuthState
import com.poslik.caisse.domain.fake.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val auth = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(auth)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun fill(email: String, password: String) {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun `invalid input is rejected without calling Firebase`() = runTest {
        fill("caisse@poslik", "secret1")
        viewModel.submit()
        assertEquals(AuthError.INVALID_EMAIL, viewModel.state.value.error)

        fill("caisse@poslik.tn", "123")
        viewModel.submit()
        assertEquals(AuthError.WEAK_PASSWORD, viewModel.state.value.error)
        assertEquals(AuthState.SignedOut, auth.authState.value)
    }

    @Test
    fun `sign up then sign in with the same credentials`() = runTest {
        viewModel.toggleMode()
        fill("caisse@poslik.tn", "secret1")
        viewModel.submit()
        assertTrue(auth.authState.value is AuthState.SignedIn)
        assertFalse(viewModel.state.value.isSubmitting)

        auth.signOut()
        viewModel.toggleMode()
        viewModel.submit()
        assertTrue(auth.authState.value is AuthState.SignedIn)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `wrong password shows an error and keeps the user signed out`() = runTest {
        auth.accounts["caisse@poslik.tn"] = "secret1"
        fill("caisse@poslik.tn", "mauvais")
        viewModel.submit()

        assertEquals(AuthError.WRONG_CREDENTIALS, viewModel.state.value.error)
        assertEquals(AuthState.SignedOut, auth.authState.value)
    }

    @Test
    fun `network failure is reported`() = runTest {
        auth.nextFailure = AuthError.NETWORK
        fill("caisse@poslik.tn", "secret1")
        viewModel.submit()

        assertEquals(AuthError.NETWORK, viewModel.state.value.error)
    }

    @Test
    fun `password reset needs a valid email and confirms the send`() = runTest {
        viewModel.sendPasswordReset()
        assertEquals(AuthError.INVALID_EMAIL, viewModel.state.value.error)

        viewModel.onEmailChange("caisse@poslik.tn")
        viewModel.sendPasswordReset()
        assertTrue(viewModel.state.value.resetEmailSent)
        assertEquals(listOf("caisse@poslik.tn"), auth.resetRequests)
    }
}
