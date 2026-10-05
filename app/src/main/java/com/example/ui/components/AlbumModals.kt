package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AlbumItem
import com.example.model.MediaItem
import com.example.model.SelectionSummary
import com.example.ui.theme.GalleryTheme

@Composable
fun CreateAlbumDialog(
  selectedMediaCount: Int,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var albumName by remember { mutableStateOf("") }
  val colors = GalleryTheme.colors

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.FolderSpecial,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "New Album", fontWeight = FontWeight.Bold, color = colors.primaryText)
      }
    },
    text = {
      Column {
        Text(
          text = if (selectedMediaCount > 0) {
            "Creating album with $selectedMediaCount selected media items."
          } else {
            "Give your new album a name. You can add photos to it anytime."
          },
          fontSize = 13.sp,
          color = colors.secondaryText
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = albumName,
          onValueChange = { albumName = it },
          placeholder = { Text("e.g., Vacation 2026, Family, Portfolio") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_album_name"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.primaryText,
            unfocusedTextColor = colors.primaryText
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(albumName) },
        enabled = albumName.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = colors.accent,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("btn_confirm_create_album")
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = colors.secondaryText)
      }
    },
    containerColor = colors.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToAlbumBottomSheet(
  albums: List<AlbumItem>,
  onAlbumSelected: (String) -> Unit,
  onCreateNewAlbum: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface,
    modifier = Modifier.testTag("add_to_album_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Add to Album",
          color = colors.primaryText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.secondaryText)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Option: Create new album right here
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(colors.surfaceVariant)
          .clickable { onCreateNewAlbum() }
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Create New Album",
          color = colors.accent,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      val customAlbums = albums.filter { it.isCustom }
      if (customAlbums.isEmpty()) {
        Text(
          text = "No custom albums created yet. Tap above to create one!",
          color = colors.secondaryText,
          fontSize = 13.sp,
          modifier = Modifier.padding(vertical = 16.dp)
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(customAlbums, key = { it.id }) { album ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .clickable {
                  album.customAlbumId?.let { onAlbumSelected(it) }
                }
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Rounded.Folder,
                  contentDescription = null,
                  tint = colors.accent,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = album.name,
                  color = colors.primaryText,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "${album.count} items",
                  color = colors.secondaryText,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun RenameAlbumDialog(
  currentAlbum: AlbumItem,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var newName by remember { mutableStateOf(currentAlbum.name) }
  val colors = GalleryTheme.colors

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Rename Album", fontWeight = FontWeight.Bold, color = colors.primaryText)
      }
    },
    text = {
      Column {
        Text("Enter a new name for this album:", fontSize = 13.sp, color = colors.secondaryText)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_rename_album"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.primaryText,
            unfocusedTextColor = colors.primaryText
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(newName) },
        enabled = newName.isNotBlank() && newName != currentAlbum.name,
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = colors.secondaryText)
      }
    },
    containerColor = colors.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun DeleteAlbumDialog(
  album: AlbumItem,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  val colors = GalleryTheme.colors

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.Warning,
          contentDescription = null,
          tint = colors.warning,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete Album \"${album.name}\"?", fontWeight = FontWeight.Bold, color = colors.primaryText)
      }
    },
    text = {
      Column {
        Text(
          text = "Are you sure you want to delete this album?",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.primaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.accentGlow)
            .padding(10.dp)
        ) {
          Text(
            text = "Deleting this album will only remove the album group from My Gallery AI. Your original photos and videos on your phone storage will NOT be deleted.",
            fontSize = 12.sp,
            color = colors.accent,
            fontWeight = FontWeight.Medium
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("btn_confirm_delete_album")
      ) {
        Text("Delete Album")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Keep Album", color = colors.secondaryText)
      }
    },
    containerColor = colors.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCoverChooserSheet(
  album: AlbumItem,
  mediaItems: List<MediaItem>,
  onCoverChosen: (String?) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val sheetState = rememberModalBottomSheetState()
  val context = LocalContext.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Choose Album Cover",
            color = colors.primaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Tap a photo to set as cover. Original photos are never modified.",
            color = colors.secondaryText,
            fontSize = 11.sp
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.secondaryText)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 360.dp)
      ) {
        items(mediaItems, key = { it.id }) { photo ->
          val isCurrentCover = photo.uri == album.coverUri
          Box(
            modifier = Modifier
              .aspectRatio(1f)
              .clip(RoundedCornerShape(8.dp))
              .border(
                width = if (isCurrentCover) 3.dp else 1.dp,
                color = if (isCurrentCover) colors.accent else colors.border,
                shape = RoundedCornerShape(8.dp)
              )
              .clickable {
                onCoverChosen(photo.uri)
              }
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(photo.uri)
                .size(200, 200)
                .crossfade(true)
                .build(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxWidth()
            )
            if (isCurrentCover) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp)
                  .size(20.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(colors.accent),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Current Cover",
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun SelectionSummaryDialog(
  summary: SelectionSummary,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.Info,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Selection Summary", fontWeight = FontWeight.Bold, color = colors.primaryText)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow("Total Items", "${summary.count}")
        DetailRow("Photos", "${summary.photoCount}")
        DetailRow("Videos", "${summary.videoCount}")
        DetailRow("Total Size", summary.formattedSize)
        DetailRow("Date Range", summary.dateSpan)
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Done")
      }
    },
    containerColor = colors.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun DetailRow(label: String, value: String) {
  val colors = GalleryTheme.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = colors.secondaryText, fontSize = 13.sp)
    Text(text = value, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}
