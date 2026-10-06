package com.poslik.caisse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poslik.caisse.R
import com.poslik.caisse.ui.theme.StatusColors

/**
 * Prévient le caissier que des tickets ne sont pas sortis de l'imprimante : sans cela, l'échec
 * ne se verrait qu'en ouvrant l'historique, et le client partirait sans son ticket.
 */
@Composable
fun PrintAlertBanner(failedCount: Int, onOpenHistory: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = failedCount > 0, modifier = modifier) {
        val color = StatusColors.Error
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.10f))
                .padding(start = 16.dp, end = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("print-alert"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(R.plurals.print_alert, failedCount, failedCount),
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.print_alert_action)) }
        }
    }
}
