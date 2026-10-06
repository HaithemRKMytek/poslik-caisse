package com.poslik.caisse.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poslik.caisse.R
import com.poslik.caisse.ui.theme.StatusColors

/**
 * Informe le caissier de l'état réseau et de la file de synchronisation, sans jamais le bloquer.
 * Quand des ventes attendent malgré le réseau, il peut relancer l'envoi.
 */
@Composable
fun NetworkBanner(
    isOnline: Boolean,
    unsyncedCount: Int,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false,
    onSyncNow: () -> Unit = {},
) {
    val canSync = isOnline && unsyncedCount > 0
    val (text, color) = when {
        !isOnline -> stringResource(R.string.network_offline) to StatusColors.Warning
        unsyncedCount > 0 -> stringResource(R.string.network_pending, unsyncedCount) to StatusColors.Warning
        else -> stringResource(R.string.network_synced) to StatusColors.Success
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f))
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (canSync) {
                TextButton(onClick = onSyncNow, enabled = !isSyncing, modifier = Modifier.testTag("sync-now")) {
                    if (isSyncing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
                        Text(stringResource(R.string.network_syncing), Modifier.padding(start = 8.dp))
                    } else {
                        Text(stringResource(R.string.network_sync_now))
                    }
                }
            }
        }
    }
}
