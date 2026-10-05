package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SortOption
import com.example.ui.theme.GalleryTheme

@Composable
fun GalleryTopBar(
  title: String = "My Gallery AI",
  subtitle: String? = null,
  isSelectionMode: Boolean = false,
  selectedCount: Int = 0,
  gridColumns: Int = 3,
  searchQuery: String = "",
  activeFilterCount: Int = 0,
  currentSortOption: SortOption = SortOption.NEWEST_FIRST,
  onSearchQueryChange: (String) -> Unit = {},
  onGridColumnsCycle: () -> Unit,
  onToggleSelectionMode: () -> Unit,
  onSelectAll: () -> Unit,
  onDeleteSelected: () -> Unit,
  onShareSelected: () -> Unit,
  onBulkFavoriteSelected: (() -> Unit)? = null,
  onAddToAlbumSelected: (() -> Unit)? = null,
  onCreatePdfSelected: (() -> Unit)? = null,
  onCompressSelected: (() -> Unit)? = null,
  onEditSelected: (() -> Unit)? = null,
  onAiEditSelected: (() -> Unit)? = null,
  onShowSelectionDetails: (() -> Unit)? = null,
  onMoveToVaultSelected: (() -> Unit)? = null,
  onRemoveFromGallerySelected: (() -> Unit)? = null,
  onOpenFilters: () -> Unit = {},
  onSortOptionSelected: (SortOption) -> Unit = {},
  onOpenSettings: () -> Unit,
  onSearchClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  var showSortDropdown by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = colors.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      if (isSelectionMode) {
        // Selection Mode Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onToggleSelectionMode,
              modifier = Modifier.testTag("btn_cancel_selection")
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancel selection",
                tint = colors.primaryText
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "$selectedCount selected",
              color = colors.primaryText,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onSelectAll,
              modifier = Modifier.testTag("btn_select_all")
            ) {
              Icon(
                imageVector = Icons.Rounded.SelectAll,
                contentDescription = "Select all",
                tint = colors.primaryText
              )
            }

            // Share Action
            IconButton(
              onClick = onShareSelected,
              enabled = selectedCount > 0,
              modifier = Modifier.testTag("btn_share_selected")
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = "Share",
                tint = if (selectedCount > 0) colors.primaryText else colors.tertiaryText
              )
            }

            // Bulk Favorite Action
            if (onBulkFavoriteSelected != null) {
              IconButton(
                onClick = onBulkFavoriteSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_bulk_favorite")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Favorite,
                  contentDescription = "Favorite",
                  tint = if (selectedCount > 0) colors.accent else colors.tertiaryText
                )
              }
            }

            // Add to Album Action
            if (onAddToAlbumSelected != null) {
              IconButton(
                onClick = onAddToAlbumSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_add_to_album")
              ) {
                Icon(
                  imageVector = Icons.Rounded.CreateNewFolder,
                  contentDescription = "Add to Album",
                  tint = if (selectedCount > 0) colors.accent else colors.tertiaryText
                )
              }
            }

            // Create PDF Action
            if (onCreatePdfSelected != null) {
              IconButton(
                onClick = onCreatePdfSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_create_pdf_selected")
              ) {
                Icon(
                  imageVector = Icons.Rounded.PictureAsPdf,
                  contentDescription = "Create PDF",
                  tint = if (selectedCount > 0) colors.accent else colors.tertiaryText
                )
              }
            }

            // Compress Selected Images Action
            if (onCompressSelected != null) {
              IconButton(
                onClick = onCompressSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_compress_selected")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Compress,
                  contentDescription = "Compress Selected",
                  tint = if (selectedCount > 0) Color(0xFF0EA5E9) else colors.tertiaryText
                )
              }
            }

            // Edit in Photo Editor Pro (when 1 item selected)
            if (selectedCount == 1 && onEditSelected != null) {
              IconButton(
                onClick = onEditSelected,
                modifier = Modifier.testTag("btn_edit_selected")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = "Edit in Photo Editor",
                  tint = colors.accent
                )
              }
            }

            // Gemini AI Photo Editor (when 1 item selected)
            if (selectedCount == 1 && onAiEditSelected != null) {
              IconButton(
                onClick = onAiEditSelected,
                modifier = Modifier.testTag("btn_ai_edit_selected")
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = "Gemini AI Photo Editor",
                  tint = Color(0xFF38BDF8)
                )
              }
            }

            // Selection Details Summary
            if (onShowSelectionDetails != null) {
              IconButton(
                onClick = onShowSelectionDetails,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_selection_details")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Info,
                  contentDescription = "Selection Details",
                  tint = if (selectedCount > 0) colors.primaryText else colors.tertiaryText
                )
              }
            }

            // Move to Vault Action
            if (onMoveToVaultSelected != null) {
              IconButton(
                onClick = onMoveToVaultSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_move_to_vault_selected")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = "Move to Private Vault",
                  tint = if (selectedCount > 0) colors.accent else colors.tertiaryText
                )
              }
            }

            // Safe Remove Action (hide from My Gallery)
            if (onRemoveFromGallerySelected != null) {
              IconButton(
                onClick = onRemoveFromGallerySelected,
                enabled = selectedCount > 0,
                modifier = Modifier.testTag("btn_safe_remove_selected")
              ) {
                Icon(
                  imageVector = Icons.Rounded.VisibilityOff,
                  contentDescription = "Remove from My Gallery",
                  tint = if (selectedCount > 0) colors.warning else colors.tertiaryText
                )
              }
            }

            // Delete Action
            IconButton(
              onClick = onDeleteSelected,
              enabled = selectedCount > 0,
              modifier = Modifier.testTag("btn_delete_selected")
            ) {
              Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = if (selectedCount > 0) colors.error else colors.tertiaryText
              )
            }
          }
        }
      } else {
        // Normal Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(colors.accent, colors.accentVariant)
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = title,
                color = colors.primaryText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
              )
              if (subtitle != null) {
                Text(
                  text = subtitle,
                  color = colors.secondaryText,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Normal
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            // Sort Dropdown Button
            Box {
              IconButton(
                onClick = { showSortDropdown = true },
                modifier = Modifier.testTag("btn_sort_menu")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Sort,
                  contentDescription = "Sort Options",
                  tint = colors.primaryText,
                  modifier = Modifier.size(22.dp)
                )
              }

              DropdownMenu(
                expanded = showSortDropdown,
                onDismissRequest = { showSortDropdown = false }
              ) {
                SortOption.entries.forEach { option ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = option.displayName,
                        fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal,
                        color = if (option == currentSortOption) colors.accent else colors.primaryText
                      )
                    },
                    onClick = {
                      showSortDropdown = false
                      onSortOptionSelected(option)
                    }
                  )
                }
              }
            }

            // Filter Sheet Trigger with badge count
            Box {
              IconButton(
                onClick = onOpenFilters,
                modifier = Modifier.testTag("btn_open_filters")
              ) {
                Icon(
                  imageVector = Icons.Rounded.FilterList,
                  contentDescription = "Filters",
                  tint = if (activeFilterCount > 0) colors.accent else colors.primaryText,
                  modifier = Modifier.size(22.dp)
                )
              }
              if (activeFilterCount > 0) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "$activeFilterCount",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            // Grid columns toggle (2 -> 3 -> 4 -> 2)
            IconButton(
              onClick = onGridColumnsCycle,
              modifier = Modifier.testTag("btn_grid_columns")
            ) {
              Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = "Grid columns ($gridColumns)",
                tint = colors.primaryText,
                modifier = Modifier.size(22.dp)
              )
            }

            // Select Mode toggle
            IconButton(
              onClick = onToggleSelectionMode,
              modifier = Modifier.testTag("btn_toggle_selection")
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Select mode",
                tint = colors.primaryText,
                modifier = Modifier.size(22.dp)
              )
            }

            // Settings button
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier.testTag("btn_open_settings")
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = colors.primaryText,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }

        // Search Bar with real-time text input
        Spacer(modifier = Modifier.height(10.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp)
            .testTag("search_bar_entry"),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "Search",
              tint = colors.secondaryText,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
              value = searchQuery,
              onValueChange = onSearchQueryChange,
              textStyle = TextStyle(
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              ),
              cursorBrush = SolidColor(colors.accent),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("search_input_field"),
              decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                  Text(
                    text = "Search photos, dates, folders, or tags...",
                    color = colors.secondaryText,
                    fontSize = 13.sp
                  )
                }
                innerTextField()
              }
            )

            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Clear,
                  contentDescription = "Clear search",
                  tint = colors.secondaryText,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accentGlow)
                .clickable { onSearchClick() }
                .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  tint = colors.accent,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "AI Search",
                  color = colors.accent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}
