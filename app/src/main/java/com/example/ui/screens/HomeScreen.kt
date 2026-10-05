package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MediaTypeFilter
import com.example.model.SmartGroupCategory
import com.example.model.SortOption
import com.example.model.TimelineSection
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GalleryTopBar
import com.example.ui.components.MediaThumbnailCard
import com.example.ui.components.ProceduralPhotoArtwork
import com.example.ui.theme.GalleryTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  photos: List<MediaItem>,
  recentPhotos: List<MediaItem>,
  timelineSections: List<TimelineSection>,
  smartClusters: List<SmartGroupCategory>,
  selectedIds: Set<String>,
  isSelectionMode: Boolean,
  gridColumns: Int,
  searchQuery: String,
  filterState: FilterState,
  sortOption: SortOption,
  onSearchQueryChange: (String) -> Unit,
  onOpenFilters: () -> Unit,
  onSortOptionSelected: (SortOption) -> Unit,
  onQuickFilterSelected: (String) -> Unit,
  onResetFilters: () -> Unit,
  onPhotoClick: (MediaItem) -> Unit,
  onPhotoLongClick: (MediaItem) -> Unit,
  onToggleSelectionMode: () -> Unit,
  onSelectAll: () -> Unit,
  onDeleteSelected: () -> Unit,
  onShareSelected: () -> Unit,
  onBulkFavoriteSelected: () -> Unit,
  onAddToAlbumSelected: () -> Unit,
  onCreatePdfSelected: (() -> Unit)? = null,
  onCompressSelected: (() -> Unit)? = null,
  onEditSelected: (() -> Unit)? = null,
  onAiEditSelected: (() -> Unit)? = null,
  onShowSelectionDetails: () -> Unit,
  onMoveToVaultSelected: (() -> Unit)? = null,
  onRemoveFromGallerySelected: () -> Unit,
  onGridColumnsCycle: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenAiSearch: () -> Unit,
  onNavigateToVault: () -> Unit,
  onNavigateToDocuments: () -> Unit,
  onNavigateToCreativeStudio: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // Top Bar with real-time search, multi-selection actions, sort dropdown and filter trigger
    GalleryTopBar(
      title = "My Gallery AI",
      subtitle = "${photos.size} items • Organized",
      isSelectionMode = isSelectionMode,
      selectedCount = selectedIds.size,
      gridColumns = gridColumns,
      searchQuery = searchQuery,
      activeFilterCount = filterState.activeFilterCount + (if (searchQuery.isNotEmpty()) 1 else 0),
      currentSortOption = sortOption,
      onSearchQueryChange = onSearchQueryChange,
      onGridColumnsCycle = onGridColumnsCycle,
      onToggleSelectionMode = onToggleSelectionMode,
      onSelectAll = onSelectAll,
      onDeleteSelected = onDeleteSelected,
      onShareSelected = onShareSelected,
      onBulkFavoriteSelected = onBulkFavoriteSelected,
      onAddToAlbumSelected = onAddToAlbumSelected,
      onCreatePdfSelected = onCreatePdfSelected,
      onCompressSelected = onCompressSelected,
      onEditSelected = onEditSelected,
      onAiEditSelected = onAiEditSelected,
      onShowSelectionDetails = onShowSelectionDetails,
      onMoveToVaultSelected = onMoveToVaultSelected,
      onRemoveFromGallerySelected = onRemoveFromGallerySelected,
      onOpenFilters = onOpenFilters,
      onSortOptionSelected = onSortOptionSelected,
      onOpenSettings = onOpenSettings,
      onSearchClick = onOpenAiSearch
    )

    // Main Content Grid
    LazyVerticalGrid(
      columns = GridCells.Fixed(gridColumns),
      contentPadding = PaddingValues(
        start = 12.dp,
        end = 12.dp,
        top = 4.dp,
        bottom = 96.dp
      ),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("home_photo_grid")
    ) {
      // SMART HOME SECTION
      item(span = { GridItemSpan(gridColumns) }) {
        Column {
          // 1. Quick Access Hub (Smart Albums, Documents, Vault)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            QuickHubCard(
              title = "Creative",
              subtitle = "Collage & Batch",
              icon = Icons.Rounded.AutoAwesome,
              badgeColor = Color(0xFFA855F7),
              onClick = onNavigateToCreativeStudio,
              modifier = Modifier.weight(1f)
            )
            QuickHubCard(
              title = "Documents",
              subtitle = "Scans & Text",
              icon = Icons.Rounded.PictureAsPdf,
              badgeColor = colors.accent,
              onClick = onNavigateToDocuments,
              modifier = Modifier.weight(1f)
            )
            QuickHubCard(
              title = "Private Vault",
              subtitle = "Secure Lock",
              icon = Icons.Rounded.Lock,
              badgeColor = colors.warning,
              onClick = onNavigateToVault,
              modifier = Modifier.weight(1f)
            )
          }

          // 2. Recent Highlights Strip (when no active deep filter)
          if (!filterState.isActive && searchQuery.isEmpty() && recentPhotos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Recent Highlights",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${recentPhotos.size} new",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(recentPhotos.take(8), key = { "recent_${it.id}" }) { photo ->
                Box(
                  modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onPhotoClick(photo) }
                ) {
                  if (!photo.uri.isNullOrBlank()) {
                    AsyncImage(
                      model = ImageRequest.Builder(context)
                        .data(photo.uri)
                        .size(150, 150)
                        .crossfade(true)
                        .build(),
                      contentDescription = photo.title,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  } else {
                    ProceduralPhotoArtwork(
                      seed = photo.colorSeed,
                      category = photo.categoryTag,
                      modifier = Modifier.fillMaxSize()
                    )
                  }
                  if (photo.mediaType == MediaType.VIDEO) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.Center)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          // 3. Quick Filter Tabs
          val filterTabs = listOf("All Media", "Photos", "Videos", "Favorites", "Edited", "Screenshots")
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            items(filterTabs) { tab ->
              val isSelected = when (tab) {
                "All Media" -> !filterState.isActive
                "Photos" -> filterState.mediaType == MediaTypeFilter.PHOTOS
                "Videos" -> filterState.mediaType == MediaTypeFilter.VIDEOS
                "Favorites" -> filterState.onlyFavorites
                "Edited" -> filterState.onlyEdited
                "Screenshots" -> filterState.onlyScreenshots
                else -> false
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isSelected) colors.accent else colors.surfaceVariant)
                  .border(
                    width = 1.dp,
                    color = if (isSelected) colors.accent else colors.border,
                    shape = RoundedCornerShape(10.dp)
                  )
                  .clickable { onQuickFilterSelected(tab) }
                  .padding(horizontal = 14.dp, vertical = 6.dp)
                  .testTag("quick_tab_$tab")
              ) {
                Text(
                  text = tab,
                  color = if (isSelected) Color.White else colors.primaryText,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }

          // 4. Active Multi-Filter Badges Strip (e.g. September + Videos + Favorites)
          if (filterState.isActive || searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
            ) {
              if (searchQuery.isNotEmpty()) {
                ActiveFilterBadge(label = "\"$searchQuery\"", onRemove = { onSearchQueryChange("") })
              }
              if (filterState.mediaType != MediaTypeFilter.ALL) {
                ActiveFilterBadge(label = filterState.mediaType.label, onRemove = { onQuickFilterSelected("All Media") })
              }
              if (filterState.onlyFavorites) {
                ActiveFilterBadge(label = "Favorites", onRemove = { onQuickFilterSelected("Favorites") })
              }
              if (filterState.onlyEdited) {
                ActiveFilterBadge(label = "Edited", onRemove = { onQuickFilterSelected("Edited") })
              }
              if (filterState.onlyScreenshots) {
                ActiveFilterBadge(label = "Screenshots", onRemove = { onQuickFilterSelected("Screenshots") })
              }
              if (filterState.onlyDocuments) {
                ActiveFilterBadge(label = "Documents", onRemove = { onQuickFilterSelected("Documents") })
              }
              if (filterState.fileType != "All") {
                ActiveFilterBadge(label = filterState.fileType, onRemove = onResetFilters)
              }
              if (filterState.dateRange != com.example.model.DateRangeFilter.ALL) {
                ActiveFilterBadge(label = filterState.dateRange.label, onRemove = onResetFilters)
              }
              if (filterState.selectedFolder != "All") {
                ActiveFilterBadge(label = filterState.selectedFolder, onRemove = onResetFilters)
              }

              // Reset All Pill
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(colors.surfaceVariant)
                  .clickable { onResetFilters() }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Clear All",
                  color = colors.secondaryText,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // PHOTOS CONTENT
      if (photos.isEmpty()) {
        item(span = { GridItemSpan(gridColumns) }) {
          EmptyStateView(
            icon = Icons.Rounded.FilterList,
            title = "No Matching Photos",
            description = "Try clearing search filters or changing sort criteria.",
            actionButtonText = "Reset All Filters",
            onActionClick = onResetFilters
          )
        }
      } else if (!filterState.isActive && searchQuery.isEmpty() && timelineSections.isNotEmpty()) {
        // Chronological Timeline view with group headers
        timelineSections.forEach { section ->
          item(span = { GridItemSpan(gridColumns) }, key = "timeline_header_${section.title}") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.CalendarToday,
                  contentDescription = null,
                  tint = colors.accent,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = section.title,
                  color = colors.primaryText,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                text = "${section.count} ${if (section.count == 1) "item" else "items"}",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }
          }

          items(items = section.items, key = { it.id }) { photo ->
            val isSelected = selectedIds.contains(photo.id)
            MediaThumbnailCard(
              item = photo,
              isSelected = isSelected,
              isSelectionMode = isSelectionMode,
              aspectRatio = 1f,
              onClick = {
                if (isSelectionMode) {
                  onPhotoLongClick(photo)
                } else {
                  onPhotoClick(photo)
                }
              },
              onLongClick = { onPhotoLongClick(photo) }
            )
          }
        }
      } else {
        // Filtered / Sorted flat grid
        item(span = { GridItemSpan(gridColumns) }) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Results (${photos.size})",
              color = colors.primaryText,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Sorted by ${sortOption.displayName}",
              color = colors.secondaryText,
              fontSize = 11.sp
            )
          }
        }

        items(items = photos, key = { it.id }) { photo ->
          val isSelected = selectedIds.contains(photo.id)
          MediaThumbnailCard(
            item = photo,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            aspectRatio = 1f,
            onClick = {
              if (isSelectionMode) {
                onPhotoLongClick(photo)
              } else {
                onPhotoClick(photo)
              }
            },
            onLongClick = { onPhotoLongClick(photo) }
          )
        }
      }
    }
  }
}

@Composable
private fun ActiveFilterBadge(
  label: String,
  onRemove: () -> Unit
) {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(colors.accentGlow)
      .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = label,
        color = colors.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.Rounded.Close,
        contentDescription = "Remove $label",
        tint = colors.accent,
        modifier = Modifier
          .size(13.dp)
          .clickable { onRemove() }
      )
    }
  }
}

@Composable
private fun QuickHubCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  badgeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(colors.surface)
      .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(12.dp)
      .testTag("hub_card_${title.lowercase().replace(" ", "_")}")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(badgeColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = badgeColor,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          color = colors.primaryText,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = subtitle,
          color = colors.secondaryText,
          fontSize = 10.sp
        )
      }
    }
  }
}
