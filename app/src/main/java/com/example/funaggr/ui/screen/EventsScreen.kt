package com.example.funaggr.ui.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funaggr.R
import com.example.funaggr.data.model.EventType
import com.example.funaggr.ui.components.EventCard
import com.example.funaggr.ui.components.FilterBar
import com.example.funaggr.ui.components.StatusBar
import com.example.funaggr.ui.viewmodel.EventsUiState
import com.example.funaggr.ui.viewmodel.EventsViewModel
import com.example.funaggr.ui.viewmodel.RefreshStatus

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(viewModel: EventsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> viewModel.onPermissionResult(result.values.any { it }) }

    LaunchedEffect(Unit) {
        when {
            hasLocationPermission(context) -> viewModel.onPermissionResult(true)
            !permissionAsked -> {
                permissionAsked = true
                permissionLauncher.launch(locationPermissions)
            }
            else -> viewModel.onPermissionResult(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Funaggr – Místní akce") },
                actions = {
                    IconButton(onClick = { viewModel.refresh(force = true) }, enabled = !state.isRefreshing) {
                        Icon(painterResource(R.drawable.ic_refresh), contentDescription = "Obnovit")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StatusBar(status = state.status, location = state.location)
            FilterBar(
                selectedFilter = state.selectedFilter,
                counts = state.counts,
                onFilterSelected = viewModel::setFilter
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh(force = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                EventList(state = state, onOpen = { url -> openUrl(context, url) }, onRetry = {
                    if (state.selectedFilter != EventType.ALL) viewModel.setFilter(EventType.ALL)
                    else viewModel.refresh(force = true)
                })
            }
        }
    }
}

@Composable
private fun EventList(state: EventsUiState, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.selectedFilter) { listState.scrollToItem(0) }

    if (state.filteredEvents.isEmpty()) {
        EmptyState(state, onRetry)
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.filteredEvents, key = { it.id }) { event ->
            EventCard(event = event, onClick = { onOpen(event.sourceUrl) })
        }
    }
}

@Composable
private fun EmptyState(state: EventsUiState, onRetry: () -> Unit) {
    // A scrollable container keeps pull-to-refresh working on the empty screen.
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isRefreshing || state.status is RefreshStatus.Idle) {
                    Text("Hledám akce v okolí…", style = MaterialTheme.typography.bodyLarge)
                    return@Box
                }
                val filtered = state.selectedFilter != EventType.ALL
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (filtered) "Žádné akce pro vybraný filtr" else "Žádné akce k zobrazení",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (filtered) "Zkuste změnit filtr nebo zobrazit všechny akce"
                        else "Zkuste obnovit data",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    Button(onClick = onRetry) {
                        Text(if (filtered) "Zobrazit vše" else "Zkusit znovu")
                    }
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean = locationPermissions.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nelze otevřít odkaz", Toast.LENGTH_SHORT).show()
    }
}
