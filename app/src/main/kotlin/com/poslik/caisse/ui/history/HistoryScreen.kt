package com.poslik.caisse.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poslik.caisse.R
import com.poslik.caisse.domain.model.Money
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SaleLine
import com.poslik.caisse.domain.model.SyncStatus
import com.poslik.caisse.domain.model.TicketNumber
import com.poslik.caisse.domain.printing.PrintFailureKind
import com.poslik.caisse.ui.components.StatusBadge
import com.poslik.caisse.ui.theme.CaisseTheme
import com.poslik.caisse.ui.theme.StatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onBack = onBack,
        onReprint = viewModel::reprint,
        onReprintErrorShown = viewModel::onReprintErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(state: HistoryUiState, onBack: () -> Unit, onReprint: (String) -> Unit, onReprintErrorShown: () -> Unit = {}) {
    val snackbarHostState = remember { SnackbarHostState() }
    val reprintError = stringResource(R.string.history_reprint_error)
    LaunchedEffect(state.reprintFailed) {
        if (state.reprintFailed) {
            onReprintErrorShown()
            snackbarHostState.showSnackbar(reprintError)
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.sales.isEmpty() -> Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().align(Alignment.TopCenter),
                ) {
                    items(state.sales, key = { it.saleId }, contentType = { "sale" }) { sale ->
                        SaleRow(sale, onReprint, Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: Sale, onReprint: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().testTag("sale-${sale.ticketNumber.label}"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sale.ticketNumber.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(sale.total.format(), style = MaterialTheme.typography.titleMedium)
            }
            if (sale.lines.isNotEmpty()) {
                Text(
                    sale.lines.joinToString(", ") { "${it.quantity}× ${it.productName}" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    TIME_FORMAT.format(Instant.ofEpochMilli(sale.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                PrintStatusBadge(sale.printStatus)
                SyncStatusBadge(sale.syncStatus)
            }
            if (sale.printStatus == PrintStatus.FAILED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        PrintFailureKind.fromCode(sale.lastPrintError).message(),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusColors.Error,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { onReprint(sale.saleId) }) {
                        Text(stringResource(R.string.history_reprint))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintFailureKind.message(): String = stringResource(
    when (this) {
        PrintFailureKind.PRINTER_OFFLINE -> R.string.print_error_offline
        PrintFailureKind.OUT_OF_PAPER -> R.string.print_error_paper
        PrintFailureKind.TIMEOUT -> R.string.print_error_timeout
        PrintFailureKind.UNKNOWN -> R.string.print_error_unknown
    },
)

@Composable
private fun PrintStatusBadge(status: PrintStatus) {
    val (text, color) = when (status) {
        PrintStatus.PENDING -> stringResource(R.string.print_pending) to StatusColors.Warning
        PrintStatus.PRINTED -> stringResource(R.string.print_printed) to StatusColors.Success
        PrintStatus.FAILED -> stringResource(R.string.print_failed) to StatusColors.Error
    }
    StatusBadge(text, color)
}

@Composable
private fun SyncStatusBadge(status: SyncStatus) {
    val (text, color) = when (status) {
        SyncStatus.PENDING -> stringResource(R.string.sync_pending) to StatusColors.Warning
        SyncStatus.SYNCED -> stringResource(R.string.sync_synced) to StatusColors.Success
        SyncStatus.CONFLICT -> stringResource(R.string.sync_conflict) to StatusColors.Error
    }
    StatusBadge(text, color)
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())

@Preview(widthDp = 900, heightDp = 600)
@Composable
private fun HistoryPreview() {
    fun sale(n: Long, print: PrintStatus, sync: SyncStatus) = Sale(
        saleId = "id-$n",
        ticketNumber = TicketNumber(RegisterCode("C01"), n),
        lines = listOf(SaleLine("espresso", "Café express", Money(2_500), 2), SaleLine("croissant", "Croissant", Money(2_200), 1)),
        total = Money(7_200),
        createdAt = 1_791_190_000_000,
        printStatus = print,
        printAttempts = 1,
        lastPrintError = if (print == PrintStatus.FAILED) PrintFailureKind.PRINTER_OFFLINE.name else null,
        syncStatus = sync,
    )
    CaisseTheme {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
                sales = listOf(
                    sale(3, PrintStatus.PENDING, SyncStatus.PENDING),
                    sale(2, PrintStatus.FAILED, SyncStatus.SYNCED),
                    sale(1, PrintStatus.PRINTED, SyncStatus.SYNCED),
                ),
            ),
            onBack = {},
            onReprint = {},
        )
    }
}
