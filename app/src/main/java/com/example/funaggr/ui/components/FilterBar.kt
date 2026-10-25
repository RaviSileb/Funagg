package com.example.funaggr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.funaggr.data.model.EventType

@Composable
fun FilterBar(
    selectedFilter: EventType,
    counts: Map<EventType, Int>,
    onFilterSelected: (EventType) -> Unit,
    modifier: Modifier = Modifier
) {
    // Hide empty categories, but always keep "Vše" and the active filter visible.
    val filters = EventType.filters.filter { it == EventType.ALL || it == selectedFilter || (counts[it] ?: 0) > 0 }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters, key = { it.name }) { type ->
            val count = counts[type] ?: 0
            FilterChip(
                selected = type == selectedFilter,
                onClick = { onFilterSelected(type) },
                label = { Text(if (count > 0) "${type.displayName} ($count)" else type.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(type.color).copy(alpha = 0.22f)
                )
            )
        }
    }
}
