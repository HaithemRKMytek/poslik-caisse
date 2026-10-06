package com.poslik.caisse.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialsTest {
    @Test
    fun `accepts a well formed email and a long enough password`() {
        assertNull(Credentials.validate(" caisse@poslik.tn ", "secret1"))
    }

    @Test
    fun `rejects a malformed email before checking the password`() {
        assertEquals(AuthError.INVALID_EMAIL, Credentials.validate("caisse@poslik", ""))
        assertEquals(AuthError.INVALID_EMAIL, Credentials.validate("caisse poslik.tn", "secret1"))
    }

    @Test
    fun `rejects a password shorter than six characters`() {
        assertEquals(AuthError.WEAK_PASSWORD, Credentials.validate("caisse@poslik.tn", "12345"))
    }
}
