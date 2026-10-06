package com.poslik.caisse.ui.pos

import com.poslik.caisse.data.printing.PrinterSettings
import com.poslik.caisse.domain.auth.AuthState
import com.poslik.caisse.domain.fake.FakeAuthRepository
import com.poslik.caisse.domain.fake.FakePrinter
import com.poslik.caisse.domain.fake.InMemorySaleRepository
import com.poslik.caisse.domain.fake.drainPrintQueue
import com.poslik.caisse.domain.model.Catalog
import com.poslik.caisse.domain.model.Money
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.network.NetworkStatus
import com.poslik.caisse.domain.printing.PrintSpooler
import com.poslik.caisse.domain.sync.SyncDiagnostics
import com.poslik.caisse.domain.usecase.CheckoutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class PosViewModelTest {
    private val repository = InMemorySaleRepository()
    private val printer = FakePrinter()
    private val online = MutableStateFlow(false)
    private val auth = FakeAuthRepository(AuthState.SignedIn(uid = "uid-1", email = "caisse@poslik.tn"))
    private var syncRequests = 0
    private var syncNowRequests = 0
    private val diagnostics = SyncDiagnostics()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): PosViewModel {
        val spooler = PrintSpooler(printer, repository, backgroundScope)
        val checkout = CheckoutUseCase(repository, spooler, { syncRequests++ }, { 0L })
        val viewModel = PosViewModel(
            checkoutUseCase = checkout,
            printerSettings = PrinterSettings(),
            authRepository = auth,
            syncNowRequester = { syncNowRequests++ },
            applicationScope = this,
            syncDiagnostics = diagnostics,
            saleRepository = repository,
            networkStatus = object : NetworkStatus {
                override val isOnline = online
            },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun `checkout offline records the sale, clears the cart and shows the ticket`() = runTest {
        val viewModel = viewModel()
        viewModel.addProduct(Catalog.products[0])
        viewModel.addProduct(Catalog.products[0])
        assertEquals(Money(5_000), viewModel.state.value.cart.total)

        viewModel.checkout()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.cart.isEmpty)
        assertEquals("C01-000001", state.lastTicket)
        assertFalse(state.isOnline)
        assertEquals(1, state.unsyncedCount)
        assertEquals(1, syncRequests)

        drainPrintQueue()
        assertEquals(PrintStatus.PRINTED, repository.all.single().printStatus)
    }

    @Test
    fun `a double tap creates a single sale`() = runTest {
        val viewModel = viewModel()
        viewModel.addProduct(Catalog.products[1])

        viewModel.checkout()
        viewModel.checkout()
        advanceUntilIdle()

        assertEquals(1, repository.all.size)
    }

    @Test
    fun `syncing flag reaches the state and the sync button triggers an immediate sync`() = runTest {
        val viewModel = viewModel()

        diagnostics.started()
        assertTrue(viewModel.state.value.isSyncing)

        diagnostics.finished()
        assertFalse(viewModel.state.value.isSyncing)

        viewModel.syncNow()
        assertEquals(1, syncNowRequests)
    }

    @Test
    fun `failed prints are counted so the cashier is warned`() = runTest {
        val viewModel = viewModel()
        printer.failing = true
        viewModel.addProduct(Catalog.products[0])

        viewModel.checkout()
        advanceUntilIdle()
        drainPrintQueue()

        assertEquals(1, viewModel.state.value.failedPrintCount)
    }

    @Test
    fun `clearing the cart empties it and disables checkout`() = runTest {
        val viewModel = viewModel()
        viewModel.addProduct(Catalog.products[0])
        viewModel.addProduct(Catalog.products[1])

        viewModel.clearCart()

        assertTrue(viewModel.state.value.cart.isEmpty)
        assertFalse(viewModel.state.value.canCheckout)
    }

    @Test
    fun `empty cart cannot be checked out`() = runTest {
        val viewModel = viewModel()

        viewModel.checkout()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canCheckout)
        assertNull(viewModel.state.value.lastTicket)
        assertTrue(repository.all.isEmpty())
    }

    @Test
    fun `sign out is refused while sales are waiting for sync`() = runTest {
        online.value = true
        val viewModel = viewModel()
        viewModel.addProduct(Catalog.products[0])
        viewModel.checkout()
        advanceUntilIdle()
        assertEquals("caisse@poslik.tn", viewModel.state.value.accountEmail)

        viewModel.signOut()

        assertFalse(viewModel.state.value.canSignOut)
        assertTrue(auth.authState.value is AuthState.SignedIn)
    }

    @Test
    fun `sign out is refused offline and allowed online once everything is synced`() = runTest {
        val viewModel = viewModel()

        viewModel.signOut()
        assertTrue(auth.authState.value is AuthState.SignedIn)

        online.value = true
        viewModel.signOut()
        assertEquals(AuthState.SignedOut, auth.authState.value)
    }
}
