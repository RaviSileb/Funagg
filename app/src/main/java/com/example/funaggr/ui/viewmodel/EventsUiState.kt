package com.example.funaggr.ui.viewmodel

import com.example.funaggr.data.model.Event
import com.example.funaggr.data.model.EventType
import com.example.funaggr.data.model.LocationData
import com.example.funaggr.data.repository.SourceReport

sealed interface RefreshStatus {
    data object Idle : RefreshStatus
    data object Locating : RefreshStatus
    data object Loading : RefreshStatus

    /** Fresh data downloaded at [time]; [reports] lists per-source results. */
    data class Updated(val time: String, val reports: List<SourceReport>) : RefreshStatus

    /** Cached data from [time] reused without downloading. */
    data class Cached(val time: String) : RefreshStatus

    /** All sources failed; older cached data from [cachedAt] is shown. */
    data class Offline(val cachedAt: String?, val reports: List<SourceReport>) : RefreshStatus

    /** All sources failed and there is nothing cached. */
    data class Error(val message: String) : RefreshStatus
}

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val counts: Map<EventType, Int> = emptyMap(),
    val selectedFilter: EventType = EventType.ALL,
    val status: RefreshStatus = RefreshStatus.Idle,
    val location: LocationData? = null
) {
    val isRefreshing: Boolean
        get() = status is RefreshStatus.Loading || status is RefreshStatus.Locating
}
