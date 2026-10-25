package com.example.funaggr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.funaggr.R
import com.example.funaggr.data.model.LocationData
import com.example.funaggr.data.repository.SourceReport
import com.example.funaggr.ui.theme.StatusError
import com.example.funaggr.ui.theme.StatusSuccess
import com.example.funaggr.ui.theme.StatusWarning
import com.example.funaggr.ui.viewmodel.RefreshStatus

@Composable
fun StatusBar(
    status: RefreshStatus,
    location: LocationData?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_place),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = location?.cityName ?: "Získávání polohy…",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val (text, color) = describe(status)
        if (text != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status is RefreshStatus.Loading || status is RefreshStatus.Locating) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = if (color == null) 6.dp else 0.dp)
                )
            }
        }
    }
}

private fun describe(status: RefreshStatus): Pair<String?, Color?> = when (status) {
    RefreshStatus.Idle -> null to null
    RefreshStatus.Locating -> "Zjišťuji polohu…" to null
    RefreshStatus.Loading -> "Načítám akce z Kudy z nudy a GoOut…" to null
    is RefreshStatus.Cached -> "Aktualizováno v ${status.time}" to StatusSuccess
    is RefreshStatus.Updated -> {
        val failed = status.reports.filterNot { it.isSuccess }
        val text = "Aktualizováno v ${status.time} · ${summary(status.reports)}"
        text to if (failed.isEmpty()) StatusSuccess else StatusWarning
    }
    is RefreshStatus.Offline ->
        "Zdroje nedostupné – zobrazuji starší data${status.cachedAt?.let { " (z $it)" } ?: ""}" to StatusWarning
    is RefreshStatus.Error -> "Chyba připojení: ${status.message}" to StatusError
}

private fun summary(reports: List<SourceReport>): String = reports.joinToString(" · ") { report ->
    when {
        report.isSuccess -> "${report.name} ${report.eventCount}"
        report.fromCache -> "${report.name} nedostupný (z cache ${report.eventCount})"
        else -> "${report.name} nedostupný"
    }
}
