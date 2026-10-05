package com.poslik.caisse.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poslik.caisse.R
import com.poslik.caisse.domain.model.Money
import com.poslik.caisse.domain.model.PrintStatus
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.domain.model.Sale
import com.poslik.caisse.domain.model.SyncStatus
import com.poslik.caisse.domain.model.TicketNumber
import com.poslik.caisse.ui.components.StatusBadge
import com.poslik.caisse.ui.theme.CaisseTheme
import com.poslik.caisse.ui.theme.StatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(state = state, onBack = onBack, onReprint = viewModel::reprint)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(state: HistoryUiState, onBack: () -> Unit, onReprint: (String) -> Unit) {
    Scaffold(
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
                state.sales.isEmpty() -> Text(stringResource(R.string.history_empty), Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.sales, key = { it.saleId }) { sale -> SaleRow(sale, onReprint) }
                }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: Sale, onReprint: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().testTag("sale-${sale.ticketNumber.label}")) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    TIME_FORMAT.format(Instant.ofEpochMilli(sale.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                PrintStatusBadge(sale.printStatus)
                SyncStatusBadge(sale.syncStatus)
            }
            if (sale.printStatus == PrintStatus.FAILED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sale.lastPrintError.orEmpty(),
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
        lines = emptyList(),
        total = Money(7_200),
        createdAt = 1_791_190_000_000,
        printStatus = print,
        printAttempts = 1,
        lastPrintError = if (print == PrintStatus.FAILED) "Imprimante hors ligne (simulation)" else null,
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
