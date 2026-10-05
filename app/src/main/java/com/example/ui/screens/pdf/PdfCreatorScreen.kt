package com.example.ui.screens.pdf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.pdf.PdfStudioEngine
import com.example.model.pdf.PdfBackground
import com.example.model.pdf.PdfConfig
import com.example.model.pdf.PdfCreatorPage
import com.example.model.pdf.PdfDimensionUnit
import com.example.model.pdf.PdfImageFit
import com.example.model.pdf.PdfMargin
import com.example.model.pdf.PdfOrientation
import com.example.model.pdf.PdfQualityPreset
import com.example.model.pdf.PdfStandardSize
import com.example.model.pdf.PdfTargetSizeOption
import com.example.ui.theme.LocalGalleryColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PdfCreatorScreen(
  pages: List<PdfCreatorPage>,
  config: PdfConfig,
  onConfigChange: (PdfConfig) -> Unit,
  onAddPagesFromGallery: () -> Unit,
  onAddPageFromScanner: () -> Unit,
  onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
  onRotatePage: (index: Int) -> Unit,
  onDuplicatePage: (index: Int) -> Unit,
  onDeletePage: (index: Int) -> Unit,
  onReplacePage: (index: Int) -> Unit,
  onPageFitChange: (index: Int, fit: PdfImageFit) -> Unit,
  onPreviewClick: () -> Unit,
  onCreatePdfClick: () -> Unit,
  onClose: () -> Unit
) {
  val colors = LocalGalleryColors.current
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabTitles = listOf("Pages (${pages.size})", "Layout & Size", "Quality & Limit", "Metadata")

  val estimatedSize = remember(pages.size, config) {
    PdfStudioEngine.estimateFileSize(pages.size, config)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Create PDF",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary
            )
            Text(
              text = "${pages.size} pages • $estimatedSize",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("pdf_creator_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Close",
              tint = colors.textPrimary
            )
          }
        },
        actions = {
          IconButton(
            onClick = onPreviewClick,
            modifier = Modifier.testTag("pdf_creator_preview_btn")
          ) {
            Icon(
              imageVector = Icons.Rounded.Visibility,
              contentDescription = "Interactive Preview",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = colors.surfaceBackground
        )
      )
    },
    bottomBar = {
      Surface(
        color = colors.surfaceCard,
        shadowElevation = 8.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onPreviewClick,
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Visibility,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Preview")
          }

          Button(
            onClick = onCreatePdfClick,
            enabled = pages.isNotEmpty(),
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("pdf_creator_submit_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            )
          ) {
            Icon(
              imageVector = Icons.Rounded.PictureAsPdf,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Create PDF",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(colors.surfaceBackground)
    ) {
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = colors.surfaceBackground,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        tabTitles.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = title,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          )
        }
      }

      when (selectedTab) {
        0 -> {
          // Tab 1: Pages Management
          PagesManagementTab(
            pages = pages,
            onAddFromGallery = onAddPagesFromGallery,
            onAddFromScanner = onAddPageFromScanner,
            onMovePage = onMovePage,
            onRotatePage = onRotatePage,
            onDuplicatePage = onDuplicatePage,
            onDeletePage = onDeletePage,
            onReplacePage = onReplacePage,
            onPageFitChange = onPageFitChange
          )
        }
        1 -> {
          // Tab 2: Layout, Page Size, Margins & Fit
          LayoutTab(
            config = config,
            onConfigChange = onConfigChange
          )
        }
        2 -> {
          // Tab 3: Quality & Target Size Limit
          QualityAndLimitTab(
            config = config,
            pagesCount = pages.size,
            onConfigChange = onConfigChange
          )
        }
        3 -> {
          // Tab 4: Metadata
          MetadataTab(
            config = config,
            onConfigChange = onConfigChange
          )
        }
      }
    }
  }
}

@Composable
private fun PagesManagementTab(
  pages: List<PdfCreatorPage>,
  onAddFromGallery: () -> Unit,
  onAddFromScanner: () -> Unit,
  onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
  onRotatePage: (index: Int) -> Unit,
  onDuplicatePage: (index: Int) -> Unit,
  onDeletePage: (index: Int) -> Unit,
  onReplacePage: (index: Int) -> Unit,
  onPageFitChange: (index: Int, fit: PdfImageFit) -> Unit
) {
  val colors = LocalGalleryColors.current
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Add page action bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        FilledTonalButton(
          onClick = onAddFromGallery,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("add_pages_gallery_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("+ Add Photos")
        }

        FilledTonalButton(
          onClick = onAddFromScanner,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("add_pages_scan_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("+ Scan Page")
        }
      }
    }

    if (pages.isEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Rounded.PictureAsPdf,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = colors.textSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No pages added yet",
              style = MaterialTheme.typography.titleMedium,
              color = colors.textPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Tap '+ Add Photos' or '+ Scan Page' to add pages to your PDF.",
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textSecondary,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }
    }

    // List of Pages
    itemsIndexed(pages) { index, page ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Thumbnail with rotation
          Box(
            modifier = Modifier
              .size(width = 64.dp, height = 84.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(colors.cardBackground)
              .border(1.dp, colors.divider, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(page.sourceUri)
                .crossfade(true)
                .build(),
              contentDescription = "Page ${index + 1}",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                  rotationZ = page.rotationDegrees.toFloat()
                }
            )

            // Page Number Badge
            Box(
              modifier = Modifier
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "${index + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          // Page Details & Controls
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (page.title.isNotBlank()) page.title else "Page ${index + 1}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Text(
              text = "${page.rotationDegrees}° rotation • ${page.fitOverride?.displayName ?: "Default Fit"}",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Move Up
              IconButton(
                onClick = { if (index > 0) onMovePage(index, index - 1) },
                enabled = index > 0,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.ArrowUpward,
                  contentDescription = "Move Up",
                  tint = if (index > 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f),
                  modifier = Modifier.size(16.dp)
                )
              }

              // Move Down
              IconButton(
                onClick = { if (index < pages.size - 1) onMovePage(index, index + 1) },
                enabled = index < pages.size - 1,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.ArrowDownward,
                  contentDescription = "Move Down",
                  tint = if (index < pages.size - 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f),
                  modifier = Modifier.size(16.dp)
                )
              }

              // Rotate 90
              IconButton(
                onClick = { onRotatePage(index) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.RotateRight,
                  contentDescription = "Rotate 90",
                  tint = colors.textPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }

              // Duplicate
              IconButton(
                onClick = { onDuplicatePage(index) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.ContentCopy,
                  contentDescription = "Duplicate",
                  tint = colors.textPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }

              // Replace
              IconButton(
                onClick = { onReplacePage(index) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Replace Image",
                  tint = colors.textPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }

              Spacer(modifier = Modifier.weight(1f))

              // Delete
              IconButton(
                onClick = { onDeletePage(index) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Delete,
                  contentDescription = "Delete Page",
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayoutTab(
  config: PdfConfig,
  onConfigChange: (PdfConfig) -> Unit
) {
  val colors = LocalGalleryColors.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Page Size
    item {
      SectionCard(title = "Standard Page Size", subtitle = "Choose international standard or custom dimension") {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PdfStandardSize.entries.forEach { size ->
            val isSelected = config.standardSize == size
            FilterChip(
              selected = isSelected,
              onClick = { onConfigChange(config.copy(standardSize = size)) },
              label = {
                Text(
                  text = "${size.displayName} (${size.dimensionDescription})",
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )
          }
        }

        // Custom Size Input Fields if CUSTOM selected
        AnimatedVisibility(visible = config.standardSize == PdfStandardSize.CUSTOM) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
              text = "Custom Dimensions",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = if (config.customWidth > 0) config.customWidth.toString() else "",
                onValueChange = { str ->
                  str.toFloatOrNull()?.let { onConfigChange(config.copy(customWidth = it)) }
                },
                label = { Text("Width") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              )

              OutlinedTextField(
                value = if (config.customHeight > 0) config.customHeight.toString() else "",
                onValueChange = { str ->
                  str.toFloatOrNull()?.let { onConfigChange(config.copy(customHeight = it)) }
                },
                label = { Text("Height") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              )

              // Unit selector
              Column(modifier = Modifier.width(90.dp)) {
                Text(text = "Unit", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  listOf(PdfDimensionUnit.MM, PdfDimensionUnit.INCH).forEach { unit ->
                    val isUnit = config.customUnit == unit
                    FilterChip(
                      selected = isUnit,
                      onClick = { onConfigChange(config.copy(customUnit = unit)) },
                      label = { Text(unit.symbol, fontSize = 11.sp) }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 2. Orientation
    item {
      SectionCard(title = "Orientation", subtitle = "Page aspect alignment") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          PdfOrientation.entries.forEach { orient ->
            val isSelected = config.orientation == orient
            FilledTonalButton(
              onClick = { onConfigChange(config.copy(orientation = orient)) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else colors.cardBackground,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else colors.textPrimary
              )
            ) {
              Text(
                text = orient.displayName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }
    }

    // 3. Image Fit Mode
    item {
      SectionCard(title = "Image Fit & Placement", subtitle = "How photos fit onto paper bounds") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PdfImageFit.entries.forEach { fit ->
            val isSelected = config.imageFit == fit
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                .clickable { onConfigChange(config.copy(imageFit = fit)) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else colors.textSecondary,
                    shape = CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .background(MaterialTheme.colorScheme.primary, CircleShape)
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Text(
                  text = fit.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                  color = colors.textPrimary
                )
                Text(
                  text = fit.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
              }
            }
          }
        }
      }
    }

    // 4. Margins
    item {
      SectionCard(title = "Page Margins", subtitle = "Outer whitespace around photos/scans") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PdfMargin.entries.forEach { margin ->
            val isSelected = config.margin == margin
            FilterChip(
              selected = isSelected,
              onClick = { onConfigChange(config.copy(margin = margin)) },
              label = {
                Text(
                  text = margin.displayName,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              }
            )
          }
        }
      }
    }

    // 5. Page Background
    item {
      SectionCard(title = "Page Background Color", subtitle = "Paper backdrop color") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          PdfBackground.entries.take(4).forEach { bg ->
            val isSelected = config.background == bg
            val previewColor = when (bg) {
              PdfBackground.WHITE -> Color.White
              PdfBackground.IVORY -> Color(0xFFFAF8F5)
              PdfBackground.SOFT_GRAY -> Color(0xFFF3F4F6)
              PdfBackground.DARK -> Color(0xFF1E293B)
              else -> Color.White
            }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .weight(1f)
                .clickable { onConfigChange(config.copy(background = bg)) }
                .padding(vertical = 4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(previewColor)
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else colors.divider,
                    shape = CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (bg == PdfBackground.DARK) Color.White else Color.Black,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = bg.displayName.split(" ").last(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QualityAndLimitTab(
  config: PdfConfig,
  pagesCount: Int,
  onConfigChange: (PdfConfig) -> Unit
) {
  val colors = LocalGalleryColors.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Output Size Estimation Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Tune,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "Estimated File Size",
              style = MaterialTheme.typography.labelMedium,
              color = colors.textSecondary
            )
            Text(
              text = PdfStudioEngine.estimateFileSize(pagesCount, config),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // 2. Target File Size Optimization
    item {
      SectionCard(
        title = "Target File Size (Portal Optimization)",
        subtitle = "Automatically tunes compression to fit portal upload requirements"
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PdfTargetSizeOption.entries.forEach { option ->
            val isSelected = config.targetSizeOption == option
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                .clickable { onConfigChange(config.copy(targetSizeOption = option)) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else colors.textSecondary,
                    shape = CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .background(MaterialTheme.colorScheme.primary, CircleShape)
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Text(
                text = option.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = colors.textPrimary
              )
            }
          }

          // Strict Maximum Size Limit Switch
          AnimatedVisibility(visible = config.targetSizeOption != PdfTargetSizeOption.NO_LIMIT) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Strict Maximum Cap",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = colors.textPrimary
                )
                Text(
                  text = "Guarantee generated PDF strictly stays under target bytes limit.",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
              }
              Switch(
                checked = config.isMaxLimitStrict,
                onCheckedChange = { onConfigChange(config.copy(isMaxLimitStrict = it)) }
              )
            }
          }
        }
      }
    }

    // 3. Quality Presets
    item {
      SectionCard(
        title = "Quality Presets",
        subtitle = "Controls image downsampling & JPEG compression"
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PdfQualityPreset.entries.forEach { preset ->
            val isSelected = config.qualityPreset == preset
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onConfigChange(config.copy(qualityPreset = preset)) },
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else colors.cardBackground
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else colors.textPrimary
                  )
                  Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary
                  )
                }

                if (isSelected) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetadataTab(
  config: PdfConfig,
  onConfigChange: (PdfConfig) -> Unit
) {
  val colors = LocalGalleryColors.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "PDF Document Metadata",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
      )
      Text(
        text = "Optional metadata embedded in the PDF properties.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary
      )
    }

    item {
      OutlinedTextField(
        value = config.fileName,
        onValueChange = { onConfigChange(config.copy(fileName = it)) },
        label = { Text("PDF File Name") },
        placeholder = { Text("e.g. Tax_Receipts_2026") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      )
    }

    item {
      OutlinedTextField(
        value = config.title,
        onValueChange = { onConfigChange(config.copy(title = it)) },
        label = { Text("Document Title") },
        placeholder = { Text("e.g. Scanned Identity Documents") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      )
    }

    item {
      OutlinedTextField(
        value = config.author,
        onValueChange = { onConfigChange(config.copy(author = it)) },
        label = { Text("Author") },
        placeholder = { Text("e.g. Amit Meena (or leave blank)") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      )
    }

    item {
      OutlinedTextField(
        value = config.subject,
        onValueChange = { onConfigChange(config.copy(subject = it)) },
        label = { Text("Subject") },
        placeholder = { Text("e.g. Official Identification & Certifications") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      )
    }

    item {
      OutlinedTextField(
        value = config.keywords,
        onValueChange = { onConfigChange(config.copy(keywords = it)) },
        label = { Text("Keywords") },
        placeholder = { Text("e.g. scanner, bill, receipt, official") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      )
    }
  }
}

@Composable
private fun SectionCard(
  title: String,
  subtitle: String,
  content: @Composable () -> Unit
) {
  val colors = LocalGalleryColors.current

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = 12.dp)
      )
      content()
    }
  }
}
