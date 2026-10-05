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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.DocumentCategories
import com.example.model.DocumentFormat
import com.example.model.DocumentItem
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.GalleryTheme
import java.util.Locale

@Composable
fun DocumentsScreen(
  documents: List<DocumentItem>,
  selectedCategory: String,
  onCategorySelected: (String) -> Unit,
  onCreatePdfClick: () -> Unit,
  onScanDocClick: () -> Unit,
  onImportPhotoClick: () -> Unit,
  onResizeClick: () -> Unit,
  onCompressClick: () -> Unit,
  onDocumentClick: (DocumentItem) -> Unit,
  onOcrClick: (DocumentItem) -> Unit,
  onAskAiClick: (DocumentItem) -> Unit,
  onRenameClick: (DocumentItem) -> Unit,
  onChangeCategoryClick: (DocumentItem) -> Unit,
  onDeleteClick: (DocumentItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  var searchQuery by remember { mutableStateOf("") }

  val filteredDocs = documents.filter { doc ->
    val matchesCategory = when (selectedCategory) {
      DocumentCategories.ALL, "All" -> true
      DocumentCategories.RECENT -> true
      DocumentCategories.SCANNED -> doc.category.contains("Scan", ignoreCase = true) || doc.format == DocumentFormat.SCAN
      else -> doc.category.equals(selectedCategory, ignoreCase = true)
    }

    val matchesSearch = if (searchQuery.isBlank()) {
      true
    } else {
      val q = searchQuery.trim().lowercase(Locale.getDefault())
      doc.title.lowercase(Locale.getDefault()).contains(q) ||
        doc.category.lowercase(Locale.getDefault()).contains(q) ||
        doc.ocrText.lowercase(Locale.getDefault()).contains(q)
    }

    matchesCategory && matchesSearch
  }.let { list ->
    if (selectedCategory == DocumentCategories.RECENT) {
      list.sortedByDescending { it.dateModifiedMillis }
    } else {
      list
    }
  }

  val totalPages = documents.sumOf { it.pageCount }
  val ocrReadyCount = documents.count { it.hasOcr }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // Top Bar
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = colors.background
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Document Hub & OCR",
              color = colors.primaryText,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "On-Device Scanner, Text Extraction & Privacy",
              color = colors.secondaryText,
              fontSize = 12.sp
            )
          }

          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(colors.accentGlow),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.DocumentScanner,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("documents_screen_content")
    ) {
      // Hero Hub Stats Card with Quick Scan
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "DOCUMENTS WORKSPACE",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${documents.size} Documents • $totalPages Pages",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = colors.textPrimary
                )
              }

              Button(
                onClick = onScanDocClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.DocumentScanner,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Doc", style = MaterialTheme.typography.labelMedium)
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              MetricChip(
                label = "OCR Indexed",
                value = "$ocrReadyCount docs",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
              )
              MetricChip(
                label = "Privacy",
                value = "100% On-Device",
                color = Color(0xFF6366F1),
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // 4 Main Tool Cards
      item {
        Column {
          Text(
            text = "Document Tools",
            color = colors.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            DocToolButton(
              title = "Scan Document",
              subtitle = "Warp & Filter",
              icon = Icons.Rounded.DocumentScanner,
              accent = Color(0xFF10B981),
              onClick = onScanDocClick,
              modifier = Modifier.weight(1f)
            )
            DocToolButton(
              title = "Import Photo",
              subtitle = "Gallery to Scan",
              icon = Icons.Rounded.AddPhotoAlternate,
              accent = Color(0xFF3B82F6),
              onClick = onImportPhotoClick,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            DocToolButton(
              title = "PDF Creator",
              subtitle = "Multi-Page PDF",
              icon = Icons.Rounded.PictureAsPdf,
              accent = colors.accent,
              onClick = onCreatePdfClick,
              modifier = Modifier.weight(1f)
            )
            DocToolButton(
              title = "Compress & Size",
              subtitle = "Optimize Storage",
              icon = Icons.Rounded.Compress,
              accent = Color(0xFFF59E0B),
              onClick = onCompressClick,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Search Bar (searches titles, categories & full OCR text)
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = "Search by title, category or OCR text (e.g. Amit, Aadhaar)...",
              color = colors.secondaryText,
              fontSize = 13.sp
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "Search",
              tint = colors.secondaryText,
              modifier = Modifier.size(18.dp)
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedContainerColor = colors.surfaceVariant,
            unfocusedContainerColor = colors.surfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_doc_search")
        )
      }

      // Category filter chips (All 9 required categories)
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DocumentCategories.ALL_CATEGORIES) { category ->
            val isSelected = category == selectedCategory ||
              (selectedCategory == "All" && category == DocumentCategories.ALL)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) colors.accent else colors.surfaceVariant)
                .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(10.dp))
                .clickable { onCategorySelected(category) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("doc_category_$category")
            ) {
              Text(
                text = category,
                color = if (isSelected) Color.White else colors.primaryText,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }
      }

      // Filtered Documents List Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (searchQuery.isNotBlank()) "Search Results" else selectedCategory,
            color = colors.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${filteredDocs.size} documents",
            color = colors.secondaryText,
            fontSize = 12.sp
          )
        }
      }

      if (filteredDocs.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Rounded.PictureAsPdf,
            title = "No Documents Found",
            description = if (searchQuery.isNotBlank()) {
              "No documents match '$searchQuery' in $selectedCategory."
            } else {
              "No documents in '$selectedCategory'. Tap 'Scan Document' or 'Import Photo' to add one."
            },
            actionButtonText = if (searchQuery.isNotBlank()) "Reset Filter" else "Scan New Document",
            onActionClick = {
              if (searchQuery.isNotBlank()) {
                searchQuery = ""
                onCategorySelected(DocumentCategories.ALL)
              } else {
                onScanDocClick()
              }
            }
          )
        }
      } else {
        items(filteredDocs, key = { it.id }) { doc ->
          DocumentItemRow(
            document = doc,
            onClick = { onDocumentClick(doc) },
            onOcrClick = { onOcrClick(doc) },
            onAskAiClick = { onAskAiClick(doc) },
            onRenameClick = { onRenameClick(doc) },
            onChangeCategoryClick = { onChangeCategoryClick(doc) },
            onDeleteClick = { onDeleteClick(doc) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }
}

@Composable
private fun MetricChip(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.1f),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
      Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
  }
}

@Composable
private fun DocToolButton(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(colors.surface)
      .border(1.dp, colors.border, RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(12.dp)
      .testTag("doc_tool_${title.lowercase().replace(" ", "_")}")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(20.dp)
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

@Composable
private fun DocumentItemRow(
  document: DocumentItem,
  onClick: () -> Unit,
  onOcrClick: () -> Unit,
  onAskAiClick: () -> Unit,
  onRenameClick: () -> Unit,
  onChangeCategoryClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("doc_row_${document.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Document Icon Box
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = if (document.format == DocumentFormat.SCAN) Icons.Rounded.DocumentScanner else Icons.Rounded.PictureAsPdf,
              contentDescription = null,
              tint = if (document.format == DocumentFormat.PDF) Color(0xFFEF4444) else colors.accent,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = document.format.name,
              color = colors.primaryText,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = document.title,
            color = colors.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${document.category} • ${document.pageCount} ${if (document.pageCount == 1) "page" else "pages"}",
              color = colors.secondaryText,
              fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "•",
              color = colors.tertiaryText,
              fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = document.formattedSize,
              color = colors.tertiaryText,
              fontSize = 11.sp
            )
          }
        }

        // Options 3-dots Menu
        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = "Options",
              tint = colors.secondaryText,
              modifier = Modifier.size(18.dp)
            )
          }

          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("View Document") },
              leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onClick()
              }
            )
            DropdownMenuItem(
              text = { Text("View OCR Text") },
              leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onOcrClick()
              }
            )
            DropdownMenuItem(
              text = { Text("Ask AI About Document") },
              leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onAskAiClick()
              }
            )
            DropdownMenuItem(
              text = { Text("Rename") },
              leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onRenameClick()
              }
            )
            DropdownMenuItem(
              text = { Text("Change Category") },
              leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onChangeCategoryClick()
              }
            )
            DropdownMenuItem(
              text = { Text("Delete Document", color = Color(0xFFEF4444)) },
              leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
              onClick = {
                menuExpanded = false
                onDeleteClick()
              }
            )
          }
        }
      }

      // OCR snippet & Action tags if OCR is available
      if (document.hasOcr) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.accent.copy(alpha = 0.08f))
            .clickable { onOcrClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.TextFields,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = document.ocrText.lines().firstOrNull { it.isNotBlank() } ?: "OCR Text available",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textPrimary,
              maxLines = 1,
              fontSize = 11.sp
            )
          }

          Text(
            text = "View OCR >",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}
