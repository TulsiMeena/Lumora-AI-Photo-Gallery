package com.example.ui.screens.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.pdf.PdfHistoryItem
import com.example.ui.theme.LocalGalleryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfStudioHomeScreen(
  history: List<PdfHistoryItem>,
  onImagesToPdfClick: () -> Unit,
  onScanToPdfClick: () -> Unit,
  onDocumentsToPdfClick: () -> Unit,
  onCompressPdfClick: () -> Unit,
  onOpenPdf: (PdfHistoryItem) -> Unit,
  onSharePdf: (PdfHistoryItem) -> Unit,
  onRenamePdf: (PdfHistoryItem, newName: String) -> Unit,
  onDeletePdf: (PdfHistoryItem, deleteDeviceFile: Boolean) -> Unit,
  onClose: () -> Unit
) {
  val colors = LocalGalleryColors.current
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }

  var pdfToRename by remember { mutableStateOf<PdfHistoryItem?>(null) }
  var renameInput by remember { mutableStateOf("") }
  var pdfToDelete by remember { mutableStateOf<PdfHistoryItem?>(null) }

  val filteredHistory = remember(history, searchQuery) {
    if (searchQuery.isBlank()) history
    else history.filter {
      it.fileName.contains(searchQuery, ignoreCase = true) ||
          it.title.contains(searchQuery, ignoreCase = true) ||
          it.keywords.contains(searchQuery, ignoreCase = true)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "PDF Studio",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary
            )
            Text(
              text = "Offline • Fast • Professional Export",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("pdf_studio_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = colors.textPrimary
            )
          }
        },
        actions = {
          IconButton(
            onClick = onImagesToPdfClick,
            modifier = Modifier.testTag("pdf_studio_new_btn")
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = "New PDF",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = colors.surfaceBackground
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(colors.surfaceBackground),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Studio Hub Hero Banner
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.PictureAsPdf,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
              )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Advanced PDF Creator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
              )
              Text(
                text = "Convert photos, scan paper documents, or compress PDFs for government portals.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
          }
        }
      }

      // 2. Action Cards Grid
      item {
        Text(
          text = "Create & Convert",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StudioActionCard(
              title = "Images → PDF",
              subtitle = "Photos & albums",
              icon = Icons.Rounded.AddPhotoAlternate,
              iconTint = Color(0xFF3B82F6),
              modifier = Modifier.weight(1f),
              testTag = "action_images_to_pdf",
              onClick = onImagesToPdfClick
            )

            StudioActionCard(
              title = "Scan → PDF",
              subtitle = "Multi-page camera scan",
              icon = Icons.Rounded.CameraAlt,
              iconTint = Color(0xFF10B981),
              modifier = Modifier.weight(1f),
              testTag = "action_scan_to_pdf",
              onClick = onScanToPdfClick
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StudioActionCard(
              title = "Documents → PDF",
              subtitle = "IDs, notes, receipts",
              icon = Icons.Rounded.Description,
              iconTint = Color(0xFF8B5CF6),
              modifier = Modifier.weight(1f),
              testTag = "action_docs_to_pdf",
              onClick = onDocumentsToPdfClick
            )

            StudioActionCard(
              title = "Target Compress",
              subtitle = "500KB / 1MB portals",
              icon = Icons.Rounded.Tune,
              iconTint = Color(0xFFF59E0B),
              modifier = Modifier.weight(1f),
              testTag = "action_compress_pdf",
              onClick = onCompressPdfClick
            )
          }
        }
      }

      // 3. Recent PDFs & History Section
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "PDF History (${history.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
          )
        }

        // Search Bar for PDF history if items exist
        if (history.isNotEmpty()) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search PDFs...") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.textSecondary
              )
            },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear",
                    tint = colors.textSecondary
                  )
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = colors.surfaceCard,
              unfocusedContainerColor = colors.surfaceCard
            )
          )
        }
      }

      // Empty State
      if (filteredHistory.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Rounded.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colors.textSecondary.copy(alpha = 0.5f)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (searchQuery.isNotBlank()) "No PDFs found for '$searchQuery'" else "No PDFs Created Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
              )
              Text(
                text = "Tap 'Images → PDF' or 'Scan → PDF' to create your first document.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }
      }

      // PDF History Items
      items(filteredHistory, key = { it.id }) { item ->
        PdfHistoryCard(
          item = item,
          onOpen = { onOpenPdf(item) },
          onShare = { onSharePdf(item) },
          onRename = {
            pdfToRename = item
            renameInput = item.fileName.removeSuffix(".pdf")
          },
          onDelete = { pdfToDelete = item }
        )
      }
    }
  }

  // Rename Dialog
  pdfToRename?.let { item ->
    AlertDialog(
      onDismissRequest = { pdfToRename = null },
      title = { Text("Rename PDF") },
      text = {
        OutlinedTextField(
          value = renameInput,
          onValueChange = { renameInput = it },
          label = { Text("File Name") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (renameInput.isNotBlank()) {
              val sanitized = if (renameInput.endsWith(".pdf", ignoreCase = true)) renameInput else "$renameInput.pdf"
              onRenamePdf(item, sanitized)
              pdfToRename = null
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { pdfToRename = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Delete / Remove Options Dialog
  pdfToDelete?.let { item ->
    AlertDialog(
      onDismissRequest = { pdfToDelete = null },
      title = { Text("Remove PDF") },
      text = {
        Column {
          Text("How would you like to remove '${item.fileName}'?")
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "• Remove from My Gallery AI History: Cleans up app history but keeps the file in your device storage.\n• Delete File from Device: Permanently removes the PDF file from storage.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeletePdf(item, false)
            pdfToDelete = null
          }
        ) {
          Text("History Only")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            onDeletePdf(item, true)
            pdfToDelete = null
          },
          colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
        ) {
          Text("Delete File")
        }
      }
    )
  }
}

@Composable
private fun StudioActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  modifier: Modifier = Modifier,
  testTag: String = "",
  onClick: () -> Unit
) {
  val colors = LocalGalleryColors.current

  Card(
    modifier = modifier
      .height(112.dp)
      .testTag(testTag)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(22.dp)
        )
      }

      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun PdfHistoryCard(
  item: PdfHistoryItem,
  onOpen: () -> Unit,
  onShare: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  val colors = LocalGalleryColors.current
  val context = LocalContext.current
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Thumbnail or PDF Icon Box
      Box(
        modifier = Modifier
          .size(width = 54.dp, height = 70.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFEF4444).copy(alpha = 0.12f))
          .border(1.dp, colors.divider, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (!item.thumbnailUri.isNullOrBlank()) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(item.thumbnailUri)
              .crossfade(true)
              .build(),
            contentDescription = "PDF Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Rounded.PictureAsPdf,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Content
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.fileName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${item.pageCount} ${if (item.pageCount == 1) "Page" else "Pages"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(text = "•", color = colors.textSecondary)
          Text(
            text = item.formattedSize,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
          )
          Text(text = "•", color = colors.textSecondary)
          Text(
            text = item.pageSizeName,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = item.formattedDate,
          style = MaterialTheme.typography.labelSmall,
          color = colors.textSecondary
        )
      }

      // Actions Menu
      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Options",
            tint = colors.textSecondary
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false }
        ) {
          DropdownMenuItem(
            text = { Text("Open PDF") },
            leadingIcon = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onOpen()
            }
          )
          DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShare()
            }
          )
          DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onRename()
            }
          )
          DropdownMenuItem(
            text = { Text("Remove", color = Color(0xFFEF4444)) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            onClick = {
              menuExpanded = false
              onDelete()
            }
          )
        }
      }
    }
  }
}
