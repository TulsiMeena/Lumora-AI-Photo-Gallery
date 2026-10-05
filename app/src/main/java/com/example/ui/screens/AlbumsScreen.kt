package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlbumItem
import com.example.model.AlbumType
import com.example.model.SmartGroupCategory
import com.example.ui.components.ProceduralPhotoArtwork
import com.example.ui.theme.GalleryTheme

@Composable
fun AlbumsScreen(
  albums: List<AlbumItem>,
  smartClusters: List<SmartGroupCategory>,
  selectedFilter: String,
  onFilterSelected: (String) -> Unit,
  onAlbumClick: (AlbumItem) -> Unit,
  onCreateAlbumClick: () -> Unit,
  onRenameAlbumClick: (AlbumItem) -> Unit,
  onChangeCoverClick: (AlbumItem) -> Unit,
  onDeleteAlbumClick: (AlbumItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  val filteredAlbums = when (selectedFilter) {
    "Custom" -> albums.filter { it.isCustom }
    "Media" -> albums.filter { it.albumType in listOf(AlbumType.VIDEOS, AlbumType.FAVORITES, AlbumType.SCREENSHOTS, AlbumType.DOCUMENTS) }
    "Recent" -> albums.filter { it.albumType in listOf(AlbumType.RECENT_ADDED, AlbumType.RECENT_EDITED) }
    else -> albums
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // Albums Header
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = colors.background
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Albums & Collections",
            color = colors.primaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${albums.size} albums • Smart categorized",
            color = colors.secondaryText,
            fontSize = 12.sp
          )
        }

        Button(
          onClick = onCreateAlbumClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("btn_create_album")
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "New Album",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    // Filter Chips
    val filterTabs = listOf("All", "Custom", "Media", "Recent")
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(filterTabs) { tab ->
        val isSelected = tab == selectedFilter
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.accent else colors.surfaceVariant)
            .border(
              width = 1.dp,
              color = if (isSelected) colors.accent else colors.border,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onFilterSelected(tab) }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("album_filter_$tab")
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

    // Albums Grid
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("albums_grid")
    ) {
      // Smart Grouping Indexing Hub (Requirement 12)
      if (smartClusters.isNotEmpty() && selectedFilter == "All") {
        item(span = { GridItemSpan(2) }) {
          Column(modifier = Modifier.padding(bottom = 6.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Smart Grouping",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(smartClusters, key = { it.id }) { cluster ->
                SmartClusterPill(cluster = cluster)
              }
            }
          }
        }
      }

      // Album Cards
      items(filteredAlbums, key = { it.id }) { album ->
        AlbumCard(
          album = album,
          onClick = { onAlbumClick(album) },
          onRename = { onRenameAlbumClick(album) },
          onChangeCover = { onChangeCoverClick(album) },
          onDelete = { onDeleteAlbumClick(album) }
        )
      }
    }
  }
}

@Composable
private fun SmartClusterPill(cluster: SmartGroupCategory) {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surfaceVariant)
      .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Column {
      Text(
        text = cluster.title,
        color = colors.primaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = cluster.subtitle,
        color = colors.secondaryText,
        fontSize = 10.sp
      )
    }
  }
}

@Composable
fun AlbumCard(
  album: AlbumItem,
  onClick: () -> Unit,
  onRename: () -> Unit = {},
  onChangeCover: () -> Unit = {},
  onDelete: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  var showMenu by remember { mutableStateOf(false) }

  val albumIcon: ImageVector = when (album.albumType) {
    AlbumType.CAMERA -> Icons.Rounded.CameraAlt
    AlbumType.VIDEOS -> Icons.Rounded.Videocam
    AlbumType.FAVORITES -> Icons.Rounded.Favorite
    AlbumType.SCREENSHOTS -> Icons.Rounded.Screenshot
    AlbumType.DOWNLOADS -> Icons.Rounded.FileDownload
    AlbumType.DOCUMENTS -> Icons.Rounded.PictureAsPdf
    AlbumType.RECENT_ADDED -> Icons.Rounded.Schedule
    AlbumType.RECENT_EDITED -> Icons.Rounded.History
    AlbumType.CUSTOM -> Icons.Rounded.FolderSpecial
    else -> Icons.Rounded.Folder
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("album_card_${album.id}")
  ) {
    // Thumbnail Box with stacked photo effect
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp))
        .background(colors.card)
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      // Thumbnail Artwork or Real Media Cover
      if (!album.coverUri.isNullOrBlank()) {
        coil.compose.AsyncImage(
          model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(album.coverUri)
            .size(360, 360)
            .crossfade(true)
            .build(),
          contentDescription = album.name,
          contentScale = androidx.compose.ui.layout.ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        ProceduralPhotoArtwork(
          seed = album.colorSeed,
          category = album.name,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Count badge
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color.Black.copy(alpha = 0.65f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${album.count}",
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Type Icon in top corner
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp)
          .size(28.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = albumIcon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
      }

      // Overflow menu for Custom Albums (Rename, Change Cover, Delete)
      if (album.isCustom) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
        ) {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = "Album actions",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Rename Album") },
              leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
              onClick = {
                showMenu = false
                onRename()
              }
            )
            DropdownMenuItem(
              text = { Text("Change Cover") },
              leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
              onClick = {
                showMenu = false
                onChangeCover()
              }
            )
            DropdownMenuItem(
              text = { Text("Delete Album", color = colors.error) },
              leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
              onClick = {
                showMenu = false
                onDelete()
              }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = album.name,
      color = colors.primaryText,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1
    )

    Text(
      text = album.description.ifEmpty { "${album.count} items" },
      color = colors.secondaryText,
      fontSize = 11.sp,
      maxLines = 1
    )
  }
}
