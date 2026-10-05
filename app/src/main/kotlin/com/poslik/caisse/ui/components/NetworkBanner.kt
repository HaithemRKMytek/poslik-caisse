package com.poslik.caisse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.poslik.caisse.R
import com.poslik.caisse.ui.theme.StatusColors

/** Informe le caissier de l'état réseau et de la file de synchronisation, sans jamais le bloquer. */
@Composable
fun NetworkBanner(isOnline: Boolean, unsyncedCount: Int, modifier: Modifier = Modifier) {
    val (text, color) = when {
        !isOnline -> stringResource(R.string.network_offline) to StatusColors.Warning
        unsyncedCount > 0 -> stringResource(R.string.network_pending, unsyncedCount) to StatusColors.Warning
        else -> stringResource(R.string.network_synced) to StatusColors.Success
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
