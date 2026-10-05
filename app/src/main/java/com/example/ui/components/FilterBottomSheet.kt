package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DateRangeFilter
import com.example.model.FilterState
import com.example.model.MediaTypeFilter
import com.example.model.OrientationFilter
import com.example.model.SortOption
import com.example.ui.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
  filterState: FilterState,
  sortOption: SortOption,
  folderOptions: List<String>,
  onFilterChange: ((FilterState) -> FilterState) -> Unit,
  onSortChange: (SortOption) -> Unit,
  onClearFilters: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface,
    modifier = modifier.testTag("filter_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.FilterList,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Smart Filters & Sorting",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close",
            tint = colors.secondaryText
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section: Sort Order
      FilterSectionHeader("Sort Order")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SortOption.entries.forEach { option ->
          val isSelected = sortOption == option
          SelectableChip(
            label = option.displayName,
            selected = isSelected,
            onClick = { onSortChange(option) }
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: Media Type
      FilterSectionHeader("Media Type")
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        MediaTypeFilter.entries.forEach { type ->
          SelectableChip(
            label = type.label,
            selected = filterState.mediaType == type,
            onClick = {
              onFilterChange { it.copy(mediaType = type) }
            },
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: Smart Categories (Multi-select toggles)
      FilterSectionHeader("Smart Categories")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        ToggleFilterChip(
          label = "Favorites Only",
          active = filterState.onlyFavorites,
          onClick = { onFilterChange { it.copy(onlyFavorites = !it.onlyFavorites) } }
        )
        ToggleFilterChip(
          label = "Recently Edited",
          active = filterState.onlyEdited,
          onClick = { onFilterChange { it.copy(onlyEdited = !it.onlyEdited) } }
        )
        ToggleFilterChip(
          label = "Screenshots",
          active = filterState.onlyScreenshots,
          onClick = { onFilterChange { it.copy(onlyScreenshots = !it.onlyScreenshots) } }
        )
        ToggleFilterChip(
          label = "Documents / Scans",
          active = filterState.onlyDocuments,
          onClick = { onFilterChange { it.copy(onlyDocuments = !it.onlyDocuments) } }
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: Orientation
      FilterSectionHeader("Orientation")
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OrientationFilter.entries.forEach { orientation ->
          SelectableChip(
            label = orientation.label,
            selected = filterState.orientation == orientation,
            onClick = { onFilterChange { it.copy(orientation = orientation) } },
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: File Format
      FilterSectionHeader("File Format")
      val formats = listOf("All", "JPEG", "PNG", "WEBP", "MP4")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        formats.forEach { fmt ->
          SelectableChip(
            label = fmt,
            selected = filterState.fileType == fmt,
            onClick = { onFilterChange { it.copy(fileType = fmt) } }
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: Date Range
      FilterSectionHeader("Date Range")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        DateRangeFilter.entries.forEach { range ->
          SelectableChip(
            label = range.label,
            selected = filterState.dateRange == range,
            onClick = { onFilterChange { it.copy(dateRange = range) } }
          )
        }
      }

      if (folderOptions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(18.dp))
        FilterSectionHeader("Folder / Album")
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val folders = listOf("All") + folderOptions
          folders.forEach { folder ->
            SelectableChip(
              label = folder,
              selected = filterState.selectedFolder == folder,
              onClick = { onFilterChange { it.copy(selectedFolder = folder) } }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Bottom Action Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onClearFilters,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("btn_clear_filters")
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Reset All", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White
          ),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("btn_apply_filters")
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Apply Filters", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun FilterSectionHeader(title: String) {
  val colors = GalleryTheme.colors
  Text(
    text = title,
    color = colors.secondaryText,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(bottom = 8.dp)
  )
}

@Composable
private fun SelectableChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) colors.accent else colors.surfaceVariant)
      .border(
        width = 1.dp,
        color = if (selected) colors.accent else colors.border,
        shape = RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 7.dp)
      .testTag("filter_chip_$label")
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else colors.primaryText,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
private fun ToggleFilterChip(
  label: String,
  active: Boolean,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (active) colors.accentGlow else colors.surfaceVariant)
      .border(
        width = 1.dp,
        color = if (active) colors.accent else colors.border,
        shape = RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 7.dp)
      .testTag("toggle_filter_$label")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (active) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = label,
        color = if (active) colors.accent else colors.primaryText,
        fontSize = 12.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
      )
    }
  }
}
