package com.poslik.caisse.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.poslik.caisse.domain.model.Cart
import com.poslik.caisse.domain.model.CartLine
import com.poslik.caisse.domain.model.Catalog
import com.poslik.caisse.domain.model.Product
import com.poslik.caisse.domain.model.RegisterCode
import com.poslik.caisse.ui.components.NetworkBanner
import com.poslik.caisse.ui.theme.CaisseTheme
import com.poslik.caisse.ui.theme.StatusColors

@Composable
fun PosScreen(registerCode: RegisterCode, onOpenHistory: () -> Unit, viewModel: PosViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PosContent(
        registerCode = registerCode.value,
        state = state,
        actions = PosActions(
            onAddProduct = viewModel::addProduct,
            onRemoveOne = viewModel::removeOne,
            onCheckout = viewModel::checkout,
            onPrinterFailureModeChange = viewModel::setPrinterFailureMode,
            onOpenHistory = onOpenHistory,
            onSignOut = viewModel::signOut,
        ),
    )
}

data class PosActions(
    val onAddProduct: (Product) -> Unit = {},
    val onRemoveOne: (String) -> Unit = {},
    val onCheckout: () -> Unit = {},
    val onPrinterFailureModeChange: (Boolean) -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onSignOut: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosContent(registerCode: String, state: PosUiState, actions: PosActions) {
    var showSignOut by rememberSaveable { mutableStateOf(false) }
    if (showSignOut && state.accountEmail != null) {
        SignOutDialog(
            state = state,
            onConfirm = {
                showSignOut = false
                actions.onSignOut()
            },
            onDismiss = { showSignOut = false },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pos_title, registerCode)) },
                actions = {
                    Text(stringResource(R.string.pos_printer_failure), style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = state.printerFailureMode,
                        onCheckedChange = actions.onPrinterFailureModeChange,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(onClick = actions.onOpenHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.pos_history))
                    }
                    if (state.accountEmail != null) {
                        IconButton(onClick = { showSignOut = true }, modifier = Modifier.testTag("sign-out")) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.pos_sign_out))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            NetworkBanner(isOnline = state.isOnline, unsyncedCount = state.unsyncedCount)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= WIDE_LAYOUT_MIN_WIDTH) {
                    Row(Modifier.fillMaxSize()) {
                        ProductGrid(state.products, actions.onAddProduct, Modifier.weight(3f).fillMaxHeight())
                        CartPanel(state, actions, Modifier.weight(2f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        ProductGrid(state.products, actions.onAddProduct, Modifier.weight(1f).fillMaxWidth())
                        CartPanel(state, actions, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(products: List<Product>, onAdd: (Product) -> Unit, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(products, key = { it.id }) { product ->
            Card(onClick = { onAdd(product) }, modifier = Modifier.testTag("product-${product.id}")) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        product.price.format(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartPanel(state: PosUiState, actions: PosActions, modifier: Modifier) {
    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.pos_cart), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (state.cart.isEmpty) {
                Text(
                    stringResource(R.string.pos_cart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(state.cart.lines, key = { it.product.id }) { line ->
                        CartLineRow(line, actions)
                        HorizontalDivider()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pos_total), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(state.cart.total.format(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = actions.onCheckout,
                enabled = state.canCheckout,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("checkout"),
            ) {
                Text(stringResource(R.string.pos_checkout, state.cart.total.format()), style = MaterialTheme.typography.titleMedium)
            }
            state.lastTicket?.let {
                Text(
                    stringResource(R.string.pos_last_ticket, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusColors.Success,
                    modifier = Modifier.padding(top = 8.dp).testTag("last-ticket"),
                )
            }
            state.checkoutError?.let {
                Text(
                    stringResource(R.string.pos_checkout_error, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusColors.Error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, actions: PosActions) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.product.name, style = MaterialTheme.typography.bodyLarge)
            Text("${line.quantity} × ${line.product.price.format()}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { actions.onRemoveOne(line.product.id) }) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.pos_remove_one))
        }
        IconButton(onClick = { actions.onAddProduct(line.product) }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.pos_add_one))
        }
        Text(
            line.subtotal.format(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(96.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun SignOutDialog(state: PosUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sign_out_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.sign_out_account, state.accountEmail.orEmpty()))
                Text(
                    when {
                        !state.isOnline -> stringResource(R.string.sign_out_blocked_offline)
                        state.unsyncedCount > 0 -> stringResource(R.string.sign_out_blocked_pending, state.unsyncedCount)
                        else -> stringResource(R.string.sign_out_warning)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.canSignOut) { Text(stringResource(R.string.sign_out_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.sign_out_cancel)) }
        },
    )
}

private val WIDE_LAYOUT_MIN_WIDTH = 720.dp

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun PosTabletPreview() {
    val cart = Cart().add(Catalog.products[0]).add(Catalog.products[0]).add(Catalog.products[5])
    CaisseTheme {
        PosContent("C01", PosUiState(cart = cart, lastTicket = "C01-000041", isOnline = false, unsyncedCount = 3), PosActions())
    }
}
