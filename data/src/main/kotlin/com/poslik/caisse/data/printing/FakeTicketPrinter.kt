package com.poslik.caisse.data.printing

import android.util.Log
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.printing.PrintResult
import com.poslik.caisse.domain.printing.TicketFormatter
import com.poslik.caisse.domain.printing.TicketPrinter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Réglages de démonstration de l'imprimante simulée.
 * Volontairement en mémoire : après un redémarrage l'imprimante « remarche », ce qui permet
 * de vérifier que les tickets en échec repartent bien à l'impression.
 */
@Singleton
class PrinterSettings @Inject constructor() {
    private val _failureMode = MutableStateFlow(false)
    val failureMode: StateFlow<Boolean> = _failureMode.asStateFlow()

    fun setFailureMode(enabled: Boolean) {
        _failureMode.value = enabled
    }
}

/** Imprimante simulée : met le temps d'une vraie impression et échoue à la demande. */
@Singleton
class FakeTicketPrinter @Inject constructor(private val settings: PrinterSettings) : TicketPrinter {

    override suspend fun print(sale: Sale): PrintResult {
        delay(PRINT_DURATION_MILLIS)
        if (settings.failureMode.value) return PrintResult.Failure("Imprimante hors ligne (simulation)")
        val date = DATE_FORMAT.format(Instant.ofEpochMilli(sale.createdAt))
        Log.i(TAG, "\n" + TicketFormatter.format(sale, date))
        return PrintResult.Success
    }

    companion object {
        private const val TAG = "FakeTicketPrinter"
        private const val PRINT_DURATION_MILLIS = 1_500L
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    }
}
