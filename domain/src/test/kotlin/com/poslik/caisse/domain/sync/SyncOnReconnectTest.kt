package com.poslik.caisse.domain.sync

import com.poslik.caisse.domain.network.NetworkStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncOnReconnectTest {
    private val online = MutableStateFlow(true)
    private var syncs = 0

    private fun kotlinx.coroutines.test.TestScope.start() {
        val network = object : NetworkStatus {
            override val isOnline = online
        }
        val watcher = SyncOnReconnect(network) { syncs++ }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { watcher.run() }
    }

    @Test
    fun `starting online does not trigger a sync`() = runTest {
        start()

        assertEquals(0, syncs)
    }

    @Test
    fun `coming back online triggers one sync, going offline none`() = runTest {
        start()

        online.value = false
        assertEquals(0, syncs)

        online.value = true
        assertEquals(1, syncs)
    }

    @Test
    fun `starting offline then connecting triggers a sync`() = runTest {
        online.value = false
        start()

        online.value = true

        assertEquals(1, syncs)
    }
}
