package com.example.ui.screens.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DocumentCategories
import com.example.model.DocumentItem
import com.example.model.DocumentPageItem
import com.example.ui.theme.GalleryTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
  document: DocumentItem,
  onClose: () -> Unit,
  onOpenOcr: () -> Unit,
  onAskAi: (prompt: String) -> Unit,
  onRenameDocument: (newTitle: String) -> Unit,
  onChangeCategory: (newCategory: String) -> Unit,
  onRotatePage: (pageIndex: Int) -> Unit,
  onDeletePage: (pageIndex: Int) -> Unit,
  onReorderPages: (fromIndex: Int, toIndex: Int) -> Unit,
  onAddPage: () -> Unit,
  onDeleteDocument: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = GalleryTheme.colors
  val coroutineScope = rememberCoroutineScope()

  // Safe fallback if document has no pages
  val pages = if (document.pages.isNotEmpty()) {
    document.pages
  } else {
    listOf(
      DocumentPageItem(
        pageId = "page_default",
        pageNumber = 1,
        imageUri = document.thumbnailUri ?: "sample://default_page",
        ocrText = document.ocrText
      )
    )
  }

  val pagerState = rememberPagerState(pageCount = { pages.size })
  var menuExpanded by remember { mutableStateOf(false) }
  var showInfoSheet by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showCategoryDialog by remember { mutableStateOf(false) }
  var showAskAiDialog by remember { mutableStateOf(false) }
  var showDeletePageDialog by remember { mutableStateOf(false) }

  // Pan & Zoom state for active page
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = Color(0xFF0F172A) // Rich deep canvas for document reading
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Navigation App Bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onClose) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = colors.textPrimary
            )
          }

          Column(
            modifier = Modifier
              .weight(1f)
              .clickable { showInfoSheet = true }
          ) {
            Text(
              text = document.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary,
              maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${document.category} • Page ${pagerState.currentPage + 1} of ${pages.size}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
              )
              Spacer(modifier = Modifier.width(6.dp))
              if (document.hasOcr) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = colors.accent.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "OCR",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }
          }

          // OCR action button
          IconButton(onClick = onOpenOcr) {
            Icon(
              imageVector = Icons.Rounded.TextFields,
              contentDescription = "View OCR Text",
              tint = colors.accent
            )
          }

          // Ask AI button
          IconButton(onClick = { showAskAiDialog = true }) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = "Ask AI",
              tint = colors.accent
            )
          }

          // 3-dots Menu
          Box {
            IconButton(onClick = { menuExpanded = true }) {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More Options",
                tint = colors.textSecondary
              )
            }

            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false }
            ) {
              DropdownMenuItem(
                text = { Text("Document Info") },
                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  showInfoSheet = true
                }
              )
              DropdownMenuItem(
                text = { Text("Rename Document") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  showRenameDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Change Category") },
                leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  showCategoryDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Copy OCR Text") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                  if (clipboard != null && document.ocrText.isNotBlank()) {
                    val clip = ClipData.newPlainText("Document OCR", document.ocrText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "OCR text copied to clipboard", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "No OCR text available to copy", Toast.LENGTH_SHORT).show()
                  }
                }
              )
              DropdownMenuItem(
                text = { Text("Share Document") },
                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, document.title)
                    putExtra(Intent.EXTRA_TEXT, "Document: ${document.title}\n\n${document.ocrText}")
                  }
                  context.startActivity(Intent.createChooser(intent, "Share Document"))
                }
              )
              DropdownMenuItem(
                text = { Text("Delete Document", color = Color(0xFFEF4444)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                onClick = {
                  menuExpanded = false
                  onDeleteDocument()
                }
              )
            }
          }
        }
      }

      // Center Page Viewer with Interactive Zoom & Pan
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
          val page = pages[pageIndex]

          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  scale = (scale * zoom).coerceIn(1f, 4f)
                  if (scale > 1f) {
                    offsetX += pan.x
                    offsetY += pan.y
                  } else {
                    offsetX = 0f
                    offsetY = 0f
                  }
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Surface(
              modifier = Modifier
                .padding(16.dp)
                .graphicsLayer(
                  scaleX = scale,
                  scaleY = scale,
                  translationX = offsetX,
                  translationY = offsetY
                ),
              shape = RoundedCornerShape(12.dp),
              color = Color.White,
              shadowElevation = 8.dp
            ) {
              if (page.imageUri.startsWith("sample://") || page.imageUri.isBlank()) {
                // Simulated clean document page view
                DocumentPageCanvas(
                  document = document,
                  page = page,
                  pageIndex = pageIndex,
                  totalPages = pages.size
                )
              } else {
                AsyncImage(
                  model = page.imageUri,
                  contentDescription = "Document Page ${page.pageNumber}",
                  modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(rotationZ = page.rotationDegrees.toFloat()),
                  contentScale = ContentScale.Fit
                )
              }
            }
          }
        }

        // Floating Page Controls (Rotate, Reorder, Delete)
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 96.dp),
          shape = RoundedCornerShape(24.dp),
          color = Color.Black.copy(alpha = 0.75f),
          tonalElevation = 6.dp
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Rotate Page
            IconButton(
              onClick = { onRotatePage(pagerState.currentPage) },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.RotateRight,
                contentDescription = "Rotate page 90°",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }

            // Move Left
            IconButton(
              onClick = {
                val curr = pagerState.currentPage
                if (curr > 0) {
                  onReorderPages(curr, curr - 1)
                  coroutineScope.launch { pagerState.animateScrollToPage(curr - 1) }
                }
              },
              enabled = pagerState.currentPage > 0,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Move page left",
                tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
              )
            }

            Text(
              text = "${pagerState.currentPage + 1} / ${pages.size}",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )

            // Move Right
            IconButton(
              onClick = {
                val curr = pagerState.currentPage
                if (curr < pages.size - 1) {
                  onReorderPages(curr, curr + 1)
                  coroutineScope.launch { pagerState.animateScrollToPage(curr + 1) }
                }
              },
              enabled = pagerState.currentPage < pages.size - 1,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Move page right",
                tint = if (pagerState.currentPage < pages.size - 1) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
              )
            }

            // Delete Page
            if (pages.size > 1) {
              IconButton(
                onClick = { showDeletePageDialog = true },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Delete,
                  contentDescription = "Delete Page",
                  tint = Color(0xFFF87171),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }

      // Bottom Filmstrip Carousel
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        tonalElevation = 8.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
          ) {
            itemsIndexed(pages) { index, page ->
              val isSelected = (pagerState.currentPage == index)
              Box(
                modifier = Modifier
                  .size(width = 46.dp, height = 62.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) colors.accent else colors.border,
                    shape = RoundedCornerShape(6.dp)
                  )
                  .background(Color.White)
                  .clickable {
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                  },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${index + 1}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) colors.accent else Color.DarkGray
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Add Page Button
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.accent.copy(alpha = 0.12f),
            modifier = Modifier.clickable { onAddPage() }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Page",
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Add Page",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent
              )
            }
          }
        }
      }
    }

    // Modal: Document Info Bottom Sheet
    if (showInfoSheet) {
      ModalBottomSheet(
        onDismissRequest = { showInfoSheet = false },
        sheetState = rememberModalBottomSheetState()
      ) {
        DocumentInfoSheetContent(
          document = document,
          onRenameClick = {
            showInfoSheet = false
            showRenameDialog = true
          },
          onChangeCategoryClick = {
            showInfoSheet = false
            showCategoryDialog = true
          },
          onClose = { showInfoSheet = false }
        )
      }
    }

    // Modal: Rename Document Dialog
    if (showRenameDialog) {
      var renameTitle by remember { mutableStateOf(document.title) }
      AlertDialog(
        onDismissRequest = { showRenameDialog = false },
        title = { Text("Rename Document") },
        text = {
          OutlinedTextField(
            value = renameTitle,
            onValueChange = { renameTitle = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          Button(
            onClick = {
              if (renameTitle.isNotBlank()) {
                onRenameDocument(renameTitle)
              }
              showRenameDialog = false
            }
          ) {
            Text("Save")
          }
        },
        dismissButton = {
          TextButton(onClick = { showRenameDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    // Modal: Change Category Dialog
    if (showCategoryDialog) {
      AlertDialog(
        onDismissRequest = { showCategoryDialog = false },
        title = { Text("Select Document Category") },
        text = {
          Column {
            DocumentCategories.EDITABLE_CATEGORIES.forEach { category ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onChangeCategory(category)
                    showCategoryDialog = false
                  }
                  .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = category,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (document.category == category) FontWeight.Bold else FontWeight.Normal,
                  color = if (document.category == category) colors.accent else colors.textPrimary,
                  modifier = Modifier.weight(1f)
                )
                if (document.category == category) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showCategoryDialog = false }) {
            Text("Close")
          }
        }
      )
    }

    // Modal: Ask AI Prompt Dialog
    if (showAskAiDialog) {
      AlertDialog(
        onDismissRequest = { showAskAiDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask AI About Document")
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Select an intelligent query to execute on this document's text:",
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textSecondary
            )
            listOf(
              "इस document में क्या लिखा है?",
              "इसका summary बनाओ।",
              "इसमें कौन-कौन सी dates हैं?",
              "इसमें दिए questions solve करो।",
              "महत्वपूर्ण numbers और amounts निकालो।"
            ).forEach { prompt ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    showAskAiDialog = false
                    onAskAi(prompt)
                  }
              ) {
                Text(
                  text = prompt,
                  style = MaterialTheme.typography.bodyMedium,
                  color = colors.textPrimary,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showAskAiDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    // Modal: Delete Page Confirmation Dialog
    if (showDeletePageDialog) {
      AlertDialog(
        onDismissRequest = { showDeletePageDialog = false },
        title = { Text("Delete Page?") },
        text = { Text("Are you sure you want to remove Page ${pagerState.currentPage + 1} from this document?") },
        confirmButton = {
          Button(
            onClick = {
              showDeletePageDialog = false
              onDeletePage(pagerState.currentPage)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeletePageDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
private fun DocumentPageCanvas(
  document: DocumentItem,
  page: DocumentPageItem,
  pageIndex: Int,
  totalPages: Int
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = document.title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.Black
      )
      Text(
        text = "PAGE ${pageIndex + 1} OF $totalPages",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.Gray
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Body content with simulated document lines & extracted text snippet
    Text(
      text = page.ocrText.ifBlank { document.ocrText },
      style = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp
      ),
      color = Color(0xFF1E293B)
    )
  }
}

@Composable
private fun DocumentInfoSheetContent(
  document: DocumentItem,
  onRenameClick: () -> Unit,
  onChangeCategoryClick: () -> Unit,
  onClose: () -> Unit
) {
  val colors = GalleryTheme.colors

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp)
  ) {
    Text(
      text = "Document Details",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(18.dp))

    InfoItemRow("Title", document.title, onActionClick = onRenameClick, actionText = "Rename")
    InfoItemRow("Category", document.category, onActionClick = onChangeCategoryClick, actionText = "Change")
    InfoItemRow("Date Added", document.dateModified)
    InfoItemRow("Pages", "${document.pageCount} page(s)")
    InfoItemRow("File Size", document.formattedSize)
    InfoItemRow("Resolution", document.resolution)
    InfoItemRow("OCR Index", if (document.hasOcr) "Processed & Searchable (${document.ocrText.split(Regex("""\s+""")).size} words)" else "Not Extracted")

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = onClose,
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      Text("Done")
    }
  }
}

@Composable
private fun InfoItemRow(
  label: String,
  value: String,
  onActionClick: (() -> Unit)? = null,
  actionText: String? = null
) {
  val colors = GalleryTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
      Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
    if (onActionClick != null && actionText != null) {
      TextButton(onClick = onActionClick) {
        Text(actionText, color = colors.accent, fontWeight = FontWeight.Bold)
      }
    }
  }
}
