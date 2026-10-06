package com.poslik.caisse.ui.pos

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.poslik.caisse.ui.components.FeedbackMessage
import com.poslik.caisse.ui.components.NetworkBanner
import com.poslik.caisse.ui.components.PrintAlertBanner
import com.poslik.caisse.ui.theme.CaisseTheme
import com.poslik.caisse.ui.theme.StatusColors

@Composable
fun PosScreen(registerCode: RegisterCode, onOpenHistory: () -> Unit, viewModel: PosViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Les actions ne dépendent que du ViewModel et du callback de navigation : on évite de les recréer à chaque recomposition.
    val actions = remember(viewModel, onOpenHistory) {
        PosActions(
            onAddProduct = viewModel::addProduct,
            onRemoveOne = viewModel::removeOne,
            onClearCart = viewModel::clearCart,
            onCheckout = viewModel::checkout,
            onSyncNow = viewModel::syncNow,
            onPrinterFailureModeChange = viewModel::setPrinterFailureMode,
            onOpenHistory = onOpenHistory,
            onSignOut = viewModel::signOut,
        )
    }
    PosContent(registerCode = registerCode.value, state = state, actions = actions)
}

data class PosActions(
    val onAddProduct: (Product) -> Unit = {},
    val onRemoveOne: (String) -> Unit = {},
    val onClearCart: () -> Unit = {},
    val onCheckout: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
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
                    // Interrupteur de démonstration : le libellé n'apparaît que si la barre a la place de l'afficher.
                    if (LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH.value) {
                        Text(stringResource(R.string.pos_printer_failure), style = MaterialTheme.typography.labelMedium)
                    }
                    val failureLabel = stringResource(R.string.pos_printer_failure)
                    Switch(
                        checked = state.printerFailureMode,
                        onCheckedChange = actions.onPrinterFailureModeChange,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .testTag("printer-failure")
                            .semantics { contentDescription = failureLabel },
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
            NetworkBanner(
                isOnline = state.isOnline,
                unsyncedCount = state.unsyncedCount,
                isSyncing = state.isSyncing,
                onSyncNow = actions.onSyncNow,
            )
            PrintAlertBanner(failedCount = state.failedPrintCount, onOpenHistory = actions.onOpenHistory)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= WIDE_LAYOUT_MIN_WIDTH) {
                    Row(Modifier.fillMaxSize()) {
                        ProductGrid(state, actions.onAddProduct, Modifier.weight(3f).fillMaxHeight())
                        CartPanel(state, actions, Modifier.weight(2f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        ProductGrid(state, actions.onAddProduct, Modifier.weight(1f).fillMaxWidth())
                        CartPanel(state, actions, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(state: PosUiState, onAdd: (Product) -> Unit, modifier: Modifier) {
    val quantities = remember(state.cart) { state.quantities }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(state.products, key = { it.id }, contentType = { "product" }) { product ->
            ProductCard(product = product, quantity = quantities[product.id] ?: 0, onAdd = onAdd)
        }
    }
}

@Composable
private fun ProductCard(product: Product, quantity: Int, onAdd: (Product) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val inCart = quantity > 0
    val description = if (inCart) stringResource(R.string.pos_product_in_cart, product.name, quantity) else product.name
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onAdd(product)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (inCart) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (inCart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .testTag("product-${product.id}")
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = PRODUCT_CARD_MIN_HEIGHT).padding(16.dp)) {
            Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    product.price.format(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (inCart) {
                QuantityBadge(quantity, Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun QuantityBadge(quantity: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun CartPanel(state: PosUiState, actions: PosActions, modifier: Modifier) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            CartHeader(state, actions.onClearCart)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.cart.isEmpty) {
                    EmptyCart(Modifier.align(Alignment.Center))
                } else {
                    CartLines(state.cart.lines, actions)
                }
            }
            CartFooter(state, actions.onCheckout)
        }
    }
}

@Composable
private fun CartHeader(state: PosUiState, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.pos_cart), style = MaterialTheme.typography.titleLarge)
        if (!state.cart.isEmpty) {
            Text(
                pluralStringResource(R.plurals.pos_cart_items, state.cart.itemCount, state.cart.itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        TextButton(onClick = onClear, enabled = !state.cart.isEmpty && !state.isCheckingOut, modifier = Modifier.testTag("clear-cart")) {
            Text(stringResource(R.string.pos_clear_cart))
        }
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.pos_cart_empty), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.pos_cart_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CartLines(lines: List<CartLine>, actions: PosActions) {
    val listState = rememberLazyListState()
    // Garde la dernière ligne ajoutée visible quand le panier dépasse l'écran.
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(lines, key = { it.product.id }, contentType = { "line" }) { line ->
            Column(Modifier.animateItem()) {
                CartLineRow(line, actions)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, actions: PosActions) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                line.product.price.format(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // La dernière unité se retire avec une corbeille : le geste annonce que la ligne disparaît.
        val removeLabel = stringResource(R.string.pos_remove_one)
        IconButton(
            onClick = { actions.onRemoveOne(line.product.id) },
            modifier = Modifier.semantics { contentDescription = removeLabel },
        ) {
            if (line.quantity > 1) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            } else {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
        Text(
            line.quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        IconButton(onClick = { actions.onAddProduct(line.product) }, enabled = line.quantity < Cart.MAX_LINE_QUANTITY) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.pos_add_one))
        }
        Text(
            line.subtotal.format(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 88.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun CartFooter(state: PosUiState, onCheckout: () -> Unit) {
    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pos_total), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(state.cart.total.format(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onCheckout,
            enabled = state.canCheckout,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("checkout"),
        ) {
            if (state.isCheckingOut) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    stringResource(R.string.pos_checking_out),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                Text(stringResource(R.string.pos_checkout, state.cart.total.format()), style = MaterialTheme.typography.titleMedium)
            }
        }
        state.lastTicket?.let {
            FeedbackMessage(stringResource(R.string.pos_last_ticket, it), StatusColors.Success, Modifier.testTag("last-ticket"))
        }
        state.checkoutError?.let {
            FeedbackMessage(stringResource(R.string.pos_checkout_error, it), StatusColors.Error)
        }
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
private val PRODUCT_CARD_MIN_HEIGHT = 88.dp

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun PosTabletPreview() {
    val cart = Cart().add(Catalog.products[0]).add(Catalog.products[0]).add(Catalog.products[5])
    CaisseTheme {
        PosContent(
            "C01",
            PosUiState(cart = cart, lastTicket = "C01-000041", isOnline = true, unsyncedCount = 3, failedPrintCount = 2),
            PosActions(),
        )
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun PosPhonePreview() {
    val cart = Cart().add(Catalog.products[1]).add(Catalog.products[3])
    CaisseTheme {
        PosContent("C01", PosUiState(cart = cart, checkoutError = "Disque plein"), PosActions())
    }
}

@Preview(widthDp = 1280, heightDp = 800, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PosDarkPreview() {
    CaisseTheme { PosContent("C01", PosUiState(isCheckingOut = true, cart = Cart().add(Catalog.products[0])), PosActions()) }
}
