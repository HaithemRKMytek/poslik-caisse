package com.poslik.caisse.domain.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

/**
 * La file d'impression tourne dans `backgroundScope`, que `advanceUntilIdle()` ne fait pas avancer :
 * on avance donc l'horloge virtuelle assez loin pour vider la file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.drainPrintQueue() {
    advanceTimeBy(DRAIN_MILLIS)
    runCurrent()
}

private const val DRAIN_MILLIS = 120_000L
