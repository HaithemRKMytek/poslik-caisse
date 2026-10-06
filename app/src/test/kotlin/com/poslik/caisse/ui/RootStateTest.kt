package com.poslik.caisse.ui

import com.poslik.caisse.domain.auth.AuthState
import com.poslik.caisse.domain.model.RegisterCode
import org.junit.Assert.assertEquals
import org.junit.Test

class RootStateTest {
    private val signedIn = AuthState.SignedIn(uid = "uid-1", email = "caisse@poslik.tn")
    private val code = RegisterCode("C01")

    @Test
    fun `signed out always goes to the login screen, even with a configured register`() {
        assertEquals(RootState.SignIn, rootStateOf(AuthState.SignedOut, null))
        assertEquals(RootState.SignIn, rootStateOf(AuthState.SignedOut, code))
    }

    @Test
    fun `signed in goes to setup, then to the register once it has a code`() {
        assertEquals(RootState.Setup, rootStateOf(signedIn, null))
        assertEquals(RootState.Ready(code), rootStateOf(signedIn, code))
    }

    @Test
    fun `without Firebase the login screen is skipped`() {
        assertEquals(RootState.Setup, rootStateOf(AuthState.Unavailable, null))
        assertEquals(RootState.Ready(code), rootStateOf(AuthState.Unavailable, code))
    }
}
