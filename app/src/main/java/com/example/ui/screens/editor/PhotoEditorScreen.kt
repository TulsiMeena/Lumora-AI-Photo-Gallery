package com.example.ui.screens.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.editor.PhotoEditorEngine
import com.example.model.editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoEditorScreen(
  sourceUri: Uri,
  mediaId: String,
  title: String = "Photo",
  initialStateJson: String? = null,
  onSaveExport: (Uri, String, PhotoEditorState) -> Unit,
  onSaveProject: (PhotoEditorState) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var previewBaseBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoadingImage by remember { mutableStateOf(true) }
  var isExporting by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }

  // Editor state & history stacks
  var editorState by remember {
    mutableStateOf(
      if (!initialStateJson.isNullOrBlank()) {
        PhotoEditorEngine.deserializeState(initialStateJson).copy(originalMediaId = mediaId, originalUri = sourceUri.toString(), title = title)
      } else {
        PhotoEditorState(originalMediaId = mediaId, originalUri = sourceUri.toString(), title = title)
      }
    )
  }

  val undoStack = remember { mutableStateListOf<PhotoEditorState>() }
  val redoStack = remember { mutableStateListOf<PhotoEditorState>() }
  var lastCommittedState by remember { mutableStateOf(editorState) }

  // Retouch radius & Drawing controls
  var retouchRadius by remember { mutableFloatStateOf(30f) }
  var drawingColor by remember { mutableLongStateOf(0xFFFF3B30) }
  var drawingStrokeWidth by remember { mutableFloatStateOf(10f) }
  var activeStrokePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

  // Push distinct state snapshot to undo stack (capped at 30 to prevent memory pressure)
  val pushStateSnapshot: (PhotoEditorState) -> Unit = { newState ->
    if (editorState != newState) {
      if (undoStack.size >= 30) {
        undoStack.removeAt(0)
      }
      undoStack.add(editorState)
      redoStack.clear()
      lastCommittedState = newState
      editorState = newState
    }
  }

  // Live slider change without clogging undo stack during continuous drag:
  val updateStateLive: (PhotoEditorState) -> Unit = { newState ->
    editorState = newState
  }

  // Debounced history snapshot for slider changes (captures final slider position)
  LaunchedEffect(editorState) {
    delay(400)
    if (editorState != lastCommittedState) {
      if (undoStack.size >= 30) {
        undoStack.removeAt(0)
      }
      undoStack.add(lastCommittedState)
      redoStack.clear()
      lastCommittedState = editorState
    }
  }

  // Load Bitmap on entry with fast downsampled preview base for 120fps editing
  LaunchedEffect(sourceUri) {
    isLoadingImage = true
    withContext(Dispatchers.IO) {
      val fullBitmap = PhotoEditorEngine.loadBitmap(context, sourceUri, maxDimension = 2048)
      val smallPreview = fullBitmap?.let {
        val maxPre = 960
        if (it.width > maxPre || it.height > maxPre) {
          val s = if (it.width > it.height) maxPre.toFloat() / it.width else maxPre.toFloat() / it.height
          Bitmap.createScaledBitmap(it, (it.width * s).toInt(), (it.height * s).toInt(), true)
        } else {
          it
        }
      }
      withContext(Dispatchers.Main) {
        originalBitmap = fullBitmap
        previewBaseBitmap = smallPreview
        previewBitmap = smallPreview
        isLoadingImage = false
      }
    }
  }

  // Smooth background rendering for heavy non-ColorMatrix adjustments (Curves, HSL, Retouch, Detail, Vignette)
  LaunchedEffect(
    editorState.curvesAdjustment,
    editorState.hslAdjustments,
    editorState.detailAdjustments,
    editorState.vignetteEffects,
    editorState.retouchItems,
    previewBaseBitmap
  ) {
    val base = previewBaseBitmap ?: return@LaunchedEffect
    val hasComplexFilters = !editorState.curvesAdjustment.isDefault ||
        !editorState.hslAdjustments.isDefault ||
        !editorState.detailAdjustments.isDefault ||
        !editorState.vignetteEffects.isDefault ||
        editorState.retouchItems.isNotEmpty()

    if (!hasComplexFilters) {
      previewBitmap = base
      return@LaunchedEffect
    }

    delay(100) // 100ms debounce ensures zero lag while interacting
    withContext(Dispatchers.Default) {
      val processed = PhotoEditorEngine.processImage(base, editorState)
      withContext(Dispatchers.Main) {
        previewBitmap = processed
      }
    }
  }

  BackHandler {
    if (editorState.hasModifications) {
      onSaveProject(editorState)
    }
    onClose()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F1117))
  ) {
    if (isLoadingImage) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = Color(0xFF3B82F6))
          Spacer(modifier = Modifier.height(12.dp))
          Text("Loading photo in full resolution...", color = Color(0xFFCBD5E1))
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top Bar with Undo / Redo / Compare / Export
        EditorTopBar(
          title = editorState.title,
          canUndo = undoStack.isNotEmpty(),
          canRedo = redoStack.isNotEmpty(),
          compareMode = editorState.compareMode,
          onUndo = {
            if (undoStack.isNotEmpty()) {
              val prev = undoStack.removeAt(undoStack.lastIndex)
              redoStack.add(editorState)
              lastCommittedState = prev
              editorState = prev
            }
          },
          onRedo = {
            if (redoStack.isNotEmpty()) {
              val next = redoStack.removeAt(redoStack.lastIndex)
              undoStack.add(editorState)
              lastCommittedState = next
              editorState = next
            }
          },
          onCompareHold = { isHolding ->
            editorState = editorState.copy(
              compareMode = if (isHolding) CompareMode.HOLD_BEFORE else CompareMode.NONE
            )
          },
          onCompareModeCycle = {
            val nextMode = when (editorState.compareMode) {
              CompareMode.NONE -> CompareMode.SPLIT_SLIDER
              CompareMode.SPLIT_SLIDER -> CompareMode.NONE
              else -> CompareMode.NONE
            }
            editorState = editorState.copy(compareMode = nextMode)
          },
          onSaveExport = { showExportDialog = true },
          onClose = {
            if (editorState.hasModifications) {
              onSaveProject(editorState)
            }
            onClose()
          }
        )

        // 2. Interactive Ultra-Smooth Editing Canvas
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          EditorCanvas(
            originalBitmap = originalBitmap,
            previewBitmap = previewBitmap,
            state = editorState,
            activeStrokePoints = activeStrokePoints,
            activeStrokeColor = drawingColor,
            activeStrokeWidth = drawingStrokeWidth,
            onUpdateState = updateStateLive,
            onAddDrawingPoint = { nx, ny ->
              activeStrokePoints = activeStrokePoints + DrawingPoint(nx, ny)
            },
            onEndDrawingStroke = {
              if (activeStrokePoints.size >= 2) {
                val newStroke = DrawingStroke(
                  points = activeStrokePoints,
                  colorHex = drawingColor,
                  strokeWidth = drawingStrokeWidth
                )
                pushStateSnapshot(editorState.copy(drawingStrokes = editorState.drawingStrokes + newStroke))
              }
              activeStrokePoints = emptyList()
            },
            onAddRetouchPoint = { nx, ny ->
              val newItem = RetouchItem(
                targetX = nx,
                targetY = ny,
                sourceX = (nx - 0.05f).coerceIn(0f, 1f),
                sourceY = (ny - 0.05f).coerceIn(0f, 1f),
                radius = retouchRadius
              )
              pushStateSnapshot(editorState.copy(retouchItems = editorState.retouchItems + newItem))
            },
            onMoveSelectedText = { id, nx, ny ->
              val updated = editorState.textLayers.map {
                if (it.id == id) it.copy(normalizedX = nx, normalizedY = ny) else it
              }
              editorState = editorState.copy(textLayers = updated)
            },
            onMoveSelectedShape = { id, nx, ny ->
              val updated = editorState.shapes.map {
                if (it.id == id) it.copy(normalizedX = nx, normalizedY = ny) else it
              }
              editorState = editorState.copy(shapes = updated)
            }
          )
        }

        // 3. Bottom Tool Panels & Category Tabs
        EditorBottomBar(
          state = editorState,
          onSelectCategory = { cat ->
            editorState = editorState.copy(activeCategory = cat)
          },
          onUpdateState = updateStateLive,
          retouchRadius = retouchRadius,
          onRetouchRadiusChange = { retouchRadius = it },
          onClearRetouch = {
            pushStateSnapshot(editorState.copy(retouchItems = emptyList()))
          },
          drawingColor = drawingColor,
          drawingStrokeWidth = drawingStrokeWidth,
          onDrawingColorChange = { drawingColor = it },
          onDrawingStrokeWidthChange = { drawingStrokeWidth = it },
          onUndoDrawingStroke = {
            if (editorState.drawingStrokes.isNotEmpty()) {
              pushStateSnapshot(editorState.copy(drawingStrokes = editorState.drawingStrokes.dropLast(1)))
            }
          },
          onClearDrawing = {
            pushStateSnapshot(editorState.copy(drawingStrokes = emptyList()))
          },
          canUndoDrawing = editorState.drawingStrokes.isNotEmpty(),
          onAddShape = { shapeType ->
            val newShape = ShapeStickerItem(type = shapeType)
            pushStateSnapshot(editorState.copy(shapes = editorState.shapes + newShape))
          },
          onAddSticker = { emoji ->
            val newSticker = ShapeStickerItem(type = EditorShapeType.STICKER, stickerSymbol = emoji)
            pushStateSnapshot(editorState.copy(shapes = editorState.shapes + newSticker))
          },
          onClearShapes = {
            pushStateSnapshot(editorState.copy(shapes = emptyList()))
          },
          onAddText = { text ->
            val newText = TextLayerItem(text = text)
            pushStateSnapshot(editorState.copy(textLayers = editorState.textLayers + newText))
          },
          onDeleteText = { id ->
            pushStateSnapshot(editorState.copy(textLayers = editorState.textLayers.filterNot { it.id == id }))
          },
          onAddPrivacyRegion = { type ->
            val newRegion = BlurMosaicRegion(type = type)
            pushStateSnapshot(editorState.copy(blurMosaicRegions = editorState.blurMosaicRegions + newRegion))
          },
          onClearPrivacyRegions = {
            pushStateSnapshot(editorState.copy(blurMosaicRegions = emptyList()))
          },
          imageAspectRatio = previewBitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
        )
      }
    }

    // 4. Export Dialog
    if (showExportDialog) {
      ExportDialog(
        isExporting = isExporting,
        onDismiss = { showExportDialog = false },
        onConfirmExport = { format, quality ->
          val srcBmp = originalBitmap ?: return@ExportDialog
          isExporting = true
          scope.launch {
            val (savedUri, savedPath) = PhotoEditorEngine.exportImage(
              context = context,
              sourceBitmap = srcBmp,
              state = editorState,
              format = format,
              qualityPercent = quality
            )
            isExporting = false
            showExportDialog = false
            if (savedUri != null && savedPath != null) {
              onSaveExport(savedUri, savedPath, editorState)
            }
          }
        }
      )
    }
  }
}
