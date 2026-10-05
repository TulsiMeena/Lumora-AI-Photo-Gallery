package com.example.ui.screens.resize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.resize.BatchResizeConfig
import com.example.model.resize.DimensionCategory
import com.example.model.resize.DimensionPreset
import com.example.model.resize.ImageMetadataInfo
import com.example.model.resize.OutputFormat
import com.example.model.resize.PassportStandard
import com.example.model.resize.PhotoSheetConfig
import com.example.model.resize.PhotoSheetPaperSize
import com.example.model.resize.QualityPreset
import com.example.model.resize.RenamePattern
import com.example.model.resize.ResizeExecutionResult
import com.example.model.resize.ResizeMode
import com.example.model.resize.ResizePresets
import com.example.model.resize.ResizeUnit
import com.example.ui.components.MediaThumbnailCard
import com.example.ui.screens.resize.components.CropBeforeResizeDialog
import com.example.ui.screens.resize.components.ImageInfoComparisonDialog
import com.example.ui.screens.resize.components.PassportGuideOverlay
import com.example.ui.screens.resize.components.ResizeSuccessSheet
import com.example.ui.theme.GalleryTheme
import com.example.utils.resize.ImageResizeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageResizeScreen(
  initialMedia: MediaItem? = null,
  allGalleryPhotos: List<MediaItem> = emptyList(),
  initialTab: Int = 0,
  onOpenInViewer: (Uri) -> Unit = {},
  onOpenInProEditor: (Uri) -> Unit = {},
  onCreatePdfFromUri: (Uri) -> Unit = {},
  onClose: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
  var currentMedia by remember { mutableStateOf(initialMedia) }
  var currentUri by remember { mutableStateOf(initialMedia?.uri?.let { Uri.parse(it) }) }

  // Loaded Bitmap & Metadata
  var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var originalMetadata by remember { mutableStateOf(ImageMetadataInfo()) }
  var isLoadingBitmap by remember { mutableStateOf(false) }

  // Units & Dimensions State
  var currentUnit by remember { mutableStateOf(ResizeUnit.PIXELS) }
  var currentDpi by remember { mutableIntStateOf(300) }
  var customDpiText by remember { mutableStateOf("300") }
  var isAspectLocked by remember { mutableStateOf(true) }

  // Numerical inputs
  var widthInput by remember { mutableStateOf("1080") }
  var heightInput by remember { mutableStateOf("1080") }
  var originalRatio by remember { mutableFloatStateOf(1.0f) }

  // Resize Mode
  var selectedMode by remember { mutableStateOf(ResizeMode.FIT) }

  // Output Format & Quality
  var selectedFormat by remember { mutableStateOf(OutputFormat.JPEG) }
  var selectedQualityPreset by remember { mutableStateOf(QualityPreset.HIGH) }
  var customQuality by remember { mutableIntStateOf(90) }
  var preserveMetadata by remember { mutableStateOf(false) }
  var jpegFallbackBgColor by remember { mutableIntStateOf(AndroidColor.WHITE) }

  // Passport Studio State
  var selectedPassportStandard by remember { mutableStateOf(ResizePresets.PASSPORT_STANDARDS[0]) }
  var passportBgColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
  var showPassportGuide by remember { mutableStateOf(true) }

  // Photo Sheet State
  var photoSheetPaper by remember { mutableStateOf(PhotoSheetPaperSize.FOUR_BY_SIX) }
  var photoSheetCopies by remember { mutableIntStateOf(8) }
  var photoSheetSpacingMm by remember { mutableFloatStateOf(3.0f) }
  var photoSheetMarginMm by remember { mutableFloatStateOf(8.0f) }
  var photoSheetIsLandscape by remember { mutableStateOf(false) }
  var photoSheetDrawCutLines by remember { mutableStateOf(true) }
  var sheetPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isGeneratingSheetPreview by remember { mutableStateOf(false) }

  // Batch Resize State
  val selectedBatchItems = remember { mutableStateListOf<MediaItem>() }
  var batchRenamePattern by remember { mutableStateOf(RenamePattern.ORIGINAL_SUFFIX) }
  var batchCustomPrefix by remember { mutableStateOf("photo") }
  var isBatchProcessing by remember { mutableStateOf(false) }
  var batchProgressCurrent by remember { mutableIntStateOf(0) }
  var batchProgressTotal by remember { mutableIntStateOf(0) }
  var batchReportMessage by remember { mutableStateOf<String?>(null) }

  // Dialog & Sheet States
  var showCropDialog by remember { mutableStateOf(false) }
  var showSpecsDialog by remember { mutableStateOf(false) }
  var executionResult by remember { mutableStateOf<ResizeExecutionResult?>(null) }
  var isProcessingSave by remember { mutableStateOf(false) }

  // Media Picker Launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      currentUri = uri
      currentMedia = MediaItem(
        id = uri.toString(),
        title = uri.lastPathSegment ?: "Imported Photo",
        uri = uri.toString()
      )
    }
  }

  // Load Image & Metadata when URI changes
  LaunchedEffect(currentUri) {
    val uri = currentUri ?: return@LaunchedEffect
    isLoadingBitmap = true
    val meta = ImageResizeEngine.readImageMetadata(context, uri)
    originalMetadata = meta

    val bmp = ImageResizeEngine.loadSafeBitmap(context, uri, maxTargetDim = 4096)
    loadedBitmap = bmp
    isLoadingBitmap = false

    if (bmp != null) {
      originalRatio = bmp.width.toFloat() / bmp.height.toFloat()
      // Initial dimension setup
      if (selectedTab == 1) {
        // Passport standard
        val (pW, pH) = selectedPassportStandard.getPixels(currentDpi)
        widthInput = ImageResizeEngine.formatUnitValue(
          ImageResizeEngine.fromPixels(pW, currentUnit, currentDpi),
          currentUnit
        )
        heightInput = ImageResizeEngine.formatUnitValue(
          ImageResizeEngine.fromPixels(pH, currentUnit, currentDpi),
          currentUnit
        )
      } else {
        widthInput = ImageResizeEngine.formatUnitValue(
          ImageResizeEngine.fromPixels(bmp.width, currentUnit, currentDpi),
          currentUnit
        )
        heightInput = ImageResizeEngine.formatUnitValue(
          ImageResizeEngine.fromPixels(bmp.height, currentUnit, currentDpi),
          currentUnit
        )
      }
    }
  }

  // Two-way Unit Synchronizer
  fun changeUnit(newUnit: ResizeUnit) {
    if (newUnit == currentUnit) return
    val wPx = ImageResizeEngine.toPixels(widthInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
    val hPx = ImageResizeEngine.toPixels(heightInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
    currentUnit = newUnit
    widthInput = ImageResizeEngine.formatUnitValue(ImageResizeEngine.fromPixels(wPx, newUnit, currentDpi), newUnit)
    heightInput = ImageResizeEngine.formatUnitValue(ImageResizeEngine.fromPixels(hPx, newUnit, currentDpi), newUnit)
  }

  fun changeDpi(newDpi: Int) {
    currentDpi = newDpi
    customDpiText = newDpi.toString()
    // Re-evaluate pixel inputs if physical unit
    if (currentUnit != ResizeUnit.PIXELS) {
      val wVal = widthInput.toDoubleOrNull() ?: 10.0
      val hVal = heightInput.toDoubleOrNull() ?: 15.0
      widthInput = ImageResizeEngine.formatUnitValue(wVal, currentUnit)
      heightInput = ImageResizeEngine.formatUnitValue(hVal, currentUnit)
    }
  }

  // Handle Dimension Input Changes with Aspect Ratio Lock
  fun onWidthChange(newVal: String) {
    widthInput = newVal
    if (isAspectLocked && originalRatio > 0.001f) {
      val wNum = newVal.toDoubleOrNull()
      if (wNum != null && wNum > 0) {
        val hNum = wNum / originalRatio
        heightInput = ImageResizeEngine.formatUnitValue(hNum, currentUnit)
      }
    }
  }

  fun onHeightChange(newVal: String) {
    heightInput = newVal
    if (isAspectLocked && originalRatio > 0.001f) {
      val hNum = newVal.toDoubleOrNull()
      if (hNum != null && hNum > 0) {
        val wNum = hNum * originalRatio
        widthInput = ImageResizeEngine.formatUnitValue(wNum, currentUnit)
      }
    }
  }

  fun applyPreset(preset: DimensionPreset) {
    currentUnit = preset.unit
    currentDpi = preset.dpi
    customDpiText = preset.dpi.toString()
    widthInput = ImageResizeEngine.formatUnitValue(preset.width, preset.unit)
    heightInput = ImageResizeEngine.formatUnitValue(preset.height, preset.unit)
    val targetPxW = ImageResizeEngine.toPixels(preset.width, preset.unit, preset.dpi)
    val targetPxH = ImageResizeEngine.toPixels(preset.height, preset.unit, preset.dpi)
    originalRatio = targetPxW.toFloat() / targetPxH.toFloat()
    Toast.makeText(context, "Applied preset: ${preset.title}", Toast.LENGTH_SHORT).show()
  }

  fun applyPassportStandard(std: PassportStandard) {
    selectedPassportStandard = std
    currentDpi = std.defaultDpi
    currentUnit = ResizeUnit.MILLIMETERS
    widthInput = ImageResizeEngine.formatUnitValue(std.widthMm, ResizeUnit.MILLIMETERS)
    heightInput = ImageResizeEngine.formatUnitValue(std.heightMm, ResizeUnit.MILLIMETERS)
    originalRatio = (std.widthMm / std.heightMm).toFloat()
  }

  // Regenerate Sheet Preview
  LaunchedEffect(
    selectedTab,
    loadedBitmap,
    photoSheetPaper,
    photoSheetCopies,
    photoSheetSpacingMm,
    photoSheetMarginMm,
    photoSheetIsLandscape,
    photoSheetDrawCutLines
  ) {
    if (selectedTab == 2 && loadedBitmap != null) {
      isGeneratingSheetPreview = true
      val config = PhotoSheetConfig(
        paperSize = photoSheetPaper,
        photoWidthMm = selectedPassportStandard.widthMm,
        photoHeightMm = selectedPassportStandard.heightMm,
        copiesCount = photoSheetCopies,
        spacingMm = photoSheetSpacingMm.toDouble(),
        marginMm = photoSheetMarginMm.toDouble(),
        isLandscape = photoSheetIsLandscape,
        drawCutLines = photoSheetDrawCutLines,
        targetDpi = 150 // Lower DPI for fast real-time preview
      )
      val preview = ImageResizeEngine.generatePassportPhotoSheetBitmap(loadedBitmap!!, config)
      sheetPreviewBitmap = preview
      isGeneratingSheetPreview = false
    }
  }

  // Core Save Action
  fun performSave() {
    val bmp = loadedBitmap
    if (bmp == null) {
      Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
      return
    }

    val targetW = ImageResizeEngine.toPixels(widthInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
    val targetH = ImageResizeEngine.toPixels(heightInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)

    if (targetW <= 0 || targetH <= 0) {
      Toast.makeText(context, "Width and height must be greater than 0", Toast.LENGTH_SHORT).show()
      return
    }

    if (targetW > ImageResizeEngine.MAX_SAFE_DIMENSION || targetH > ImageResizeEngine.MAX_SAFE_DIMENSION) {
      Toast.makeText(context, "Dimensions cannot exceed ${ImageResizeEngine.MAX_SAFE_DIMENSION}px", Toast.LENGTH_LONG).show()
      return
    }

    isProcessingSave = true
    coroutineScope.launch {
      try {
        // Step 1: Apply Passport background if active
        val preparedBmp = if (selectedTab == 1 && passportBgColor != AndroidColor.TRANSPARENT) {
          ImageResizeEngine.applyPassportBackground(bmp, passportBgColor)
        } else {
          bmp
        }

        // Step 2: Resize according to mode
        val resized = ImageResizeEngine.resizeBitmap(
          source = preparedBmp,
          targetWidth = targetW,
          targetHeight = targetH,
          mode = selectedMode,
          backgroundColor = jpegFallbackBgColor
        )

        val qualityVal = if (selectedQualityPreset == QualityPreset.CUSTOM) customQuality else selectedQualityPreset.qualityValue
        val baseFileName = currentMedia?.title?.substringBeforeLast('.') ?: "photo"
        val outFileName = "${baseFileName}_${targetW}x${targetH}${selectedFormat.extension}"

        // Step 3: Save to Storage
        val savedFile = ImageResizeEngine.saveResizedImage(
          context = context,
          bitmap = resized,
          fileName = outFileName,
          format = selectedFormat,
          quality = qualityVal,
          dpi = currentDpi,
          preserveMetadata = preserveMetadata,
          originalUri = currentUri,
          backgroundColorForJpeg = jpegFallbackBgColor
        )

        val result = ResizeExecutionResult(
          originalUri = currentUri.toString(),
          originalTitle = currentMedia?.title ?: "Photo",
          outputUri = Uri.fromFile(savedFile).toString(),
          outputFilePath = savedFile.absolutePath,
          width = targetW,
          height = targetH,
          dpi = currentDpi,
          outputFormat = selectedFormat,
          fileSizeBytes = savedFile.length(),
          isSuccess = true
        )

        executionResult = result
        isProcessingSave = false
      } catch (e: Exception) {
        e.printStackTrace()
        isProcessingSave = false
        Toast.makeText(context, "Error resizing: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  // Save Photo Sheet as PDF
  fun savePhotoSheetAsPdf() {
    val bmp = loadedBitmap ?: return
    isProcessingSave = true
    coroutineScope.launch {
      try {
        val config = PhotoSheetConfig(
          paperSize = photoSheetPaper,
          photoWidthMm = selectedPassportStandard.widthMm,
          photoHeightMm = selectedPassportStandard.heightMm,
          copiesCount = photoSheetCopies,
          spacingMm = photoSheetSpacingMm.toDouble(),
          marginMm = photoSheetMarginMm.toDouble(),
          isLandscape = photoSheetIsLandscape,
          drawCutLines = photoSheetDrawCutLines,
          targetDpi = 300
        )
        val dir = File(context.getExternalFilesDir(null), "pdfs")
        if (!dir.exists()) dir.mkdirs()
        val pdfFile = File(dir, "Passport_Sheet_${System.currentTimeMillis()}.pdf")
        val success = ImageResizeEngine.generatePassportPhotoSheetPdf(context, bmp, config, pdfFile)
        isProcessingSave = false
        if (success) {
          Toast.makeText(context, "Passport Photo Sheet PDF saved successfully!", Toast.LENGTH_LONG).show()
          onCreatePdfFromUri(Uri.fromFile(pdfFile))
        } else {
          Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
        }
      } catch (e: Exception) {
        isProcessingSave = false
        Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  // Save Photo Sheet as Image
  fun savePhotoSheetAsImage() {
    val bmp = loadedBitmap ?: return
    isProcessingSave = true
    coroutineScope.launch {
      try {
        val config = PhotoSheetConfig(
          paperSize = photoSheetPaper,
          photoWidthMm = selectedPassportStandard.widthMm,
          photoHeightMm = selectedPassportStandard.heightMm,
          copiesCount = photoSheetCopies,
          spacingMm = photoSheetSpacingMm.toDouble(),
          marginMm = photoSheetMarginMm.toDouble(),
          isLandscape = photoSheetIsLandscape,
          drawCutLines = photoSheetDrawCutLines,
          targetDpi = 300
        )
        val sheetBmp = ImageResizeEngine.generatePassportPhotoSheetBitmap(bmp, config)
        val fileName = "Passport_Sheet_${photoSheetPaper.name}_300DPI.jpg"
        val savedFile = ImageResizeEngine.saveResizedImage(
          context = context,
          bitmap = sheetBmp,
          fileName = fileName,
          format = OutputFormat.JPEG,
          quality = 95,
          dpi = 300
        )
        isProcessingSave = false
        executionResult = ResizeExecutionResult(
          originalUri = currentUri.toString(),
          originalTitle = "Passport Photo Sheet",
          outputUri = Uri.fromFile(savedFile).toString(),
          outputFilePath = savedFile.absolutePath,
          width = sheetBmp.width,
          height = sheetBmp.height,
          dpi = 300,
          outputFormat = OutputFormat.JPEG,
          fileSizeBytes = savedFile.length()
        )
      } catch (e: Exception) {
        isProcessingSave = false
        Toast.makeText(context, "Error saving sheet: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  // Batch Resize Execution
  fun runBatchResize() {
    if (selectedBatchItems.isEmpty()) {
      Toast.makeText(context, "Select at least 1 image for batch resizing", Toast.LENGTH_SHORT).show()
      return
    }

    val targetW = ImageResizeEngine.toPixels(widthInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
    val targetH = ImageResizeEngine.toPixels(heightInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)

    isBatchProcessing = true
    batchProgressCurrent = 0
    batchProgressTotal = selectedBatchItems.size

    coroutineScope.launch {
      var successCount = 0
      var failureCount = 0
      val qualityVal = if (selectedQualityPreset == QualityPreset.CUSTOM) customQuality else selectedQualityPreset.qualityValue

      for ((index, item) in selectedBatchItems.withIndex()) {
        batchProgressCurrent = index + 1
        try {
          val uri = Uri.parse(item.uri ?: "")
          val rawBmp = ImageResizeEngine.loadSafeBitmap(context, uri, maxTargetDim = 3000)
          if (rawBmp != null) {
            val resized = ImageResizeEngine.resizeBitmap(
              source = rawBmp,
              targetWidth = targetW,
              targetHeight = targetH,
              mode = selectedMode,
              backgroundColor = jpegFallbackBgColor
            )

            val baseName = when (batchRenamePattern) {
              RenamePattern.ORIGINAL_SUFFIX -> "${item.title.substringBeforeLast('.')}_resized"
              RenamePattern.CUSTOM_PREFIX -> "${batchCustomPrefix}_${"%03d".format(index + 1)}"
              RenamePattern.CUSTOM_SUFFIX -> "${item.title.substringBeforeLast('.')}_${targetW}px"
              RenamePattern.SEQUENTIAL -> "photo_${"%03d".format(index + 1)}"
            }
            val fileName = "$baseName${selectedFormat.extension}"

            ImageResizeEngine.saveResizedImage(
              context = context,
              bitmap = resized,
              fileName = fileName,
              format = selectedFormat,
              quality = qualityVal,
              dpi = currentDpi,
              preserveMetadata = preserveMetadata,
              originalUri = uri,
              backgroundColorForJpeg = jpegFallbackBgColor
            )
            successCount++
          } else {
            failureCount++
          }
        } catch (_: Exception) {
          failureCount++
        }
      }

      isBatchProcessing = false
      batchReportMessage = "Batch Complete: $successCount resized successfully" +
        if (failureCount > 0) ", $failureCount items failed." else "."
      Toast.makeText(context, batchReportMessage, Toast.LENGTH_LONG).show()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Image Resize & Studio",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = colors.primaryText
            )
            Text(
              text = "100% Offline • DPI & Dimensions",
              fontSize = 11.sp,
              color = colors.secondaryText
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onClose, modifier = Modifier.testTag("btn_close_resize")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
          }
        },
        actions = {
          // Info button
          IconButton(onClick = { showSpecsDialog = true }, modifier = Modifier.testTag("btn_specs_info")) {
            Icon(Icons.Rounded.Info, contentDescription = "Image Specs", tint = colors.accent)
          }
          // Reset button
          IconButton(
            onClick = {
              if (loadedBitmap != null) {
                currentUnit = ResizeUnit.PIXELS
                currentDpi = 300
                customDpiText = "300"
                widthInput = loadedBitmap!!.width.toString()
                heightInput = loadedBitmap!!.height.toString()
                originalRatio = loadedBitmap!!.width.toFloat() / loadedBitmap!!.height.toFloat()
                Toast.makeText(context, "Reset to original dimensions", Toast.LENGTH_SHORT).show()
              }
            }
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = colors.secondaryText)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.cardBackground)
      )
    },
    containerColor = colors.background
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Tab Bar: Dimensions & DPI, Passport Studio, Print Sheet, Batch
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = colors.cardBackground,
        contentColor = colors.accent
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Dimensions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = {
            selectedTab = 1
            applyPassportStandard(selectedPassportStandard)
          },
          text = { Text("Passport", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.AccountBox, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Photo Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Tab(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          text = { Text("Batch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
          icon = { Icon(Icons.Rounded.DynamicFeed, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // --- 1. Source Image Picker & Recent Carousel ---
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Source Image",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = colors.primaryText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  FilledTonalButton(
                    onClick = {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Picker", fontSize = 12.sp)
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Current Image Info Chip
              if (currentMedia != null && loadedBitmap != null) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(48.dp)
                      .clip(RoundedCornerShape(8.dp))
                  ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                      drawImage(
                        image = loadedBitmap!!.asImageBitmap(),
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = currentMedia?.title ?: "Selected Image",
                      color = colors.primaryText,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "Original: ${originalMetadata.widthPx} × ${originalMetadata.heightPx} px • ${originalMetadata.formattedSize} • ${originalMetadata.formatName}",
                      color = colors.secondaryText,
                      fontSize = 11.sp
                    )
                  }

                  OutlinedButton(
                    onClick = { showCropDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_crop_before_resize")
                  ) {
                    Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crop", fontSize = 11.sp)
                  }
                }
              } else {
                Text(
                  text = "No image selected. Choose from below or tap Picker:",
                  color = colors.secondaryText,
                  fontSize = 12.sp
                )
              }

              // Recent Gallery Photos Row
              if (allGalleryPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "Recent Photos:",
                  color = colors.secondaryText,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(allGalleryPhotos.take(12)) { photo ->
                    val isSelected = currentMedia?.id == photo.id
                    Box(
                      modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                          width = if (isSelected) 2.dp else 1.dp,
                          color = if (isSelected) colors.accent else colors.divider,
                          shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                          currentMedia = photo
                          currentUri = Uri.parse(photo.uri ?: "")
                        }
                    ) {
                      AsyncImage(
                        model = photo.uri,
                        contentDescription = photo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // --- TAB SPECIFIC CONTENT ---

        when (selectedTab) {
          0 -> {
            // TAB 0: Dimensions & DPI Studio
            item {
              DimensionsAndDpiSection(
                currentUnit = currentUnit,
                onUnitSelected = { changeUnit(it) },
                widthInput = widthInput,
                onWidthChange = { onWidthChange(it) },
                heightInput = heightInput,
                onHeightChange = { onHeightChange(it) },
                isAspectLocked = isAspectLocked,
                onAspectLockToggle = { isAspectLocked = it },
                originalRatio = originalRatio,
                currentDpi = currentDpi,
                customDpiText = customDpiText,
                onDpiSelected = { changeDpi(it) },
                onCustomDpiChange = {
                  customDpiText = it
                  it.toIntOrNull()?.let { d -> if (d > 0) changeDpi(d) }
                },
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
                onApplyPreset = { applyPreset(it) }
              )
            }
          }

          1 -> {
            // TAB 1: Dedicated Passport Photo Studio
            item {
              PassportStudioSection(
                selectedStandard = selectedPassportStandard,
                onStandardSelected = { applyPassportStandard(it) },
                currentDpi = currentDpi,
                onDpiChange = { changeDpi(it) },
                bgColor = passportBgColor,
                onBgColorChange = { passportBgColor = it },
                showGuide = showPassportGuide,
                onGuideToggle = { showPassportGuide = it },
                onOpenCrop = { showCropDialog = true }
              )
            }
          }

          2 -> {
            // TAB 2: Multi-copy Print Sheet
            item {
              PhotoSheetSection(
                paperSize = photoSheetPaper,
                onPaperChange = { photoSheetPaper = it },
                copies = photoSheetCopies,
                onCopiesChange = { photoSheetCopies = it },
                spacingMm = photoSheetSpacingMm,
                onSpacingChange = { photoSheetSpacingMm = it },
                marginMm = photoSheetMarginMm,
                onMarginChange = { photoSheetMarginMm = it },
                isLandscape = photoSheetIsLandscape,
                onLandscapeToggle = { photoSheetIsLandscape = it },
                drawCutLines = photoSheetDrawCutLines,
                onCutLinesToggle = { photoSheetDrawCutLines = it },
                previewBitmap = sheetPreviewBitmap,
                isGenerating = isGeneratingSheetPreview,
                onSaveAsPdf = { savePhotoSheetAsPdf() },
                onSaveAsImage = { savePhotoSheetAsImage() }
              )
            }
          }

          3 -> {
            // TAB 3: Batch Resize
            item {
              BatchResizeSection(
                allPhotos = allGalleryPhotos,
                selectedItems = selectedBatchItems,
                onToggleItem = { item ->
                  if (selectedBatchItems.contains(item)) selectedBatchItems.remove(item)
                  else selectedBatchItems.add(item)
                },
                onSelectAll = {
                  selectedBatchItems.clear()
                  selectedBatchItems.addAll(allGalleryPhotos)
                },
                onClearSelection = { selectedBatchItems.clear() },
                renamePattern = batchRenamePattern,
                onPatternChange = { batchRenamePattern = it },
                customPrefix = batchCustomPrefix,
                onCustomPrefixChange = { batchCustomPrefix = it },
                isProcessing = isBatchProcessing,
                currentProgress = batchProgressCurrent,
                totalProgress = batchProgressTotal,
                onRunBatch = { runBatchResize() }
              )
            }
          }
        }

        // --- Output Settings Card (for Tab 0 & 1) ---
        if (selectedTab in listOf(0, 1)) {
          item {
            OutputSettingsCard(
              format = selectedFormat,
              onFormatChange = { selectedFormat = it },
              qualityPreset = selectedQualityPreset,
              onQualityPresetChange = { selectedQualityPreset = it },
              customQuality = customQuality,
              onCustomQualityChange = { customQuality = it },
              preserveMetadata = preserveMetadata,
              onPreserveMetadataToggle = { preserveMetadata = it },
              fallbackBgColor = jpegFallbackBgColor,
              onFallbackBgColorChange = { jpegFallbackBgColor = it },
              hasAlpha = originalMetadata.hasAlpha,
              targetWidthPx = ImageResizeEngine.toPixels(widthInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi),
              targetHeightPx = ImageResizeEngine.toPixels(heightInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi),
              currentDpi = currentDpi
            )
          }

          // Main Action Button
          item {
            Button(
              onClick = { performSave() },
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_save_resized_main"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              enabled = !isProcessingSave && loadedBitmap != null
            ) {
              if (isProcessingSave) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing Resized Copy...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
              } else {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Save Resized Copy (New File)",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }

  // --- Modals & Sheets ---

  // 1. Crop Before Resize Modal
  if (showCropDialog && loadedBitmap != null) {
    CropBeforeResizeDialog(
      sourceBitmap = loadedBitmap!!,
      onCropApplied = { cropped ->
        loadedBitmap = cropped
        originalRatio = cropped.width.toFloat() / cropped.height.toFloat()
        widthInput = ImageResizeEngine.formatUnitValue(
          ImageResizeEngine.fromPixels(cropped.width, currentUnit, currentDpi),
          currentUnit
        )
        heightInput = ImageResizeEngine.formatUnitValue(
          ImageResizeEngine.fromPixels(cropped.height, currentUnit, currentDpi),
          currentUnit
        )
        showCropDialog = false
        Toast.makeText(context, "Crop applied to session", Toast.LENGTH_SHORT).show()
      },
      onDismiss = { showCropDialog = false }
    )
  }

  // 2. Full Image Specs Comparison Dialog
  if (showSpecsDialog) {
    val targetWPx = ImageResizeEngine.toPixels(widthInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
    val targetHPx = ImageResizeEngine.toPixels(heightInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
    val qualityVal = if (selectedQualityPreset == QualityPreset.CUSTOM) customQuality else selectedQualityPreset.qualityValue
    val estSize = ImageResizeEngine.estimateFileSize(targetWPx, targetHPx, selectedFormat, qualityVal)

    ImageInfoComparisonDialog(
      originalInfo = originalMetadata,
      outputWidth = targetWPx,
      outputHeight = targetHPx,
      outputDpi = currentDpi,
      outputFormat = selectedFormat,
      estimatedSizeBytes = estSize,
      onDismiss = { showSpecsDialog = false }
    )
  }

  // 3. Post-Save Success Bottom Sheet
  executionResult?.let { res ->
    ResizeSuccessSheet(
      result = res,
      onDismiss = { executionResult = null },
      onOpenInViewer = { uri ->
        executionResult = null
        onOpenInViewer(uri)
      },
      onOpenInProEditor = { uri ->
        executionResult = null
        onOpenInProEditor(uri)
      },
      onCreatePdf = { uri ->
        executionResult = null
        onCreatePdfFromUri(uri)
      },
      onResizeAgain = {
        executionResult = null
      }
    )
  }
}

// ---------------------------------------------------------
// COMPONENT 1: Dimensions & DPI Configuration Section
// ---------------------------------------------------------
@Composable
private fun DimensionsAndDpiSection(
  currentUnit: ResizeUnit,
  onUnitSelected: (ResizeUnit) -> Unit,
  widthInput: String,
  onWidthChange: (String) -> Unit,
  heightInput: String,
  onHeightChange: (String) -> Unit,
  isAspectLocked: Boolean,
  onAspectLockToggle: (Boolean) -> Unit,
  originalRatio: Float,
  currentDpi: Int,
  customDpiText: String,
  onDpiSelected: (Int) -> Unit,
  onCustomDpiChange: (String) -> Unit,
  selectedMode: ResizeMode,
  onModeSelected: (ResizeMode) -> Unit,
  onApplyPreset: (DimensionPreset) -> Unit
) {
  val colors = GalleryTheme.colors
  var selectedCategory by remember { mutableStateOf<DimensionCategory?>(null) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      // 1. Units Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Measurement Unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          ResizeUnit.values().forEach { unit ->
            val isSelected = currentUnit == unit
            FilterChip(
              selected = isSelected,
              onClick = { onUnitSelected(unit) },
              label = { Text(unit.symbol, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                selectedLabelColor = colors.accent
              )
            )
          }
        }
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // 2. Width & Height Inputs with Aspect Ratio Lock
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = widthInput,
          onValueChange = onWidthChange,
          label = { Text("Width (${currentUnit.symbol})") },
          modifier = Modifier.weight(1f).testTag("input_resize_width"),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
          shape = RoundedCornerShape(12.dp)
        )

        IconButton(
          onClick = { onAspectLockToggle(!isAspectLocked) },
          modifier = Modifier.padding(top = 4.dp).testTag("btn_toggle_aspect_lock")
        ) {
          Icon(
            imageVector = if (isAspectLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            contentDescription = "Lock Ratio",
            tint = if (isAspectLocked) colors.accent else colors.secondaryText
          )
        }

        OutlinedTextField(
          value = heightInput,
          onValueChange = onHeightChange,
          label = { Text("Height (${currentUnit.symbol})") },
          modifier = Modifier.weight(1f).testTag("input_resize_height"),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
          shape = RoundedCornerShape(12.dp)
        )
      }

      // Live Aspect Ratio Notice & Equivalent Px calculation
      val wPx = ImageResizeEngine.toPixels(widthInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
      val hPx = ImageResizeEngine.toPixels(heightInput.toDoubleOrNull() ?: 1080.0, currentUnit, currentDpi)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (isAspectLocked) "Aspect Ratio Locked (≈ %.2f)".format(originalRatio) else "Aspect Ratio Unlocked",
          color = if (isAspectLocked) colors.accent else colors.secondaryText,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "Equals: $wPx × $hPx px",
          color = colors.primaryText,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // 3. DPI / PPI Section
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Print DPI / PPI", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
          Text("Default Print: 300 DPI", fontSize = 11.sp, color = colors.accent)
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ResizePresets.DPI_PRESETS.forEach { dpi ->
            val isSelected = currentDpi == dpi
            FilterChip(
              selected = isSelected,
              onClick = { onDpiSelected(dpi) },
              label = { Text("$dpi DPI", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                selectedLabelColor = colors.accent
              )
            )
          }
        }

        OutlinedTextField(
          value = customDpiText,
          onValueChange = onCustomDpiChange,
          label = { Text("Custom DPI / PPI") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(12.dp)
        )

        Text(
          text = "Note: DPI controls physical print size. Changing DPI metadata alone does not alter pixel detail unless dimensions are resampled.",
          color = colors.secondaryText,
          fontSize = 10.sp
        )
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // 4. Resize Modes
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Resize Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)

        ResizeMode.values().forEach { mode ->
          val isSelected = selectedMode == mode
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.surface)
              .border(
                1.dp,
                if (isSelected) colors.accent else Color.Transparent,
                RoundedCornerShape(10.dp)
              )
              .clickable { onModeSelected(mode) }
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = mode.label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.accent else colors.primaryText
              )
              Text(
                text = mode.description,
                fontSize = 10.sp,
                color = colors.secondaryText
              )
            }
          }
        }
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // 5. Dimension Presets
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Common Presets", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = selectedCategory == null,
            onClick = { selectedCategory = null },
            label = { Text("All", fontSize = 11.sp) }
          )
          DimensionCategory.values().forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.label, fontSize = 11.sp) }
            )
          }
        }

        val filteredPresets = if (selectedCategory == null) {
          ResizePresets.ALL_PRESETS
        } else {
          ResizePresets.ALL_PRESETS.filter { it.category == selectedCategory }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          filteredPresets.forEach { preset ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onApplyPreset(preset) },
              colors = CardDefaults.cardColors(containerColor = colors.surface),
              shape = RoundedCornerShape(10.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(preset.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                  Text(preset.description, fontSize = 10.sp, color = colors.secondaryText)
                }
                Text(
                  text = "${preset.width} × ${preset.height} ${preset.unit.symbol}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = colors.accent
                )
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------
// COMPONENT 2: Passport Photo Studio Section
// ---------------------------------------------------------
@Composable
private fun PassportStudioSection(
  selectedStandard: PassportStandard,
  onStandardSelected: (PassportStandard) -> Unit,
  currentDpi: Int,
  onDpiChange: (Int) -> Unit,
  bgColor: Int,
  onBgColorChange: (Int) -> Unit,
  showGuide: Boolean,
  onGuideToggle: (Boolean) -> Unit,
  onOpenCrop: () -> Unit
) {
  val colors = GalleryTheme.colors

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.AccountBox, contentDescription = null, tint = colors.accent)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Passport & Visa Photo Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
      }

      // Government/Visa Disclaimer Notice (REQUIRED)
      Surface(
        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Verify the required photo dimensions and background rules for your specific application authority or consulate.",
            color = colors.primaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }
      }

      // Passport Standards Selector
      Text("Country / Biometric Standard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ResizePresets.PASSPORT_STANDARDS.forEach { std ->
          val isSelected = selectedStandard.id == std.id
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.surface)
              .border(1.dp, if (isSelected) colors.accent else Color.Transparent, RoundedCornerShape(10.dp))
              .clickable { onStandardSelected(std) }
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = std.countryOrStandard,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) colors.accent else colors.primaryText
              )
              Text(text = std.popularUse, fontSize = 10.sp, color = colors.secondaryText)
              Text(text = "${std.faceCoverageNote} • ${std.recommendedBg}", fontSize = 9.sp, color = colors.accent)
            }
            val (pxW, pxH) = std.getPixels(currentDpi)
            Text(
              text = "$pxW × $pxH px\n@ $currentDpi DPI",
              fontSize = 10.sp,
              textAlign = TextAlign.End,
              fontWeight = FontWeight.Bold,
              color = colors.primaryText
            )
          }
        }
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // Face Guide Overlay Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Biometric Face Guide Overlay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
          Text("Displays head oval, eye line and chin alignment markings", fontSize = 10.sp, color = colors.secondaryText)
        }
        Switch(
          checked = showGuide,
          onCheckedChange = onGuideToggle,
          colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
        )
      }

      // Background Color Options
      Text("Background Preparation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val bgOptions = listOf(
          Pair("White", AndroidColor.WHITE),
          Pair("Light Blue", AndroidColor.rgb(224, 242, 254)),
          Pair("Light Gray", AndroidColor.rgb(243, 244, 246)),
          Pair("Original", AndroidColor.TRANSPARENT)
        )
        bgOptions.forEach { (label, col) ->
          val isSel = bgColor == col
          FilterChip(
            selected = isSel,
            onClick = { onBgColorChange(col) },
            label = { Text(label, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.accent.copy(alpha = 0.2f),
              selectedLabelColor = colors.accent
            )
          )
        }
      }

      // Crop & Align Button
      FilledTonalButton(
        onClick = onOpenCrop,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Rounded.Crop, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Crop & Align Face for ${selectedStandard.countryOrStandard}")
      }
    }
  }
}

// ---------------------------------------------------------
// COMPONENT 3: Photo Sheet Section (Print Grid)
// ---------------------------------------------------------
@Composable
private fun PhotoSheetSection(
  paperSize: PhotoSheetPaperSize,
  onPaperChange: (PhotoSheetPaperSize) -> Unit,
  copies: Int,
  onCopiesChange: (Int) -> Unit,
  spacingMm: Float,
  onSpacingChange: (Float) -> Unit,
  marginMm: Float,
  onMarginChange: (Float) -> Unit,
  isLandscape: Boolean,
  onLandscapeToggle: (Boolean) -> Unit,
  drawCutLines: Boolean,
  onCutLinesToggle: (Boolean) -> Unit,
  previewBitmap: Bitmap?,
  isGenerating: Boolean,
  onSaveAsPdf: () -> Unit,
  onSaveAsImage: () -> Unit
) {
  val colors = GalleryTheme.colors

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Print, contentDescription = null, tint = colors.accent)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Multi-Copy Print Sheet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
      }

      // Paper Size Selection
      Text("Paper Size", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        PhotoSheetPaperSize.values().forEach { paper ->
          val isSelected = paperSize == paper
          FilterChip(
            selected = isSelected,
            onClick = { onPaperChange(paper) },
            label = { Text(paper.displayName, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.accent.copy(alpha = 0.2f),
              selectedLabelColor = colors.accent
            )
          )
        }
      }

      // Copies Count Selection
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Number of Copies: $copies", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf(2, 4, 6, 8, 12, 16, 24).forEach { count ->
            val isSelected = copies == count
            FilterChip(
              selected = isSelected,
              onClick = { onCopiesChange(count) },
              label = { Text("$count", fontSize = 11.sp) }
            )
          }
        }
      }

      // Sheet Margins & Spacing Sliders
      Column {
        Text("Photo Spacing: ${"%.1f".format(spacingMm)} mm", fontSize = 11.sp, color = colors.secondaryText)
        Slider(
          value = spacingMm,
          onValueChange = onSpacingChange,
          valueRange = 0f..10f,
          colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
        )
      }

      Column {
        Text("Page Margins: ${"%.1f".format(marginMm)} mm", fontSize = 11.sp, color = colors.secondaryText)
        Slider(
          value = marginMm,
          onValueChange = onMarginChange,
          valueRange = 4f..25f,
          colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
        )
      }

      // Options: Landscape & Cut lines
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Draw Cut Lines (Guides)", fontSize = 12.sp, color = colors.primaryText)
        Switch(checked = drawCutLines, onCheckedChange = onCutLinesToggle)
      }

      // Live Sheet Preview Box
      Text("Sheet Preview (300 DPI Layout)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color.DarkGray)
          .border(1.dp, colors.divider, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (isGenerating) {
          CircularProgressIndicator(color = colors.accent)
        } else if (previewBitmap != null) {
          Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val aspect = previewBitmap.width.toFloat() / previewBitmap.height.toFloat()
            val boxAspect = size.width / size.height
            val drawW: Float
            val drawH: Float
            if (aspect > boxAspect) {
              drawW = size.width
              drawH = drawW / aspect
            } else {
              drawH = size.height
              drawW = drawH * aspect
            }
            val left = (size.width - drawW) / 2f
            val top = (size.height - drawH) / 2f
            drawImage(
              image = previewBitmap.asImageBitmap(),
              dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
              dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
            )
          }
        } else {
          Text("Select photo to preview sheet", color = Color.LightGray, fontSize = 12.sp)
        }
      }

      // Action Buttons: Save as PDF & Save as Image
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = onSaveAsPdf,
          modifier = Modifier.weight(1f).testTag("btn_save_sheet_pdf"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save as PDF", fontSize = 12.sp)
        }

        Button(
          onClick = onSaveAsImage,
          modifier = Modifier.weight(1f).testTag("btn_save_sheet_image"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) {
          Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save as Image", fontSize = 12.sp)
        }
      }
    }
  }
}

// ---------------------------------------------------------
// COMPONENT 4: Batch Resize Section
// ---------------------------------------------------------
@Composable
private fun BatchResizeSection(
  allPhotos: List<MediaItem>,
  selectedItems: List<MediaItem>,
  onToggleItem: (MediaItem) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  renamePattern: RenamePattern,
  onPatternChange: (RenamePattern) -> Unit,
  customPrefix: String,
  onCustomPrefixChange: (String) -> Unit,
  isProcessing: Boolean,
  currentProgress: Int,
  totalProgress: Int,
  onRunBatch: () -> Unit
) {
  val colors = GalleryTheme.colors

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Batch Image Resize", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
          Text("${selectedItems.size} selected of ${allPhotos.size} photos", fontSize = 11.sp, color = colors.secondaryText)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(onClick = onSelectAll) { Text("Select All", fontSize = 12.sp) }
          TextButton(onClick = onClearSelection) { Text("Clear", fontSize = 12.sp) }
        }
      }

      // Photos Selection Grid / Row
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(allPhotos) { photo ->
          val isSelected = selectedItems.contains(photo)
          Box(
            modifier = Modifier
              .size(70.dp)
              .clip(RoundedCornerShape(10.dp))
              .border(
                2.dp,
                if (isSelected) colors.accent else Color.Transparent,
                RoundedCornerShape(10.dp)
              )
              .clickable { onToggleItem(photo) }
          ) {
            AsyncImage(
              model = photo.uri,
              contentDescription = photo.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            if (isSelected) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(colors.accent),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // Batch Rename Strategy
      Text("Batch File Naming", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RenamePattern.values().forEach { pat ->
          val isSel = renamePattern == pat
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSel) colors.accent.copy(alpha = 0.12f) else colors.surface)
              .clickable { onPatternChange(pat) }
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(pat.label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = colors.primaryText)
              Text("Example: ${pat.example}", fontSize = 10.sp, color = colors.secondaryText)
            }
          }
        }
      }

      if (renamePattern == RenamePattern.CUSTOM_PREFIX) {
        OutlinedTextField(
          value = customPrefix,
          onValueChange = onCustomPrefixChange,
          label = { Text("Prefix text") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
      }

      // Progress bar if running
      if (isProcessing) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Processing $currentProgress of $totalProgress images...", fontSize = 12.sp, color = colors.accent)
          LinearProgressIndicator(
            progress = { if (totalProgress > 0) currentProgress.toFloat() / totalProgress else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = colors.accent
          )
        }
      }

      Button(
        onClick = onRunBatch,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_run_batch_resize"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
        enabled = !isProcessing && selectedItems.isNotEmpty()
      ) {
        Icon(Icons.Rounded.DynamicFeed, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Batch Resize (${selectedItems.size} Images)")
      }
    }
  }
}

// ---------------------------------------------------------
// COMPONENT 5: Output Settings Card (Format, Quality, Exif)
// ---------------------------------------------------------
@Composable
private fun OutputSettingsCard(
  format: OutputFormat,
  onFormatChange: (OutputFormat) -> Unit,
  qualityPreset: QualityPreset,
  onQualityPresetChange: (QualityPreset) -> Unit,
  customQuality: Int,
  onCustomQualityChange: (Int) -> Unit,
  preserveMetadata: Boolean,
  onPreserveMetadataToggle: (Boolean) -> Unit,
  fallbackBgColor: Int,
  onFallbackBgColorChange: (Int) -> Unit,
  hasAlpha: Boolean,
  targetWidthPx: Int,
  targetHeightPx: Int,
  currentDpi: Int
) {
  val colors = GalleryTheme.colors
  val effectiveQuality = if (qualityPreset == QualityPreset.CUSTOM) customQuality else qualityPreset.qualityValue
  val estimatedSizeBytes = ImageResizeEngine.estimateFileSize(targetWidthPx, targetHeightPx, format, effectiveQuality)

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("Output Format & Quality", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primaryText)

      // Format Choice: JPG, PNG, WebP
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutputFormat.values().forEach { fmt ->
          val isSelected = format == fmt
          FilterChip(
            selected = isSelected,
            onClick = { onFormatChange(fmt) },
            label = { Text(fmt.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.accent.copy(alpha = 0.2f),
              selectedLabelColor = colors.accent
            ),
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Transparency warning for JPEG
      if (format == OutputFormat.JPEG && hasAlpha) {
        Surface(
          color = colors.warning.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "JPEG does not support transparency. Transparent areas will need a background color.",
              fontSize = 11.sp,
              color = colors.primaryText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(
                Pair("White Canvas", AndroidColor.WHITE),
                Pair("Black Canvas", AndroidColor.BLACK)
              ).forEach { (lbl, col) ->
                FilterChip(
                  selected = fallbackBgColor == col,
                  onClick = { onFallbackBgColorChange(col) },
                  label = { Text(lbl, fontSize = 10.sp) }
                )
              }
            }
          }
        }
      }

      // Quality Selector (for JPEG and WebP)
      if (format.supportsQualitySetting) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Compression Quality", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
            Text("$effectiveQuality%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            QualityPreset.values().forEach { qPreset ->
              val isSel = qualityPreset == qPreset
              FilterChip(
                selected = isSel,
                onClick = { onQualityPresetChange(qPreset) },
                label = { Text(qPreset.label, fontSize = 10.sp) }
              )
            }
          }

          if (qualityPreset == QualityPreset.CUSTOM) {
            Slider(
              value = customQuality.toFloat(),
              onValueChange = { onCustomQualityChange(it.roundToInt()) },
              valueRange = 10f..100f,
              colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
            )
          }
        }
      } else {
        Text("PNG provides lossless output without compression artifacts.", fontSize = 11.sp, color = colors.secondaryText)
      }

      Divider(color = colors.divider.copy(alpha = 0.5f))

      // Estimated Output File Size Preview
      val estKb = estimatedSizeBytes / 1024.0
      val estMb = estKb / 1024.0
      val estDisplay = if (estMb >= 1.0) "≈ %.2f MB".format(estMb) else "≈ %.1f KB".format(estKb)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(colors.surface)
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Estimated Output Size:", fontSize = 11.sp, color = colors.secondaryText)
          Text("Approximation only. Exact size varies with content detail.", fontSize = 9.sp, color = colors.secondaryText)
        }
        Text(
          text = estDisplay,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = colors.accent
        )
      }

      // Metadata Preservation Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Preserve EXIF Metadata", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
          Text("Keep camera info, date & orientation (disable for privacy)", fontSize = 10.sp, color = colors.secondaryText)
        }
        Switch(
          checked = preserveMetadata,
          onCheckedChange = onPreserveMetadataToggle,
          colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
        )
      }
    }
  }
}
