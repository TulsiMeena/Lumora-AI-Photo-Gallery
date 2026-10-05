package com.example.ui.screens.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Filter
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.compression.CompressionConfig
import com.example.model.compression.CompressionMode
import com.example.model.creative.BatchAdjustmentConfig
import com.example.model.creative.BatchJob
import com.example.model.creative.BatchJobStatus
import com.example.model.creative.BatchOperation
import com.example.model.creative.BatchRenameConfig
import com.example.model.creative.BatchResizeParams
import com.example.model.creative.CollageConfig
import com.example.model.creative.CollageLayout
import com.example.model.creative.ContactSheetConfig
import com.example.model.creative.CreativeStudioTab
import com.example.model.creative.FrameConfig
import com.example.model.creative.FrameType
import com.example.model.creative.GifConfig
import com.example.model.creative.PosterConfig
import com.example.model.creative.PosterFormat
import com.example.model.creative.PosterTemplate
import com.example.model.creative.SignatureConfig
import com.example.model.creative.StickerItem
import com.example.model.creative.TextDesignItem
import com.example.model.creative.WatermarkConfig
import com.example.model.creative.WatermarkPosition
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode
import com.example.ui.screens.creative.components.BatchProgressDialog
import com.example.ui.screens.creative.components.SharePreviewDialog
import com.example.ui.screens.creative.components.SignaturePadDialog
import com.example.ui.screens.creative.components.SlideshowDialog
import com.example.ui.theme.GalleryTheme
import com.example.utils.creative.BatchProcessingEngine
import com.example.utils.creative.CollageEngine
import com.example.utils.creative.ContactSheetEngine
import com.example.utils.creative.GifEncoder
import com.example.utils.creative.NativeShareHelper
import com.example.utils.creative.WatermarkEngine
import com.example.utils.resize.ImageResizeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreativeStudioScreen(
  allGalleryPhotos: List<MediaItem>,
  initialTab: CreativeStudioTab = CreativeStudioTab.ALL,
  onOpenPhotoViewer: (MediaItem) -> Unit,
  onOpenPassportStudio: () -> Unit,
  onOpenPdfStudio: () -> Unit,
  onOpenImageResize: (MediaItem?) -> Unit,
  onOpenImageCompressor: (MediaItem?) -> Unit,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var selectedTab by remember { mutableStateOf(initialTab) }
  val selectedPhotos = remember { mutableStateListOf<MediaItem>() }

  // Sub-editor active modes
  var isCollageActive by remember { mutableStateOf(false) }
  var isPosterActive by remember { mutableStateOf(false) }
  var isWatermarkActive by remember { mutableStateOf(false) }
  var isFramesActive by remember { mutableStateOf(false) }
  var isContactSheetActive by remember { mutableStateOf(false) }
  var isGifActive by remember { mutableStateOf(false) }
  var isBatchActive by remember { mutableStateOf(false) }
  var isSlideshowActive by remember { mutableStateOf(false) }

  // Dialog triggers
  var showSignaturePad by remember { mutableStateOf(false) }
  var activeBatchJob by remember { mutableStateOf<BatchJob?>(null) }
  var showSharePreviewDialog by remember { mutableStateOf(false) }

  // Initialize selected photos if empty
  LaunchedEffect(allGalleryPhotos) {
    if (selectedPhotos.isEmpty() && allGalleryPhotos.isNotEmpty()) {
      selectedPhotos.addAll(allGalleryPhotos.take(6))
    }
  }

  BackHandler {
    when {
      isCollageActive -> isCollageActive = false
      isPosterActive -> isPosterActive = false
      isWatermarkActive -> isWatermarkActive = false
      isFramesActive -> isFramesActive = false
      isContactSheetActive -> isContactSheetActive = false
      isGifActive -> isGifActive = false
      isBatchActive -> isBatchActive = false
      else -> onClose()
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("creative_studio_screen"),
    color = colors.background
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header
      CreativeStudioTopBar(
        activeTitle = when {
          isCollageActive -> "Collage Studio"
          isPosterActive -> "Poster Studio"
          isWatermarkActive -> "Watermark & Sign"
          isFramesActive -> "Frames & Stickers"
          isContactSheetActive -> "Contact Sheet"
          isGifActive -> "GIF Maker"
          isBatchActive -> "Batch Processing Studio"
          else -> "Creative Studio"
        },
        selectedCount = selectedPhotos.size,
        onBack = {
          when {
            isCollageActive -> isCollageActive = false
            isPosterActive -> isPosterActive = false
            isWatermarkActive -> isWatermarkActive = false
            isFramesActive -> isFramesActive = false
            isContactSheetActive -> isContactSheetActive = false
            isGifActive -> isGifActive = false
            isBatchActive -> isBatchActive = false
            else -> onClose()
          }
        },
        onShareSelection = {
          if (selectedPhotos.isNotEmpty()) {
            showSharePreviewDialog = true
          } else {
            Toast.makeText(context, "Select at least 1 photo to share", Toast.LENGTH_SHORT).show()
          }
        }
      )

      // Main content body
      when {
        isCollageActive -> {
          CollageStudioWorkspace(
            availablePhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onTogglePhoto = { photo ->
              if (selectedPhotos.contains(photo)) selectedPhotos.remove(photo)
              else selectedPhotos.add(photo)
            },
            onCollageSaved = { uri ->
              Toast.makeText(context, "Collage saved to Gallery!", Toast.LENGTH_LONG).show()
              isCollageActive = false
            }
          )
        }

        isPosterActive -> {
          PosterStudioWorkspace(
            availablePhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onPhotoSelected = { photo ->
              selectedPhotos.clear()
              selectedPhotos.add(photo)
            },
            onPosterSaved = { uri ->
              Toast.makeText(context, "Poster saved to Gallery!", Toast.LENGTH_LONG).show()
              isPosterActive = false
            }
          )
        }

        isWatermarkActive -> {
          WatermarkStudioWorkspace(
            availablePhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onPhotoSelected = { photo ->
              selectedPhotos.clear()
              selectedPhotos.add(photo)
            },
            onOpenSignaturePad = { showSignaturePad = true },
            onWatermarkSaved = {
              Toast.makeText(context, "Watermarked photo saved!", Toast.LENGTH_LONG).show()
              isWatermarkActive = false
            }
          )
        }

        isFramesActive -> {
          FramesStudioWorkspace(
            availablePhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onPhotoSelected = { photo ->
              selectedPhotos.clear()
              selectedPhotos.add(photo)
            },
            onFrameSaved = {
              Toast.makeText(context, "Framed photo saved to Gallery!", Toast.LENGTH_LONG).show()
              isFramesActive = false
            }
          )
        }

        isContactSheetActive -> {
          ContactSheetStudioWorkspace(
            availablePhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onTogglePhoto = { photo ->
              if (selectedPhotos.contains(photo)) selectedPhotos.remove(photo)
              else selectedPhotos.add(photo)
            },
            onExportSuccess = {
              Toast.makeText(context, "Contact sheet exported successfully!", Toast.LENGTH_LONG).show()
              isContactSheetActive = false
            }
          )
        }

        isGifActive -> {
          GifStudioWorkspace(
            availablePhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onTogglePhoto = { photo ->
              if (selectedPhotos.contains(photo)) selectedPhotos.remove(photo)
              else selectedPhotos.add(photo)
            },
            onGifSaved = {
              Toast.makeText(context, "Animated GIF created and saved to Gallery!", Toast.LENGTH_LONG).show()
              isGifActive = false
            }
          )
        }

        isBatchActive -> {
          BatchToolsWorkspace(
            allPhotos = allGalleryPhotos,
            selectedPhotos = selectedPhotos,
            onTogglePhoto = { photo ->
              if (selectedPhotos.contains(photo)) selectedPhotos.remove(photo)
              else selectedPhotos.add(photo)
            },
            onSelectAll = {
              selectedPhotos.clear()
              selectedPhotos.addAll(allGalleryPhotos)
            },
            onClearSelection = {
              selectedPhotos.clear()
            },
            onLaunchBatchJob = { job ->
              activeBatchJob = job
            }
          )
        }

        else -> {
          // Home Overview of Creative Studio
          CreativeStudioHomeContent(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            selectedCount = selectedPhotos.size,
            allPhotosCount = allGalleryPhotos.size,
            onOpenCollage = { isCollageActive = true },
            onOpenPoster = { isPosterActive = true },
            onOpenWatermark = { isWatermarkActive = true },
            onOpenFrames = { isFramesActive = true },
            onOpenContactSheet = { isContactSheetActive = true },
            onOpenGif = { isGifActive = true },
            onOpenSlideshow = {
              if (selectedPhotos.isNotEmpty()) isSlideshowActive = true
              else Toast.makeText(context, "Select at least 1 photo for slideshow", Toast.LENGTH_SHORT).show()
            },
            onOpenBatch = { isBatchActive = true },
            onOpenPassportStudio = onOpenPassportStudio,
            onOpenPdfStudio = onOpenPdfStudio,
            onOpenImageResize = { onOpenImageResize(selectedPhotos.firstOrNull()) },
            onOpenImageCompressor = { onOpenImageCompressor(selectedPhotos.firstOrNull()) }
          )
        }
      }
    }

    // Modal Overlays
    if (showSignaturePad) {
      SignaturePadDialog(
        onDismiss = { showSignaturePad = false },
        onSaveSignature = { cfg ->
          showSignaturePad = false
          Toast.makeText(context, "Signature recorded successfully!", Toast.LENGTH_SHORT).show()
        }
      )
    }

    if (isSlideshowActive && selectedPhotos.isNotEmpty()) {
      SlideshowDialog(
        items = selectedPhotos,
        onDismiss = { isSlideshowActive = false }
      )
    }

    activeBatchJob?.let { job ->
      BatchProgressDialog(
        job = job,
        onOpenResultsInGallery = {
          activeBatchJob = null
          isBatchActive = false
          onClose()
        },
        onDismiss = {
          activeBatchJob = null
        }
      )
    }

    if (showSharePreviewDialog && selectedPhotos.isNotEmpty()) {
      SharePreviewDialog(
        items = selectedPhotos,
        isFromVault = false,
        onConfirmShare = { mode ->
          showSharePreviewDialog = false
          coroutineScope.launch {
            if (mode == com.example.model.creative.ShareMode.COMPRESSED) {
              Toast.makeText(context, "Optimizing share copy...", Toast.LENGTH_SHORT).show()
            } else if (mode == com.example.model.creative.ShareMode.RESIZED) {
              Toast.makeText(context, "Resizing share copy...", Toast.LENGTH_SHORT).show()
            }
            NativeShareHelper.shareMediaItems(context, selectedPhotos, mode)
          }
        },
        onDismiss = { showSharePreviewDialog = false }
      )
    }
  }
}

@Composable
private fun CreativeStudioTopBar(
  activeTitle: String,
  selectedCount: Int,
  onBack: () -> Unit,
  onShareSelection: () -> Unit
) {
  val colors = GalleryTheme.colors
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, colors.border.copy(alpha = 0.5f)),
    color = colors.surface,
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("creative_btn_back")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = activeTitle,
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "$selectedCount photos in selection",
            color = colors.secondaryText,
            fontSize = 12.sp
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
          onClick = onShareSelection,
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
          border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.testTag("creative_btn_share_selection")
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share Center", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun CreativeStudioHomeContent(
  selectedTab: CreativeStudioTab,
  onTabSelected: (CreativeStudioTab) -> Unit,
  selectedCount: Int,
  allPhotosCount: Int,
  onOpenCollage: () -> Unit,
  onOpenPoster: () -> Unit,
  onOpenWatermark: () -> Unit,
  onOpenFrames: () -> Unit,
  onOpenContactSheet: () -> Unit,
  onOpenGif: () -> Unit,
  onOpenSlideshow: () -> Unit,
  onOpenBatch: () -> Unit,
  onOpenPassportStudio: () -> Unit,
  onOpenPdfStudio: () -> Unit,
  onOpenImageResize: () -> Unit,
  onOpenImageCompressor: () -> Unit
) {
  val colors = GalleryTheme.colors

  Column(modifier = Modifier.fillMaxSize()) {
    // Studio Category Tabs
    ScrollableTabRow(
      selectedTabIndex = selectedTab.ordinal,
      containerColor = colors.surface,
      contentColor = colors.accent,
      edgePadding = 16.dp,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
          color = colors.accent,
          height = 3.dp
        )
      },
      divider = { HorizontalDivider(color = colors.border.copy(alpha = 0.4f)) }
    ) {
      CreativeStudioTab.values().forEach { tab ->
        Tab(
          selected = selectedTab == tab,
          onClick = { onTabSelected(tab) },
          text = {
            Text(
              text = tab.title,
              fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
              fontSize = 13.sp,
              color = if (selectedTab == tab) colors.accent else colors.secondaryText
            )
          }
        )
      }
    }

    // Scrollable Tool Cards Grid
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Banner Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surface),
          border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "Offline-First Creative Engine",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.primaryText
              )
              Text(
                "All collages, posters, GIF animations, watermark stamps and batch operations run 100% on-device.",
                fontSize = 12.sp,
                color = colors.secondaryText
              )
            }
          }
        }
      }

      // Filtered Tool Cards
      if (selectedTab == CreativeStudioTab.ALL || selectedTab == CreativeStudioTab.COLLAGE) {
        item {
          StudioSectionTitle("Collages & Grids")
        }
        item {
          StudioToolCard(
            title = "Collage Maker",
            subtitle = "Combine 2 to 9 photos with customizable grids, spacing, corner radius, and canvas aspect ratios.",
            icon = Icons.Rounded.GridView,
            badge = "2–9 Photos",
            accentColor = Color(0xFF38BDF8),
            onClick = onOpenCollage
          )
        }
      }

      if (selectedTab == CreativeStudioTab.ALL || selectedTab == CreativeStudioTab.POSTER) {
        item {
          StudioSectionTitle("Posters & Publishing")
        }
        item {
          StudioToolCard(
            title = "Poster Studio",
            subtitle = "Design editorial storyboards, square album covers, and A4 print posters with title and theme typography.",
            icon = Icons.Rounded.Style,
            badge = "5 Templates",
            accentColor = Color(0xFFA855F7),
            onClick = onOpenPoster
          )
        }
        item {
          StudioToolCard(
            title = "Frames & Stickers",
            subtitle = "Apply Polaroid classic frames with handwritten captions, soft shadows, rounded borders, and emoji accents.",
            icon = Icons.Rounded.Wallpaper,
            badge = "Polaroid & Retro",
            accentColor = Color(0xFFF43F5E),
            onClick = onOpenFrames
          )
        }
      }

      if (selectedTab == CreativeStudioTab.ALL || selectedTab == CreativeStudioTab.WATERMARK) {
        item {
          StudioSectionTitle("Watermark, Branding & Signature")
        }
        item {
          StudioToolCard(
            title = "Watermark & Copyright",
            subtitle = "Stamp copyright text or logo images with 8 preset positions, opacity, rotation, and repeat tile patterns.",
            icon = Icons.Rounded.BrandingWatermark,
            badge = "Batch Ready",
            accentColor = Color(0xFFF59E0B),
            onClick = onOpenWatermark
          )
        }
      }

      if (selectedTab == CreativeStudioTab.ALL || selectedTab == CreativeStudioTab.CONTACT_SHEET) {
        item {
          StudioSectionTitle("Indexing & Document Creation")
        }
        item {
          StudioToolCard(
            title = "Contact Sheet Generator",
            subtitle = "Generate numbered thumbnail index sheets showing filenames, dates, dimensions, and file sizes. Export as Image or PDF.",
            icon = Icons.Rounded.DynamicFeed,
            badge = "Image & PDF",
            accentColor = Color(0xFF10B981),
            onClick = onOpenContactSheet
          )
        }
        item {
          StudioToolCard(
            title = "Photos to PDF Studio",
            subtitle = "Convert your gallery photos into high-grade multi-page PDF documents with reordering, fit modes, and compression.",
            icon = Icons.Rounded.PictureAsPdf,
            badge = "Prompt 6 Engine",
            accentColor = Color(0xFFEF4444),
            onClick = onOpenPdfStudio
          )
        }
      }

      if (selectedTab == CreativeStudioTab.ALL || selectedTab == CreativeStudioTab.GIF || selectedTab == CreativeStudioTab.SLIDESHOW) {
        item {
          StudioSectionTitle("Motion & Animation")
        }
        item {
          StudioToolCard(
            title = "Animated GIF Creator",
            subtitle = "Compile sequential photos into a looping animated GIF with custom frame delay, loop count, and quality controls.",
            icon = Icons.Rounded.Animation,
            badge = "Real GIF Engine",
            accentColor = Color(0xFF8B5CF6),
            onClick = onOpenGif
          )
        }
        item {
          StudioToolCard(
            title = "Photo Slideshow Player",
            subtitle = "Watch a cinematic presentation of your selected photos with auto-play, timer, and smooth crossfades.",
            icon = Icons.Rounded.Slideshow,
            badge = "Local Playback",
            accentColor = Color(0xFF06B6D4),
            onClick = onOpenSlideshow
          )
        }
      }

      if (selectedTab == CreativeStudioTab.ALL || selectedTab == CreativeStudioTab.BATCH) {
        item {
          StudioSectionTitle("Batch Processing Suite")
        }
        item {
          StudioToolCard(
            title = "Batch Processing Studio",
            subtitle = "Apply Resize, Compress, Format Convert, Sequential Rename, Watermark, and Adjustments across multiple photos at once.",
            icon = Icons.Rounded.Layers,
            badge = "10 Operations",
            accentColor = Color(0xFF3B82F6),
            onClick = onOpenBatch
          )
        }
        item {
          StudioToolCard(
            title = "Batch Dimensions & DPI",
            subtitle = "Concurrently resize photos to exact pixel targets, physical DPI standards, and passport photo dimensions.",
            icon = Icons.Rounded.Crop,
            badge = "Prompt 10 Engine",
            accentColor = Color(0xFF14B8A6),
            onClick = onOpenImageResize
          )
        }
        item {
          StudioToolCard(
            title = "Batch Target Size & Compress",
            subtitle = "Reduce photos to strict KB/MB targets (e.g. 500 KB limit for exam portals) with smart quality preservation.",
            icon = Icons.Rounded.Compress,
            badge = "Prompt 11 Engine",
            accentColor = Color(0xFFE11D48),
            onClick = onOpenImageCompressor
          )
        }
        item {
          StudioToolCard(
            title = "Passport Photo Sheet",
            subtitle = "Generate biometric passport photo grid sheets with 35x45mm guidelines and cut lines ready for printing.",
            icon = Icons.Rounded.AccountBox,
            badge = "Print Ready",
            accentColor = Color(0xFF6366F1),
            onClick = onOpenPassportStudio
          )
        }
      }
    }
  }
}

@Composable
private fun StudioSectionTitle(title: String) {
  val colors = GalleryTheme.colors
  Text(
    text = title,
    color = colors.primaryText,
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
  )
}

@Composable
private fun StudioToolCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  badge: String,
  accentColor: Color,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surface),
    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("tool_card_${title.replace(" ", "_").lowercase()}")
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(accentColor.copy(alpha = 0.15f))
          .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = title,
            color = colors.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.15f),
            modifier = Modifier.padding(start = 6.dp)
          ) {
            Text(
              text = badge,
              color = accentColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitle,
          color = colors.secondaryText,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 1. COLLAGE MAKER WORKSPACE
// -------------------------------------------------------------
@Composable
private fun CollageStudioWorkspace(
  availablePhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onTogglePhoto: (MediaItem) -> Unit,
  onCollageSaved: (Uri) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var config by remember { mutableStateOf(CollageConfig()) }
  var isExporting by remember { mutableStateOf(false) }

  val maxSlots = config.layout.photoCount

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      "Choose Layout (${config.layout.photoCount} slots needed)",
      fontWeight = FontWeight.Bold,
      color = colors.primaryText,
      fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(CollageLayout.values()) { layout ->
        val isSelected = config.layout == layout
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface,
          border = BorderStroke(
            1.dp,
            if (isSelected) colors.accent else colors.border
          ),
          modifier = Modifier.clickable { config = config.copy(layout = layout) }
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              layout.label,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp,
              color = if (isSelected) colors.accent else colors.primaryText
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Interactive Preview Box
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(config.aspectRatio)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(config.backgroundColor))
          .padding(config.paddingDp.dp)
      ) {
        val photosToRender = selectedPhotos.take(maxSlots)
        when (config.layout) {
          CollageLayout.SPLIT_2_V -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
              CollageCell(photosToRender.getOrNull(0), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              CollageCell(photosToRender.getOrNull(1), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
            }
          }
          CollageLayout.SPLIT_2_H -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
              CollageCell(photosToRender.getOrNull(0), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              CollageCell(photosToRender.getOrNull(1), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
            }
          }
          CollageLayout.GRID_3 -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
              CollageCell(photosToRender.getOrNull(0), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              Column(modifier = Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
                CollageCell(photosToRender.getOrNull(1), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(2), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              }
            }
          }
          CollageLayout.GRID_4 -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
              Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
                CollageCell(photosToRender.getOrNull(0), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(1), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              }
              Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
                CollageCell(photosToRender.getOrNull(2), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(3), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              }
            }
          }
          CollageLayout.GRID_6 -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
              Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
                CollageCell(photosToRender.getOrNull(0), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(1), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(2), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              }
              Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
                CollageCell(photosToRender.getOrNull(3), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(4), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                CollageCell(photosToRender.getOrNull(5), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
              }
            }
          }
          CollageLayout.GRID_9 -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
              for (row in 0 until 3) {
                Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(config.paddingDp.dp)) {
                  for (col in 0 until 3) {
                    val idx = row * 3 + col
                    CollageCell(photosToRender.getOrNull(idx), Modifier.weight(1f).fillMaxSize(), config.cornerRadiusDp)
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Controls: Spacing & Radius
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Spacing: ${config.paddingDp.toInt()} dp", fontSize = 12.sp, color = colors.secondaryText)
      Text("Corner Radius: ${config.cornerRadiusDp.toInt()} dp", fontSize = 12.sp, color = colors.secondaryText)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Slider(
        value = config.paddingDp,
        onValueChange = { config = config.copy(paddingDp = it) },
        valueRange = 0f..24f,
        modifier = Modifier.weight(1f),
        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
      )
      Slider(
        value = config.cornerRadiusDp,
        onValueChange = { config = config.copy(cornerRadiusDp = it) },
        valueRange = 0f..32f,
        modifier = Modifier.weight(1f),
        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
      )
    }

    // Canvas Background Color Options
    Text("Canvas Background", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(
        AndroidColor.WHITE to "White",
        AndroidColor.BLACK to "Dark",
        AndroidColor.parseColor("#F1F5F9") to "Light Gray",
        AndroidColor.parseColor("#FEF3C7") to "Warm Amber",
        AndroidColor.parseColor("#E0F2FE") to "Cool Sky"
      ).forEach { (col, _) ->
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(col))
            .border(
              2.dp,
              if (config.backgroundColor == col) colors.accent else colors.border,
              CircleShape
            )
            .clickable { config = config.copy(backgroundColor = col) }
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Photo Selection Tray
    Text(
      "Selected Photos (${selectedPhotos.size} of $maxSlots slots)",
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      color = colors.primaryText
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(availablePhotos.take(30)) { photo ->
        val isSelected = selectedPhotos.contains(photo)
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) colors.accent else colors.border,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onTogglePhoto(photo) }
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(photo.uri).size(150, 150).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          if (isSelected) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.accent),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Export Button
    Button(
      onClick = {
        if (selectedPhotos.isEmpty()) {
          Toast.makeText(context, "Select at least 1 photo for collage", Toast.LENGTH_SHORT).show()
          return@Button
        }
        isExporting = true
        coroutineScope.launch {
          val uris = selectedPhotos.take(maxSlots).mapNotNull { it.uri?.let { u -> Uri.parse(u) } }
          val bitmap = CollageEngine.createCollageBitmap(context, uris, config, targetDimension = 2048)
          if (bitmap != null) {
            val picsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val outDir = File(picsDir, "My Gallery AI/Collages").apply { mkdirs() }
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outDir, "Collage_$time.jpg")
            withContext(Dispatchers.IO) {
              FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
            }
            isExporting = false
            onCollageSaved(Uri.fromFile(file))
          } else {
            isExporting = false
            Toast.makeText(context, "Failed to render collage bitmap", Toast.LENGTH_SHORT).show()
          }
        }
      },
      enabled = !isExporting,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_export_collage"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      if (isExporting) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Rendering High-Res Collage...", color = Color.White)
      } else {
        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export High-Res Collage (2048px)", fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}

@Composable
private fun CollageCell(photo: MediaItem?, modifier: Modifier, cornerRadiusDp: Float) {
  val context = LocalContext.current
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(cornerRadiusDp.dp))
      .background(Color(0xFFE2E8F0)),
    contentAlignment = Alignment.Center
  ) {
    if (photo?.uri != null) {
      AsyncImage(
        model = ImageRequest.Builder(context).data(photo.uri).size(400, 400).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(28.dp))
    }
  }
}

// -------------------------------------------------------------
// 2. POSTER STUDIO WORKSPACE
// -------------------------------------------------------------
@Composable
private fun PosterStudioWorkspace(
  availablePhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onPhotoSelected: (MediaItem) -> Unit,
  onPosterSaved: (Uri) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var posterConfig by remember { mutableStateOf(PosterConfig()) }
  var isExporting by remember { mutableStateOf(false) }

  val targetPhoto = selectedPhotos.firstOrNull() ?: availablePhotos.firstOrNull()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Template chooser
    Text("Select Poster Template", fontWeight = FontWeight.Bold, color = colors.primaryText, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(PosterTemplate.values()) { tpl ->
        val isSelected = posterConfig.template == tpl
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface,
          border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
          modifier = Modifier.clickable { posterConfig = posterConfig.copy(template = tpl) }
        ) {
          Text(
            tpl.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.accent else colors.primaryText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Interactive Poster Live Preview
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(posterConfig.backgroundColor)),
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(posterConfig.format.aspectRatio)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (targetPhoto?.uri != null) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(targetPhoto.uri).size(800, 1000).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .padding(
                when (posterConfig.template) {
                  PosterTemplate.MINIMAL -> 24.dp
                  PosterTemplate.STORY -> 12.dp
                  else -> 0.dp
                }
              )
              .clip(RoundedCornerShape(12.dp))
          )
        }

        // Gradient & Editorial Overlay
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                  Color.Black.copy(alpha = 0.4f),
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.85f)
                )
              )
            )
        )

        // Title and Author typography
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(20.dp)
        ) {
          Text(
            text = posterConfig.title.uppercase(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = posterConfig.subtitle,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "BY ${posterConfig.author.uppercase()}",
            color = Color(posterConfig.primaryColor),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Form text fields
    OutlinedTextField(
      value = posterConfig.title,
      onValueChange = { posterConfig = posterConfig.copy(title = it) },
      label = { Text("Poster Main Title") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = posterConfig.subtitle,
      onValueChange = { posterConfig = posterConfig.copy(subtitle = it) },
      label = { Text("Subtitle / Description") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = posterConfig.author,
      onValueChange = { posterConfig = posterConfig.copy(author = it) },
      label = { Text("Author / Signature Tag") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Photo Selector
    Text("Select Background Image", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(availablePhotos.take(20)) { photo ->
        val isSelected = targetPhoto?.id == photo.id
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, if (isSelected) colors.accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onPhotoSelected(photo) }
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(photo.uri).size(120, 120).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = {
        val uriStr = targetPhoto?.uri ?: return@Button
        isExporting = true
        coroutineScope.launch {
          val srcBmp = ImageResizeEngine.loadSafeBitmap(context, Uri.parse(uriStr), 2048)
          if (srcBmp != null) {
            val outBmp = Bitmap.createBitmap(srcBmp.width, srcBmp.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBmp)
            canvas.drawBitmap(srcBmp, 0f, 0f, null)

            // Draw typography banner onto bitmap
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
              color = AndroidColor.WHITE
              textSize = srcBmp.width * 0.055f
              isFakeBoldText = true
              setShadowLayer(8f, 0f, 4f, AndroidColor.BLACK)
            }
            canvas.drawText(posterConfig.title.uppercase(), srcBmp.width * 0.06f, srcBmp.height * 0.88f, paint)

            paint.textSize = srcBmp.width * 0.03f
            paint.isFakeBoldText = false
            canvas.drawText(posterConfig.subtitle, srcBmp.width * 0.06f, srcBmp.height * 0.93f, paint)

            val picsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val outDir = File(picsDir, "My Gallery AI/Posters").apply { mkdirs() }
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outDir, "Poster_$time.jpg")
            withContext(Dispatchers.IO) {
              FileOutputStream(file).use { fos ->
                outBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
            }
            outBmp.recycle()
            srcBmp.recycle()
            isExporting = false
            onPosterSaved(Uri.fromFile(file))
          } else {
            isExporting = false
            Toast.makeText(context, "Could not load high-res image", Toast.LENGTH_SHORT).show()
          }
        }
      },
      enabled = !isExporting,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_export_poster"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      if (isExporting) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Exporting High-Res Poster...", color = Color.White)
      } else {
        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export High-Res Poster", fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}

// -------------------------------------------------------------
// 3. WATERMARK STUDIO WORKSPACE
// -------------------------------------------------------------
@Composable
private fun WatermarkStudioWorkspace(
  availablePhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onPhotoSelected: (MediaItem) -> Unit,
  onOpenSignaturePad: () -> Unit,
  onWatermarkSaved: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var watermarkConfig by remember { mutableStateOf(WatermarkConfig()) }
  var isExporting by remember { mutableStateOf(false) }

  val targetPhoto = selectedPhotos.firstOrNull() ?: availablePhotos.firstOrNull()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Watermark Text Input
    OutlinedTextField(
      value = watermarkConfig.text,
      onValueChange = { watermarkConfig = watermarkConfig.copy(text = it) },
      label = { Text("Watermark Copyright Text") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Position Selector (9 Presets)
    Text("Watermark Position", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(WatermarkPosition.values()) { pos ->
        val isSelected = watermarkConfig.position == pos
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface,
          border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
          modifier = Modifier.clickable { watermarkConfig = watermarkConfig.copy(position = pos) }
        ) {
          Text(
            pos.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.accent else colors.primaryText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Sliders: Opacity & Rotation
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Opacity: ${(watermarkConfig.opacity * 100).toInt()}%", fontSize = 12.sp, color = colors.secondaryText)
      Text("Rotation: ${watermarkConfig.rotation.toInt()}°", fontSize = 12.sp, color = colors.secondaryText)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Slider(
        value = watermarkConfig.opacity,
        onValueChange = { watermarkConfig = watermarkConfig.copy(opacity = it) },
        valueRange = 0.1f..1.0f,
        modifier = Modifier.weight(1f)
      )
      Slider(
        value = watermarkConfig.rotation,
        onValueChange = { watermarkConfig = watermarkConfig.copy(rotation = it) },
        valueRange = -45f..45f,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Quick Draw Signature Option
    OutlinedButton(
      onClick = onOpenSignaturePad,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Draw Handwritten Digital Signature")
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Live Watermark Preview
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (targetPhoto?.uri != null) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(targetPhoto.uri).size(600, 600).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

        // Live Simulated Overlay
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          contentAlignment = when (watermarkConfig.position) {
            WatermarkPosition.TOP_LEFT -> Alignment.TopStart
            WatermarkPosition.TOP_CENTER -> Alignment.TopCenter
            WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
            WatermarkPosition.CENTER -> Alignment.Center
            WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
            WatermarkPosition.BOTTOM_CENTER -> Alignment.BottomCenter
            WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
            WatermarkPosition.TILED -> Alignment.Center
          }
        ) {
          Text(
            text = watermarkConfig.text,
            color = Color.White.copy(alpha = watermarkConfig.opacity),
            fontSize = (watermarkConfig.fontSizeSp * 0.75f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = {
        val uriStr = targetPhoto?.uri ?: return@Button
        isExporting = true
        coroutineScope.launch {
          val srcBmp = ImageResizeEngine.loadSafeBitmap(context, Uri.parse(uriStr), 2048)
          if (srcBmp != null) {
            val wmBmp = WatermarkEngine.applyWatermark(srcBmp, watermarkConfig)
            val picsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val outDir = File(picsDir, "My Gallery AI/Watermarked").apply { mkdirs() }
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outDir, "WM_$time.jpg")
            withContext(Dispatchers.IO) {
              FileOutputStream(file).use { fos ->
                wmBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
            }
            wmBmp.recycle()
            srcBmp.recycle()
            isExporting = false
            onWatermarkSaved()
          } else {
            isExporting = false
            Toast.makeText(context, "Failed to decode image", Toast.LENGTH_SHORT).show()
          }
        }
      },
      enabled = !isExporting,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_export_watermark"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      if (isExporting) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Applying Watermark...", color = Color.White)
      } else {
        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Watermarked Copy", fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}

// -------------------------------------------------------------
// 4. FRAMES & STICKERS WORKSPACE
// -------------------------------------------------------------
@Composable
private fun FramesStudioWorkspace(
  availablePhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onPhotoSelected: (MediaItem) -> Unit,
  onFrameSaved: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var frameConfig by remember { mutableStateOf(FrameConfig()) }
  var isExporting by remember { mutableStateOf(false) }

  val targetPhoto = selectedPhotos.firstOrNull() ?: availablePhotos.firstOrNull()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text("Frame Style", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(FrameType.values()) { fType ->
        val isSelected = frameConfig.type == fType
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface,
          border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
          modifier = Modifier.clickable { frameConfig = frameConfig.copy(type = fType) }
        ) {
          Text(
            fType.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.accent else colors.primaryText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Interactive Preview
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(if (frameConfig.type == FrameType.POLAROID) 0.82f else 1f)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(frameConfig.color))
          .padding(
            top = frameConfig.paddingDp.dp,
            start = frameConfig.paddingDp.dp,
            end = frameConfig.paddingDp.dp,
            bottom = if (frameConfig.type == FrameType.POLAROID) (frameConfig.paddingDp * 3.5f).dp else frameConfig.paddingDp.dp
          )
      ) {
        if (targetPhoto?.uri != null) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(targetPhoto.uri).size(600, 600).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(frameConfig.cornerRadiusDp.dp))
          )
        }

        if (frameConfig.type == FrameType.POLAROID) {
          Text(
            text = "Memories • My Gallery AI",
            color = Color.DarkGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(top = 10.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Frame Thickness Slider
    Text("Border Thickness: ${frameConfig.paddingDp.toInt()} dp", fontSize = 12.sp, color = colors.secondaryText)
    Slider(
      value = frameConfig.paddingDp,
      onValueChange = { frameConfig = frameConfig.copy(paddingDp = it) },
      valueRange = 4f..40f
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = {
        val uriStr = targetPhoto?.uri ?: return@Button
        isExporting = true
        coroutineScope.launch {
          val srcBmp = ImageResizeEngine.loadSafeBitmap(context, Uri.parse(uriStr), 2048)
          if (srcBmp != null) {
            val pad = (srcBmp.width * 0.05f).toInt()
            val bottomPad = if (frameConfig.type == FrameType.POLAROID) (pad * 3.5f).toInt() else pad
            val outBmp = Bitmap.createBitmap(srcBmp.width + pad * 2, srcBmp.height + pad + bottomPad, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBmp)
            canvas.drawColor(frameConfig.color)
            canvas.drawBitmap(srcBmp, pad.toFloat(), pad.toFloat(), null)

            val picsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val outDir = File(picsDir, "My Gallery AI/Framed").apply { mkdirs() }
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outDir, "Framed_$time.jpg")
            withContext(Dispatchers.IO) {
              FileOutputStream(file).use { fos ->
                outBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
            }
            outBmp.recycle()
            srcBmp.recycle()
            isExporting = false
            onFrameSaved()
          } else {
            isExporting = false
            Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
          }
        }
      },
      enabled = !isExporting,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_export_frame"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      if (isExporting) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Exporting Framed Photo...", color = Color.White)
      } else {
        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Framed Photo", fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}

// -------------------------------------------------------------
// 5. CONTACT SHEET WORKSPACE
// -------------------------------------------------------------
@Composable
private fun ContactSheetStudioWorkspace(
  availablePhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onTogglePhoto: (MediaItem) -> Unit,
  onExportSuccess: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var sheetConfig by remember { mutableStateOf(ContactSheetConfig()) }
  var isExportingImage by remember { mutableStateOf(false) }
  var isExportingPdf by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text("Grid Columns", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(2, 3, 4, 5).forEach { cols ->
        val isSelected = sheetConfig.columns == cols
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface,
          border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
          modifier = Modifier.clickable { sheetConfig = sheetConfig.copy(columns = cols) }
        ) {
          Text(
            "$cols Columns",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.accent else colors.primaryText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
      value = sheetConfig.headerTitle,
      onValueChange = { sheetConfig = sheetConfig.copy(headerTitle = it) },
      label = { Text("Header Sheet Title") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text("Selected Items (${selectedPhotos.size} photos)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(availablePhotos.take(25)) { photo ->
        val isSelected = selectedPhotos.contains(photo)
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, if (isSelected) colors.accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onTogglePhoto(photo) }
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(photo.uri).size(120, 120).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = {
          if (selectedPhotos.isEmpty()) {
            Toast.makeText(context, "Select at least 1 photo", Toast.LENGTH_SHORT).show()
            return@Button
          }
          isExportingImage = true
          coroutineScope.launch {
            val bmp = ContactSheetEngine.createContactSheetBitmap(context, selectedPhotos, sheetConfig)
            if (bmp != null) {
              val picsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
              val outDir = File(picsDir, "My Gallery AI/ContactSheets").apply { mkdirs() }
              val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
              val file = File(outDir, "ContactSheet_$time.jpg")
              withContext(Dispatchers.IO) {
                FileOutputStream(file).use { fos ->
                  bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
              }
              bmp.recycle()
              isExportingImage = false
              onExportSuccess()
            } else {
              isExportingImage = false
              Toast.makeText(context, "Failed to render contact sheet", Toast.LENGTH_SHORT).show()
            }
          }
        },
        enabled = !isExportingImage && !isExportingPdf,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
      ) {
        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Export as Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = {
          if (selectedPhotos.isEmpty()) {
            Toast.makeText(context, "Select at least 1 photo", Toast.LENGTH_SHORT).show()
            return@OutlinedButton
          }
          isExportingPdf = true
          coroutineScope.launch {
            val res = ContactSheetEngine.createContactSheetPdf(context, selectedPhotos, sheetConfig)
            isExportingPdf = false
            if (res.isSuccess) {
              onExportSuccess()
            } else {
              Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
            }
          }
        },
        enabled = !isExportingImage && !isExportingPdf,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Export as PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

// -------------------------------------------------------------
// 6. GIF CREATOR WORKSPACE
// -------------------------------------------------------------
@Composable
private fun GifStudioWorkspace(
  availablePhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onTogglePhoto: (MediaItem) -> Unit,
  onGifSaved: (Uri) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var gifConfig by remember { mutableStateOf(GifConfig()) }
  var currentPreviewFrame by remember { mutableIntStateOf(0) }
  var isExporting by remember { mutableStateOf(false) }

  // Animated live preview timer
  LaunchedEffect(selectedPhotos.size, gifConfig.frameDelayMs) {
    if (selectedPhotos.isNotEmpty()) {
      while (true) {
        kotlinx.coroutines.delay(gifConfig.frameDelayMs.toLong().coerceAtLeast(100L))
        currentPreviewFrame = (currentPreviewFrame + 1) % selectedPhotos.size
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Live animated preview
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val currentPhoto = selectedPhotos.getOrNull(currentPreviewFrame)
        if (currentPhoto?.uri != null) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(currentPhoto.uri).size(480, 480).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Text("Select at least 2 photos to preview animation", color = colors.secondaryText, fontSize = 13.sp)
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color.Black.copy(alpha = 0.7f),
          modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
        ) {
          Text(
            text = "Frame ${currentPreviewFrame + 1}/${selectedPhotos.size} • ${gifConfig.frameDelayMs}ms",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Frame Delay: ${gifConfig.frameDelayMs} ms", fontSize = 12.sp, color = colors.secondaryText)
    Slider(
      value = gifConfig.frameDelayMs.toFloat(),
      onValueChange = { gifConfig = gifConfig.copy(frameDelayMs = it.toInt()) },
      valueRange = 100f..1000f,
      steps = 9
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text("Select Photos for GIF (${selectedPhotos.size} frames)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(availablePhotos.take(20)) { photo ->
        val isSelected = selectedPhotos.contains(photo)
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, if (isSelected) colors.accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onTogglePhoto(photo) }
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(photo.uri).size(120, 120).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = {
        if (selectedPhotos.size < 2) {
          Toast.makeText(context, "Select at least 2 photos to make a GIF", Toast.LENGTH_SHORT).show()
          return@Button
        }
        isExporting = true
        coroutineScope.launch {
          val frames = mutableListOf<Bitmap>()
          for (item in selectedPhotos) {
            val bmp = item.uri?.let { ImageResizeEngine.loadSafeBitmap(context, Uri.parse(it), 480) }
            if (bmp != null) frames.add(bmp)
          }
          val res = GifEncoder.createAndSaveGif(context, frames, gifConfig)
          frames.forEach { it.recycle() }
          isExporting = false
          if (res.isSuccess) {
            onGifSaved(res.getOrThrow())
          } else {
            Toast.makeText(context, "GIF creation failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
          }
        }
      },
      enabled = !isExporting,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_export_gif"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      if (isExporting) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Encoding Animated GIF...", color = Color.White)
      } else {
        Icon(Icons.Rounded.Animation, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create & Export Animated GIF", fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}

// -------------------------------------------------------------
// 7. BATCH TOOLS WORKSPACE
// -------------------------------------------------------------
@Composable
private fun BatchToolsWorkspace(
  allPhotos: List<MediaItem>,
  selectedPhotos: List<MediaItem>,
  onTogglePhoto: (MediaItem) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onLaunchBatchJob: (BatchJob) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val colors = GalleryTheme.colors

  var operation by remember { mutableStateOf(BatchOperation.RESIZE) }
  var renamePrefix by remember { mutableStateOf("Photo_") }
  var renameStartIndex by remember { mutableIntStateOf(1) }
  var resizeWidth by remember { mutableIntStateOf(1080) }
  var resizeHeight by remember { mutableIntStateOf(1080) }
  var targetSizeBytes by remember { mutableStateOf(500 * 1024L) }
  var isProcessing by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Operation selection pills
    Text("Select Batch Operation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primaryText)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(BatchOperation.values()) { op ->
        val isSelected = operation == op
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface,
          border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
          modifier = Modifier.clickable { operation = op }
        ) {
          Text(
            op.title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.accent else colors.primaryText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Operation specific configuration
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(operation.description, fontSize = 13.sp, color = colors.secondaryText)
        Spacer(modifier = Modifier.height(12.dp))

        when (operation) {
          BatchOperation.RENAME -> {
            OutlinedTextField(
              value = renamePrefix,
              onValueChange = { renamePrefix = it },
              label = { Text("Filename Prefix") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Example output: ${renamePrefix}001.jpg, ${renamePrefix}002.jpg", fontSize = 12.sp, color = colors.accent)
          }

          BatchOperation.RESIZE -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedTextField(
                value = resizeWidth.toString(),
                onValueChange = { resizeWidth = it.toIntOrNull() ?: 1080 },
                label = { Text("Width (px)") },
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = resizeHeight.toString(),
                onValueChange = { resizeHeight = it.toIntOrNull() ?: 1080 },
                label = { Text("Height (px)") },
                modifier = Modifier.weight(1f)
              )
            }
          }

          BatchOperation.COMPRESS -> {
            Text("Target Size Limit: ${targetSizeBytes / 1024} KB per photo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Slider(
              value = (targetSizeBytes / 1024L).toFloat(),
              onValueChange = { targetSizeBytes = it.toLong() * 1024L },
              valueRange = 50f..2000f,
              steps = 19
            )
          }

          else -> {
            Text("Standard preset adjustments will be applied safely to new copies.", fontSize = 12.sp, color = colors.secondaryText)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Multi-selection bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("${selectedPhotos.size} of ${allPhotos.size} photos selected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      Row {
        TextButton(onClick = onSelectAll) { Text("Select All", fontSize = 12.sp) }
        TextButton(onClick = onClearSelection) { Text("Clear", fontSize = 12.sp) }
      }
    }

    // Grid of photos for selection
    LazyVerticalGrid(
      columns = GridCells.Fixed(4),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
    ) {
      items(allPhotos) { photo ->
        val isSelected = selectedPhotos.contains(photo)
        Box(
          modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, if (isSelected) colors.accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onTogglePhoto(photo) }
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(photo.uri).size(120, 120).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          if (isSelected) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(colors.accent),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Start Batch Processing Button
    Button(
      onClick = {
        if (selectedPhotos.isEmpty()) {
          Toast.makeText(context, "Select at least 1 photo for batch processing", Toast.LENGTH_SHORT).show()
          return@Button
        }
        isProcessing = true
        coroutineScope.launch {
          val initialJob = BatchJob(
            id = java.util.UUID.randomUUID().toString(),
            operation = operation,
            totalItems = selectedPhotos.size,
            status = BatchJobStatus.RUNNING
          )
          onLaunchBatchJob(initialJob)

          val finishedJob = BatchProcessingEngine.executeBatchJob(
            context = context,
            items = selectedPhotos,
            operation = operation,
            renameConfig = BatchRenameConfig(prefix = renamePrefix, startNumber = renameStartIndex),
            resizeConfig = BatchResizeParams(targetWidth = resizeWidth, targetHeight = resizeHeight),
            compressionConfig = CompressionConfig(mode = CompressionMode.TARGET_SIZE, targetSizeBytes = targetSizeBytes),
            onProgress = { liveJob ->
              onLaunchBatchJob(liveJob)
            }
          )
          isProcessing = false
          onLaunchBatchJob(finishedJob)
        }
      },
      enabled = !isProcessing && selectedPhotos.isNotEmpty(),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_start_batch_processing"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
    ) {
      Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Process ${selectedPhotos.size} Photos with ${operation.title}", fontWeight = FontWeight.Bold, color = Color.White)
    }
  }
}
