package com.example.ui.screens.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.compression.BatchCompressionItem
import com.example.model.compression.BatchItemStatus
import com.example.model.compression.CompressionConfig
import com.example.model.compression.CompressionExecutionResult
import com.example.model.compression.CompressionMode
import com.example.model.compression.QualityLevel
import com.example.model.compression.SmartCompressPreset
import com.example.model.compression.TargetSizePreset
import com.example.model.compression.formatFileSize
import com.example.model.resize.ImageMetadataInfo
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode
import com.example.ui.screens.compression.components.BatchCompressResultsDialog
import com.example.ui.screens.compression.components.CompareBeforeAfterDialog
import com.example.ui.screens.compression.components.CompressSuccessSheet
import com.example.ui.theme.GalleryTheme
import com.example.utils.compression.ImageCompressionEngine
import com.example.utils.resize.ImageResizeEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageCompressorScreen(
  initialMedia: MediaItem? = null,
  initialBatchMedias: List<MediaItem> = emptyList(),
  allGalleryPhotos: List<MediaItem> = emptyList(),
  initialTab: Int = 0,
  onOpenInViewer: (Uri) -> Unit,
  onOpenInProEditor: (Uri) -> Unit,
  onOpenInResize: (Uri) -> Unit,
  onCreatePdfFromUri: (Uri) -> Unit,
  onCreatePdfFromBatch: (List<Uri>) -> Unit,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val colors = GalleryTheme.colors
  val scope = rememberCoroutineScope()

  // Tab state: 0 = Single Compress, 1 = Batch Compress, 2 = Format Converter, 3 = Smart Presets
  var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

  // Selected single photo state
  var selectedPhoto by remember { mutableStateOf(initialMedia ?: allGalleryPhotos.firstOrNull()) }
  var selectedUri by remember { mutableStateOf(selectedPhoto?.uri?.let { Uri.parse(it) }) }
  var metadataInfo by remember { mutableStateOf(ImageMetadataInfo()) }
  var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isDecoding by remember { mutableStateOf(false) }

  // Compression Configuration
  var compressionMode by remember { mutableStateOf(CompressionMode.TARGET_SIZE) }
  var selectedTargetPreset by remember { mutableStateOf(TargetSizePreset.KB_500) }
  var customTargetValueText by remember { mutableStateOf("500") }
  var customTargetUnitIsMb by remember { mutableStateOf(false) }
  var isStrictMaxSizeMode by remember { mutableStateOf(true) }

  // Quality settings
  var qualitySliderValue by remember { mutableFloatStateOf(80f) }
  var selectedQualityLevel by remember { mutableStateOf(QualityLevel.HIGH) }

  // Smart preset
  var selectedSmartPreset by remember { mutableStateOf(SmartCompressPreset.BALANCED) }

  // Output Format & options
  var selectedOutputFormat by remember { mutableStateOf(OutputFormat.JPEG) }
  var isWebpLossless by remember { mutableStateOf(false) }
  var transparentBgColor by remember { mutableIntStateOf(AndroidColor.WHITE) }

  // Metadata & Privacy
  var preserveMetadata by remember { mutableStateOf(false) }
  var removeMetadata by remember { mutableStateOf(true) }

  // Combined Resize settings
  var enableDimensionResize by remember { mutableStateOf(false) }
  var targetWidthText by remember { mutableStateOf("1080") }
  var targetHeightText by remember { mutableStateOf("1080") }
  var lockAspectRatio by remember { mutableStateOf(true) }
  var selectedResizeMode by remember { mutableStateOf(ResizeMode.ORIGINAL_RATIO) }

  // Live size estimation & preview
  var estimatedSizeBytes by remember { mutableLongStateOf(0L) }
  var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isCalculatingEstimate by remember { mutableStateOf(false) }

  // Batch State
  val batchItems = remember {
    mutableStateListOf<BatchCompressionItem>().apply {
      if (initialBatchMedias.isNotEmpty()) {
        addAll(initialBatchMedias.map {
          BatchCompressionItem(
            id = it.id,
            uri = it.uri ?: "",
            title = it.title,
            originalSize = 0L,
            originalWidth = 0,
            originalHeight = 0,
            originalFormat = "JPEG"
          )
        })
      } else if (allGalleryPhotos.isNotEmpty()) {
        addAll(allGalleryPhotos.take(6).map {
          BatchCompressionItem(
            id = it.id,
            uri = it.uri ?: "",
            title = it.title,
            originalSize = 0L,
            originalWidth = 0,
            originalHeight = 0,
            originalFormat = "JPEG"
          )
        })
      }
    }
  }

  var isBatchProcessing by remember { mutableStateOf(false) }
  var batchProgressCompleted by remember { mutableIntStateOf(0) }
  var batchProgressTotal by remember { mutableIntStateOf(0) }
  var batchCurrentItemName by remember { mutableStateOf("") }
  var batchJob by remember { mutableStateOf<Job?>(null) }
  var showBatchResultsDialog by remember { mutableStateOf(false) }
  val batchFinishedResults = remember { mutableStateListOf<BatchCompressionItem>() }

  // Dialogs & Sheets
  var isExecutingCompression by remember { mutableStateOf(false) }
  var singleSuccessResult by remember { mutableStateOf<CompressionExecutionResult?>(null) }
  var showCompareDialog by remember { mutableStateOf(false) }
  var showPrivacyInfoDialog by remember { mutableStateOf(false) }

  // Photo Pickers
  val singlePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedUri = uri
      selectedPhoto = MediaItem(id = uri.toString(), title = uri.lastPathSegment ?: "Selected Photo", uri = uri.toString())
    }
  }

  val multiplePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
  ) { uris ->
    if (uris.isNotEmpty()) {
      batchItems.clear()
      batchItems.addAll(uris.mapIndexed { idx, uri ->
        BatchCompressionItem(
          id = uri.toString(),
          uri = uri.toString(),
          title = uri.lastPathSegment ?: "Photo_${idx + 1}",
          originalSize = 0L,
          originalWidth = 0,
          originalHeight = 0,
          originalFormat = "JPEG"
        )
      })
      selectedTab = 1
    }
  }

  // Load image metadata & bitmap when selectedUri changes
  LaunchedEffect(selectedUri) {
    val uri = selectedUri ?: return@LaunchedEffect
    isDecoding = true
    try {
      val meta = ImageResizeEngine.readImageMetadata(context, uri)
      metadataInfo = meta
      targetWidthText = meta.widthPx.toString()
      targetHeightText = meta.heightPx.toString()

      val bmp = ImageResizeEngine.loadSafeBitmap(context, uri, maxTargetDim = 2048)
      sourceBitmap = bmp
    } catch (_: Exception) {
    } finally {
      isDecoding = false
    }
  }

  // Re-estimate file size when settings change
  LaunchedEffect(
    sourceBitmap,
    selectedOutputFormat,
    qualitySliderValue,
    isWebpLossless,
    selectedTargetPreset,
    customTargetValueText,
    customTargetUnitIsMb,
    compressionMode,
    selectedSmartPreset
  ) {
    val bmp = sourceBitmap ?: return@LaunchedEffect
    isCalculatingEstimate = true
    try {
      val est = ImageCompressionEngine.estimateCompressedSize(
        bitmap = bmp,
        outputFormat = selectedOutputFormat,
        quality = qualitySliderValue.roundToInt(),
        webpLossless = isWebpLossless,
        transparentBgColor = transparentBgColor
      )
      estimatedSizeBytes = est
    } catch (_: Exception) {
    } finally {
      isCalculatingEstimate = false
    }
  }

  // Helper to construct current config
  fun buildCurrentConfig(): CompressionConfig {
    val targetBytes = if (selectedTargetPreset == TargetSizePreset.CUSTOM) {
      val num = customTargetValueText.toDoubleOrNull() ?: 500.0
      if (customTargetUnitIsMb) (num * 1024 * 1024).toLong() else (num * 1024).toLong()
    } else {
      selectedTargetPreset.bytes
    }

    val width = targetWidthText.toIntOrNull() ?: metadataInfo.widthPx
    val height = targetHeightText.toIntOrNull() ?: metadataInfo.heightPx

    return CompressionConfig(
      mode = compressionMode,
      targetSizeBytes = targetBytes,
      isMaximumSizeMode = isStrictMaxSizeMode,
      qualityPercent = qualitySliderValue.roundToInt(),
      smartPreset = selectedSmartPreset,
      outputFormat = selectedOutputFormat,
      webpLossless = isWebpLossless,
      transparentBgColor = transparentBgColor,
      preserveMetadata = preserveMetadata,
      removeMetadata = removeMetadata,
      enableDimensionResize = enableDimensionResize,
      customWidth = width,
      customHeight = height,
      lockAspectRatio = lockAspectRatio,
      resizeMode = selectedResizeMode
    )
  }

  // Execute Single Compression
  fun executeSingleCompression() {
    val uri = selectedUri ?: return
    isExecutingCompression = true
    scope.launch {
      val config = buildCurrentConfig()
      val result = ImageCompressionEngine.compressImage(
        context = context,
        sourceUri = uri,
        config = config,
        originalTitle = selectedPhoto?.title ?: "photo"
      )
      isExecutingCompression = false
      if (result.isSuccess) {
        singleSuccessResult = result
      } else {
        Toast.makeText(context, result.statusMessage, Toast.LENGTH_LONG).show()
      }
    }
  }

  // Execute Batch Compression
  fun startBatchCompression() {
    if (batchItems.isEmpty()) {
      Toast.makeText(context, "Please add photos for batch compression", Toast.LENGTH_SHORT).show()
      return
    }

    isBatchProcessing = true
    batchProgressCompleted = 0
    batchProgressTotal = batchItems.size
    batchFinishedResults.clear()

    val config = buildCurrentConfig()
    batchJob = scope.launch {
      try {
        val results = ImageCompressionEngine.processBatchCompression(
          context = context,
          items = batchItems.toList(),
          config = config,
          onProgress = { completed, total, currentItem ->
            batchProgressCompleted = completed
            batchProgressTotal = total
            batchCurrentItemName = currentItem.title
          }
        )
        batchFinishedResults.clear()
        batchFinishedResults.addAll(results)
        showBatchResultsDialog = true
      } catch (_: Exception) {
      } finally {
        isBatchProcessing = false
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Image Compressor",
              color = colors.primaryText,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981))
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "100% Offline • On-Device Safe",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("btn_close_compressor")
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
          }
        },
        actions = {
          IconButton(
            onClick = { showPrivacyInfoDialog = true },
            modifier = Modifier.testTag("btn_compressor_privacy")
          ) {
            Icon(Icons.Rounded.Security, contentDescription = "Privacy & Offline", tint = colors.accent)
          }

          if (selectedTab == 0 && sourceBitmap != null) {
            IconButton(
              onClick = {
                scope.launch {
                  val preview = ImageCompressionEngine.generateCompressedPreview(
                    bitmap = sourceBitmap!!,
                    outputFormat = selectedOutputFormat,
                    quality = qualitySliderValue.roundToInt(),
                    webpLossless = isWebpLossless,
                    transparentBgColor = transparentBgColor
                  )
                  previewBitmap = preview
                  showCompareDialog = true
                }
              },
              modifier = Modifier.testTag("btn_compare_preview")
            ) {
              Icon(Icons.Rounded.Compare, contentDescription = "Compare", tint = Color(0xFF38BDF8))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
      )
    },
    containerColor = colors.background
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 4 Core Tabs
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = colors.surface,
        contentColor = colors.accent
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = {
            selectedTab = 0
            compressionMode = CompressionMode.TARGET_SIZE
          },
          text = { Text("Target Size", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.Compress, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Batch (${batchItems.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.Collections, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = {
            selectedTab = 2
            compressionMode = CompressionMode.CONVERT_FORMAT
          },
          text = { Text("Converter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        Tab(
          selected = selectedTab == 3,
          onClick = {
            selectedTab = 3
            compressionMode = CompressionMode.SMART_COMPRESS
          },
          text = { Text("Smart Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
      }

      // Content for Tabs
      Box(modifier = Modifier.weight(1f)) {
        when (selectedTab) {
          0 -> SingleCompressTabContent(
            selectedPhoto = selectedPhoto,
            selectedUri = selectedUri,
            metadataInfo = metadataInfo,
            isDecoding = isDecoding,
            allPhotos = allGalleryPhotos,
            onPickSingle = {
              singlePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSelectRecent = {
              selectedPhoto = it
              selectedUri = it.uri?.let { u -> Uri.parse(u) }
            },
            compressionMode = compressionMode,
            onCompressionModeChange = { compressionMode = it },
            selectedTargetPreset = selectedTargetPreset,
            onTargetPresetSelected = { selectedTargetPreset = it },
            customTargetValueText = customTargetValueText,
            onCustomTargetValueChange = { customTargetValueText = it },
            customTargetUnitIsMb = customTargetUnitIsMb,
            onCustomTargetUnitToggle = { customTargetUnitIsMb = it },
            isStrictMaxSizeMode = isStrictMaxSizeMode,
            onStrictMaxSizeModeToggle = { isStrictMaxSizeMode = it },
            qualitySliderValue = qualitySliderValue,
            onQualitySliderChange = {
              qualitySliderValue = it
              selectedQualityLevel = QualityLevel.CUSTOM
            },
            selectedQualityLevel = selectedQualityLevel,
            onQualityLevelSelect = {
              selectedQualityLevel = it
              qualitySliderValue = it.qualityValue.toFloat()
            },
            selectedOutputFormat = selectedOutputFormat,
            onOutputFormatChange = { selectedOutputFormat = it },
            isWebpLossless = isWebpLossless,
            onWebpLosslessChange = { isWebpLossless = it },
            transparentBgColor = transparentBgColor,
            onTransparentBgColorChange = { transparentBgColor = it },
            enableDimensionResize = enableDimensionResize,
            onEnableDimensionResizeToggle = { enableDimensionResize = it },
            targetWidthText = targetWidthText,
            targetHeightText = targetHeightText,
            onTargetWidthChange = { targetWidthText = it },
            onTargetHeightChange = { targetHeightText = it },
            lockAspectRatio = lockAspectRatio,
            onLockAspectRatioToggle = { lockAspectRatio = it },
            selectedResizeMode = selectedResizeMode,
            onResizeModeSelect = { selectedResizeMode = it },
            preserveMetadata = preserveMetadata,
            removeMetadata = removeMetadata,
            onPreserveMetadataToggle = {
              preserveMetadata = it
              if (it) removeMetadata = false
            },
            onRemoveMetadataToggle = {
              removeMetadata = it
              if (it) preserveMetadata = false
            },
            estimatedSizeBytes = estimatedSizeBytes,
            isCalculatingEstimate = isCalculatingEstimate,
            onCompareClick = {
              if (sourceBitmap != null) {
                scope.launch {
                  val preview = ImageCompressionEngine.generateCompressedPreview(
                    bitmap = sourceBitmap!!,
                    outputFormat = selectedOutputFormat,
                    quality = qualitySliderValue.roundToInt(),
                    webpLossless = isWebpLossless,
                    transparentBgColor = transparentBgColor
                  )
                  previewBitmap = preview
                  showCompareDialog = true
                }
              }
            }
          )

          1 -> BatchCompressTabContent(
            items = batchItems,
            onAddFromGallery = {
              multiplePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveItem = { item -> batchItems.remove(item) },
            onClearAll = { batchItems.clear() },
            selectedTargetPreset = selectedTargetPreset,
            onTargetPresetSelected = { selectedTargetPreset = it },
            isStrictMaxSizeMode = isStrictMaxSizeMode,
            onStrictMaxSizeModeToggle = { isStrictMaxSizeMode = it },
            selectedOutputFormat = selectedOutputFormat,
            onOutputFormatChange = { selectedOutputFormat = it },
            selectedQuality = qualitySliderValue.roundToInt(),
            onQualityChange = { qualitySliderValue = it.toFloat() }
          )

          2 -> FormatConverterTabContent(
            selectedPhoto = selectedPhoto,
            selectedUri = selectedUri,
            metadataInfo = metadataInfo,
            onPickSingle = {
              singlePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            selectedOutputFormat = selectedOutputFormat,
            onOutputFormatChange = { selectedOutputFormat = it },
            isWebpLossless = isWebpLossless,
            onWebpLosslessChange = { isWebpLossless = it },
            transparentBgColor = transparentBgColor,
            onTransparentBgColorChange = { transparentBgColor = it },
            qualityValue = qualitySliderValue,
            onQualityChange = { qualitySliderValue = it },
            estimatedSizeBytes = estimatedSizeBytes
          )

          3 -> SmartPresetsTabContent(
            selectedPreset = selectedSmartPreset,
            onPresetSelect = {
              selectedSmartPreset = it
              compressionMode = CompressionMode.SMART_COMPRESS
              qualitySliderValue = it.defaultQuality.toFloat()
              selectedTab = 0
            }
          )
        }
      }

      // Bottom Execution Bar
      Surface(
        color = colors.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = if (selectedTab == 1) "Batch Queue: ${batchItems.size} items" else "Output: ${selectedOutputFormat.displayName}",
              color = colors.primaryText,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (selectedTab == 1) "Process all files concurrently"
              else "Est: ~${formatFileSize(estimatedSizeBytes)} (Original: ${formatFileSize(metadataInfo.sizeBytes)})",
              color = Color(0xFF0EA5E9),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          if (selectedTab == 1) {
            Button(
              onClick = { startBatchCompression() },
              enabled = batchItems.isNotEmpty() && !isBatchProcessing,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              modifier = Modifier.testTag("btn_start_batch_compress")
            ) {
              Icon(Icons.Rounded.Compress, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Batch Compress", fontWeight = FontWeight.Bold)
            }
          } else {
            Button(
              onClick = { executeSingleCompression() },
              enabled = selectedUri != null && !isExecutingCompression,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              modifier = Modifier.testTag("btn_compress_and_save")
            ) {
              if (isExecutingCompression) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Optimizing…", fontWeight = FontWeight.Bold)
              } else {
                Icon(Icons.Rounded.Compress, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compress & Save", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // --- Modals & Overlays ---

  // 1. Batch Progress Dialog with Cancellation
  if (isBatchProcessing) {
    AlertDialog(
      onDismissRequest = { /* prevent dismissal without cancellation */ },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Text("Batch Compressing…", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            text = "Processing $batchProgressCompleted of $batchProgressTotal images",
            color = colors.primaryText,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = { if (batchProgressTotal > 0) batchProgressCompleted.toFloat() / batchProgressTotal else 0f },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = batchCurrentItemName,
            color = colors.secondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(
          onClick = {
            batchJob?.cancel()
            isBatchProcessing = false
            Toast.makeText(context, "Cancelled batch compression", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Cancel", color = colors.warning)
        }
      }
    )
  }

  // 2. Batch Results Dialog
  if (showBatchResultsDialog) {
    BatchCompressResultsDialog(
      results = batchFinishedResults,
      onRetryFailed = {
        showBatchResultsDialog = false
        val failedUris = batchFinishedResults.filter { it.status == BatchItemStatus.FAILED }
        batchItems.clear()
        batchItems.addAll(failedUris)
        startBatchCompression()
      },
      onCreatePdfFromBatch = { uris ->
        showBatchResultsDialog = false
        onCreatePdfFromBatch(uris)
      },
      onDismiss = { showBatchResultsDialog = false }
    )
  }

  // 3. Single Compress Success Sheet
  singleSuccessResult?.let { result ->
    CompressSuccessSheet(
      result = result,
      onDismiss = { singleSuccessResult = null },
      onOpenInViewer = onOpenInViewer,
      onOpenInEditor = onOpenInProEditor,
      onOpenInResize = onOpenInResize,
      onCreatePdf = onCreatePdfFromUri,
      onCompressAgain = { singleSuccessResult = null }
    )
  }

  // 4. Compare Before & After Dialog
  if (showCompareDialog) {
    CompareBeforeAfterDialog(
      originalUri = selectedUri,
      originalSize = metadataInfo.sizeBytes,
      originalDims = "${metadataInfo.widthPx} × ${metadataInfo.heightPx}",
      compressedBitmap = previewBitmap,
      compressedSize = estimatedSizeBytes,
      compressedDims = "${metadataInfo.widthPx} × ${metadataInfo.heightPx}",
      qualityUsed = qualitySliderValue.roundToInt(),
      formatName = selectedOutputFormat.displayName,
      onDismiss = { showCompareDialog = false }
    )
  }

  // 5. Offline Privacy Dialog
  if (showPrivacyInfoDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyInfoDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = Color(0xFF10B981))
          Spacer(modifier = Modifier.width(8.dp))
          Text("100% On-Device Privacy Guarantee")
        }
      },
      text = {
        Column {
          Text(
            text = "All image compression, dimension resizing and format conversions are executed locally on your Android device using native system codecs.",
            fontSize = 13.sp,
            color = colors.primaryText
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "• Zero Cloud Uploads: No photos are sent to external servers or AI APIs.\n• Safe Copying: Original photos are never overwritten.\n• Metadata Shield: Optional EXIF scrubbing removes GPS and camera serial tags for privacy.",
            fontSize = 12.sp,
            color = colors.secondaryText
          )
        }
      },
      confirmButton = {
        Button(onClick = { showPrivacyInfoDialog = false }) {
          Text("Got It")
        }
      }
    )
  }
}

// ==========================================
// SUB-COMPONENTS: TAB 0 - SINGLE COMPRESS
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleCompressTabContent(
  selectedPhoto: MediaItem?,
  selectedUri: Uri?,
  metadataInfo: ImageMetadataInfo,
  isDecoding: Boolean,
  allPhotos: List<MediaItem>,
  onPickSingle: () -> Unit,
  onSelectRecent: (MediaItem) -> Unit,
  compressionMode: CompressionMode,
  onCompressionModeChange: (CompressionMode) -> Unit,
  selectedTargetPreset: TargetSizePreset,
  onTargetPresetSelected: (TargetSizePreset) -> Unit,
  customTargetValueText: String,
  onCustomTargetValueChange: (String) -> Unit,
  customTargetUnitIsMb: Boolean,
  onCustomTargetUnitToggle: (Boolean) -> Unit,
  isStrictMaxSizeMode: Boolean,
  onStrictMaxSizeModeToggle: (Boolean) -> Unit,
  qualitySliderValue: Float,
  onQualitySliderChange: (Float) -> Unit,
  selectedQualityLevel: QualityLevel,
  onQualityLevelSelect: (QualityLevel) -> Unit,
  selectedOutputFormat: OutputFormat,
  onOutputFormatChange: (OutputFormat) -> Unit,
  isWebpLossless: Boolean,
  onWebpLosslessChange: (Boolean) -> Unit,
  transparentBgColor: Int,
  onTransparentBgColorChange: (Int) -> Unit,
  enableDimensionResize: Boolean,
  onEnableDimensionResizeToggle: (Boolean) -> Unit,
  targetWidthText: String,
  targetHeightText: String,
  onTargetWidthChange: (String) -> Unit,
  onTargetHeightChange: (String) -> Unit,
  lockAspectRatio: Boolean,
  onLockAspectRatioToggle: (Boolean) -> Unit,
  selectedResizeMode: ResizeMode,
  onResizeModeSelect: (ResizeMode) -> Unit,
  preserveMetadata: Boolean,
  removeMetadata: Boolean,
  onPreserveMetadataToggle: (Boolean) -> Unit,
  onRemoveMetadataToggle: (Boolean) -> Unit,
  estimatedSizeBytes: Long,
  isCalculatingEstimate: Boolean,
  onCompareClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Image Selector & Preview Header Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              if (selectedUri != null) {
                AsyncImage(
                  model = selectedUri,
                  contentDescription = "Selected Photo",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = colors.accent)
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = metadataInfo.fileName.take(22),
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${metadataInfo.widthPx} × ${metadataInfo.heightPx} px • ${metadataInfo.formatName}",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
              Text(
                text = "Size: ${metadataInfo.formattedSize}",
                color = Color(0xFF0EA5E9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          OutlinedButton(
            onClick = onPickSingle,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("btn_pick_image_compress")
          ) {
            Text("Change", fontSize = 12.sp)
          }
        }

        // Recent Photos Quick Row
        if (allPhotos.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text("Recent Gallery Images:", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allPhotos.take(12)) { item ->
              AsyncImage(
                model = item.uri,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .border(
                    width = if (item.uri == selectedUri?.toString()) 2.dp else 0.5.dp,
                    color = if (item.uri == selectedUri?.toString()) colors.accent else colors.border,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .clickable { onSelectRecent(item) }
              )
            }
          }
        }
      }
    }

    // 2. Mode Selector (Target Size vs Quality vs Strict Max Size)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Compression Mode", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ModeSelectChip(
            label = "Target Size",
            selected = compressionMode == CompressionMode.TARGET_SIZE,
            onClick = { onCompressionModeChange(CompressionMode.TARGET_SIZE) },
            modifier = Modifier.weight(1f)
          )
          ModeSelectChip(
            label = "Quality Mode",
            selected = compressionMode == CompressionMode.QUALITY,
            onClick = { onCompressionModeChange(CompressionMode.QUALITY) },
            modifier = Modifier.weight(1f)
          )
          ModeSelectChip(
            label = "Max File Size",
            selected = compressionMode == CompressionMode.MAXIMUM_SIZE,
            onClick = { onCompressionModeChange(CompressionMode.MAXIMUM_SIZE) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // 3. TARGET FILE SIZE SETTINGS (When in TARGET_SIZE or MAXIMUM_SIZE mode)
    if (compressionMode == CompressionMode.TARGET_SIZE || compressionMode == CompressionMode.MAXIMUM_SIZE) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Target File Size Preset", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Select desired file weight boundary", color = colors.secondaryText, fontSize = 11.sp)
            }
            if (compressionMode == CompressionMode.MAXIMUM_SIZE) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF10B981).copy(alpha = 0.2f))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text("Strict Max <= Target", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Target Size Preset Pills
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            TargetSizePreset.values().forEach { preset ->
              FilterChip(
                selected = selectedTargetPreset == preset,
                onClick = { onTargetPresetSelected(preset) },
                label = { Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.accent,
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          // Custom Input if selected
          if (selectedTargetPreset == TargetSizePreset.CUSTOM) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = customTargetValueText,
                onValueChange = onCustomTargetValueChange,
                label = { Text("Custom Size") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
              )

              Button(
                onClick = { onCustomTargetUnitToggle(!customTargetUnitIsMb) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (customTargetUnitIsMb) colors.accent else colors.surfaceVariant
                )
              ) {
                Text(if (customTargetUnitIsMb) "MB" else "KB", color = if (customTargetUnitIsMb) Color.White else colors.primaryText)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Strict Maximum Size Switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Strict Maximum File Size (<= Target)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Attempts smart downscaling if quality reduction alone cannot fit target", color = colors.secondaryText, fontSize = 11.sp)
            }
            Switch(
              checked = isStrictMaxSizeMode,
              onCheckedChange = onStrictMaxSizeModeToggle,
              colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
            )
          }

          // Disclaimer Note
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "• Compression depends on image content and complexity.\n• If target size cannot be reached without excessive quality degradation, the app will inform you honestly.",
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 14.sp
          )
        }
      }
    }

    // 4. QUALITY MODE CONTROLS (Always available or prominent in Quality mode)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Image Quality Controls", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("${qualitySliderValue.roundToInt()}%", color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (selectedOutputFormat == OutputFormat.PNG) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "PNG uses lossless compression to preserve full transparency and clarity. Quality slider does not degrade PNG.",
            color = colors.secondaryText,
            fontSize = 11.sp
          )
        } else {
          Spacer(modifier = Modifier.height(8.dp))

          // Quality Levels
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            QualityLevel.values().take(4).forEach { lvl ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (selectedQualityLevel == lvl) colors.accent else colors.surfaceVariant)
                  .clickable { onQualityLevelSelect(lvl) }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = lvl.label,
                  color = if (selectedQualityLevel == lvl) Color.White else colors.primaryText,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Slider
          Slider(
            value = qualitySliderValue,
            onValueChange = onQualitySliderChange,
            valueRange = 10f..100f,
            steps = 90,
            colors = SliderDefaults.colors(
              thumbColor = colors.accent,
              activeTrackColor = colors.accent
            )
          )
        }
      }
    }

    // 5. OUTPUT FORMAT SELECTOR (JPG, PNG, WebP)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Output Format", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutputFormat.values().forEach { fmt ->
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selectedOutputFormat == fmt) colors.accent else colors.surfaceVariant)
                .clickable { onOutputFormatChange(fmt) }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = fmt.displayName,
                color = if (selectedOutputFormat == fmt) Color.White else colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Format-specific warnings and options
        if (selectedOutputFormat == OutputFormat.JPEG && metadataInfo.hasAlpha) {
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(colors.warning.copy(alpha = 0.15f))
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "JPEG does not support transparency. Transparent areas will be filled with your selected background color.",
              color = colors.primaryText,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text("Fill Background Color:", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BgColorOption(color = AndroidColor.WHITE, label = "White", isSelected = transparentBgColor == AndroidColor.WHITE) {
              onTransparentBgColorChange(AndroidColor.WHITE)
            }
            BgColorOption(color = AndroidColor.BLACK, label = "Black", isSelected = transparentBgColor == AndroidColor.BLACK) {
              onTransparentBgColorChange(AndroidColor.BLACK)
            }
            BgColorOption(color = AndroidColor.LTGRAY, label = "Light Gray", isSelected = transparentBgColor == AndroidColor.LTGRAY) {
              onTransparentBgColorChange(AndroidColor.LTGRAY)
            }
          }
        } else if (selectedOutputFormat == OutputFormat.WEBP) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("WebP Lossless Mode", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Maintains perfect pixels at larger file size", color = colors.secondaryText, fontSize = 11.sp)
            }
            Switch(
              checked = isWebpLossless,
              onCheckedChange = onWebpLosslessChange,
              colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
            )
          }
        }
      }
    }

    // 6. COMBINED RESIZE + COMPRESS SECTION (Prompt 10 Connection)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Resize Dimensions + Compress", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Change pixel size in the same single-pass operation", color = colors.secondaryText, fontSize = 11.sp)
          }
          Switch(
            checked = enableDimensionResize,
            onCheckedChange = onEnableDimensionResizeToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
          )
        }

        AnimatedVisibility(visible = enableDimensionResize) {
          Column(modifier = Modifier.padding(top = 10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = targetWidthText,
                onValueChange = onTargetWidthChange,
                label = { Text("Width (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
              )

              IconButton(onClick = { onLockAspectRatioToggle(!lockAspectRatio) }) {
                Icon(
                  imageVector = if (lockAspectRatio) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                  contentDescription = "Lock Ratio",
                  tint = if (lockAspectRatio) colors.accent else colors.secondaryText
                )
              }

              OutlinedTextField(
                value = targetHeightText,
                onValueChange = onTargetHeightChange,
                label = { Text("Height (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Fitting Mode:", color = colors.secondaryText, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              ResizeMode.values().take(3).forEach { mode ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedResizeMode == mode) colors.accent else colors.surfaceVariant)
                    .clickable { onResizeModeSelect(mode) }
                    .padding(vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = mode.label.substringBefore(" "),
                    color = if (selectedResizeMode == mode) Color.White else colors.primaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }
        }
      }
    }

    // 7. METADATA & PRIVACY CONTROLS
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Metadata & Privacy Shield", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Remove Metadata (Privacy Mode)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Strips GPS locations, camera serials and device data to save bytes & protect privacy", color = colors.secondaryText, fontSize = 11.sp)
          }
          Switch(
            checked = removeMetadata,
            onCheckedChange = onRemoveMetadataToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Preserve Camera EXIF", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Copies capture date, lens model and exposure data to new file", color = colors.secondaryText, fontSize = 11.sp)
          }
          Switch(
            checked = preserveMetadata,
            onCheckedChange = onPreserveMetadataToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
          )
        }
      }
    }

    // 8. LIVE PREVIEW & COMPARISON TRIGGER BUTTON
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onCompareClick() }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Compare, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Interactive Compare Preview", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Tap to view side-by-side or split visual check", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0284C7))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text("Preview", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ==========================================
// SUB-COMPONENTS: TAB 1 - BATCH COMPRESS
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchCompressTabContent(
  items: List<BatchCompressionItem>,
  onAddFromGallery: () -> Unit,
  onRemoveItem: (BatchCompressionItem) -> Unit,
  onClearAll: () -> Unit,
  selectedTargetPreset: TargetSizePreset,
  onTargetPresetSelected: (TargetSizePreset) -> Unit,
  isStrictMaxSizeMode: Boolean,
  onStrictMaxSizeModeToggle: (Boolean) -> Unit,
  selectedOutputFormat: OutputFormat,
  onOutputFormatChange: (OutputFormat) -> Unit,
  selectedQuality: Int,
  onQualityChange: (Int) -> Unit
) {
  val colors = GalleryTheme.colors
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header & Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Batch Compression Queue", color = colors.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("${items.size} photos queued", color = colors.secondaryText, fontSize = 12.sp)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onAddFromGallery,
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Photos", fontSize = 12.sp)
        }

        if (items.isNotEmpty()) {
          TextButton(onClick = onClearAll) {
            Text("Clear", color = colors.warning, fontSize = 12.sp)
          }
        }
      }
    }

    // Common Target Settings Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Common Target Size for All Images", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          TargetSizePreset.values().take(6).forEach { preset ->
            FilterChip(
              selected = selectedTargetPreset == preset,
              onClick = { onTargetPresetSelected(preset) },
              label = { Text(preset.label, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accent,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Target Output Format", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutputFormat.values().forEach { fmt ->
              FilterChip(
                selected = selectedOutputFormat == fmt,
                onClick = { onOutputFormatChange(fmt) },
                label = { Text(fmt.displayName, fontSize = 11.sp) }
              )
            }
          }
        }
      }
    }

    // Queued Items List
    if (items.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Rounded.Collections, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text("No photos queued", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("Tap 'Add Photos' to select multiple items from your gallery.", color = colors.secondaryText, fontSize = 12.sp)
        }
      }
    } else {
      Text("Images to Compress:", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      items.forEach { item ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surface),
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              AsyncImage(
                model = item.uri,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(36.dp)
                  .clip(RoundedCornerShape(6.dp))
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = item.title,
                color = colors.primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            IconButton(
              onClick = { onRemoveItem(item) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = "Remove", tint = colors.accent, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ==========================================
// SUB-COMPONENTS: TAB 2 - FORMAT CONVERTER
// ==========================================

@Composable
private fun FormatConverterTabContent(
  selectedPhoto: MediaItem?,
  selectedUri: Uri?,
  metadataInfo: ImageMetadataInfo,
  onPickSingle: () -> Unit,
  selectedOutputFormat: OutputFormat,
  onOutputFormatChange: (OutputFormat) -> Unit,
  isWebpLossless: Boolean,
  onWebpLosslessChange: (Boolean) -> Unit,
  transparentBgColor: Int,
  onTransparentBgColorChange: (Int) -> Unit,
  qualityValue: Float,
  onQualityChange: (Float) -> Unit,
  estimatedSizeBytes: Long
) {
  val colors = GalleryTheme.colors
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Info Header
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          AsyncImage(
            model = selectedUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(8.dp))
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Source: ${metadataInfo.formatName}", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${metadataInfo.widthPx} × ${metadataInfo.heightPx} px", color = colors.secondaryText, fontSize = 12.sp)
            Text(metadataInfo.formattedSize, color = Color(0xFF0EA5E9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
        OutlinedButton(onClick = onPickSingle, shape = RoundedCornerShape(8.dp)) {
          Text("Change", fontSize = 11.sp)
        }
      }
    }

    // Convert Target Cards (JPG, PNG, WebP)
    Text("Select Target Format:", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    FormatOptionCard(
      format = OutputFormat.JPEG,
      title = "JPG / JPEG",
      description = "Universal compatibility across all devices, email & portals. Lossy compression.",
      badge = "Universal",
      isSelected = selectedOutputFormat == OutputFormat.JPEG,
      onClick = { onOutputFormatChange(OutputFormat.JPEG) }
    )

    FormatOptionCard(
      format = OutputFormat.PNG,
      title = "PNG (Lossless)",
      description = "Preserves 100% pixel sharpness and transparent alpha channels.",
      badge = "Transparent",
      isSelected = selectedOutputFormat == OutputFormat.PNG,
      onClick = { onOutputFormatChange(OutputFormat.PNG) }
    )

    FormatOptionCard(
      format = OutputFormat.WEBP,
      title = "WebP (Modern)",
      description = "Google's next-gen format with 30%+ smaller size than JPEG at identical quality.",
      badge = "Ultra Compact",
      isSelected = selectedOutputFormat == OutputFormat.WEBP,
      onClick = { onOutputFormatChange(OutputFormat.WEBP) }
    )

    // Transparent source warning when converting to JPEG
    if (selectedOutputFormat == OutputFormat.JPEG && metadataInfo.hasAlpha) {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, colors.warning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("Transparency Replacement:", color = colors.warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("JPEG does not support transparency. Choose background fill color:", color = colors.secondaryText, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BgColorOption(color = AndroidColor.WHITE, label = "White", isSelected = transparentBgColor == AndroidColor.WHITE) {
              onTransparentBgColorChange(AndroidColor.WHITE)
            }
            BgColorOption(color = AndroidColor.BLACK, label = "Black", isSelected = transparentBgColor == AndroidColor.BLACK) {
              onTransparentBgColorChange(AndroidColor.BLACK)
            }
            BgColorOption(color = AndroidColor.LTGRAY, label = "Light Gray", isSelected = transparentBgColor == AndroidColor.LTGRAY) {
              onTransparentBgColorChange(AndroidColor.LTGRAY)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ==========================================
// SUB-COMPONENTS: TAB 3 - SMART PRESETS
// ==========================================

@Composable
private fun SmartPresetsTabContent(
  selectedPreset: SmartCompressPreset,
  onPresetSelect: (SmartCompressPreset) -> Unit
) {
  val colors = GalleryTheme.colors
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Smart Optimization Presets",
      color = colors.primaryText,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Balanced rules automatically adjusting dimensions, quality and target size for your specific sharing scenario.",
      color = colors.secondaryText,
      fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(4.dp))

    SmartCompressPreset.values().forEach { preset ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (selectedPreset == preset) colors.accent.copy(alpha = 0.12f) else colors.surface
        ),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (selectedPreset == preset) 1.5.dp else 0.5.dp,
            color = if (selectedPreset == preset) colors.accent else colors.border.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp)
          )
          .clickable { onPresetSelect(preset) }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = preset.label,
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(colors.surfaceVariant)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "< ${formatFileSize(preset.recommendedTargetBytes)}",
                  color = Color(0xFF0EA5E9),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = preset.description,
              color = colors.secondaryText,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }

          RadioButton(
            selected = selectedPreset == preset,
            onClick = { onPresetSelect(preset) }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// Helper UI Elements
@Composable
private fun ModeSelectChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) colors.accent else colors.surfaceVariant)
      .clickable { onClick() }
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else colors.primaryText,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun FormatOptionCard(
  format: OutputFormat,
  title: String,
  description: String,
  badge: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.surface
    ),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isSelected) 1.5.dp else 0.5.dp,
        color = if (isSelected) colors.accent else colors.border.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
      )
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(title, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(colors.accent.copy(alpha = 0.18f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(badge, color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(description, color = colors.secondaryText, fontSize = 11.sp)
      }
      RadioButton(selected = isSelected, onClick = onClick)
    }
  }
}

@Composable
private fun BgColorOption(
  color: Int,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(colors.surfaceVariant)
      .border(if (isSelected) 1.5.dp else 0.dp, colors.accent, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Box(
      modifier = Modifier
        .size(16.dp)
        .clip(CircleShape)
        .background(Color(color))
        .border(1.dp, Color.Gray, CircleShape)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(label, color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
  }
}
