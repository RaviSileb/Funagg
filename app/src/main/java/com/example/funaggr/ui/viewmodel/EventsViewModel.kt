package com.example.funaggr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funaggr.data.model.EventType
import com.example.funaggr.data.model.LocationData
import com.example.funaggr.data.repository.EventRepository
import com.example.funaggr.data.repository.RefreshOutcome
import com.example.funaggr.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(EventType.ALL)
    private val status = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    private val location = MutableStateFlow<LocationData?>(null)

    val uiState: StateFlow<EventsUiState> = combine(
        repository.observeEvents(), selectedFilter, status, location
    ) { events, filter, status, location ->
        val counts = events.groupingBy { it.type }.eachCount() + (EventType.ALL to events.size)
        EventsUiState(
            events = events,
            filteredEvents = if (filter == EventType.ALL) events else events.filter { it.type == filter },
            counts = counts,
            selectedFilter = filter,
            status = status,
            location = location
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    private var refreshJob: Job? = null
    private var started = false

    /** Called once the location permission dialog was answered (or was not needed). */
    fun onPermissionResult(granted: Boolean) {
        if (!started) {
            started = true
            refresh(force = false)
        } else if (granted) {
            refresh(force = true)
        }
    }

    fun refresh(force: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            status.value = RefreshStatus.Locating
            val current = locationProvider.currentLocation() ?: location.value?.takeUnless { it.isFallback }
                ?: locationProvider.defaultLocation
            location.value = current

            status.value = RefreshStatus.Loading
            status.value = when (val outcome = repository.refresh(current, force = force)) {
                is RefreshOutcome.FromCache -> RefreshStatus.Cached(formatTime(outcome.fetchedAt))
                is RefreshOutcome.Fetched -> RefreshStatus.Updated(formatTime(outcome.fetchedAt), outcome.reports)
                is RefreshOutcome.Failed ->
                    if (outcome.hasCache) {
                        RefreshStatus.Offline(outcome.cachedAt?.let(::formatDateTime), outcome.reports)
                    } else {
                        RefreshStatus.Error(
                            outcome.reports.joinToString("; ") { "${it.name}: ${it.error ?: "bez dat"}" }
                        )
                    }
            }
        }
    }

    fun setFilter(filter: EventType) {
        selectedFilter.value = filter
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    private fun formatDateTime(millis: Long): String =
        SimpleDateFormat("d. M. HH:mm", Locale.getDefault()).format(Date(millis))
}
