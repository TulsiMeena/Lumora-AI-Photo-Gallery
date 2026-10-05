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
  onOpenAiEditor: ((Uri, String) -> Unit)? = null,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
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

  // Retouch radius & Drawing controls
  var retouchRadius by remember { mutableFloatStateOf(30f) }
  var drawingColor by remember { mutableLongStateOf(0xFFFF3B30) }
  var drawingStrokeWidth by remember { mutableFloatStateOf(10f) }
  var activeStrokePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

  // Push state snapshot to undo stack
  val pushState: (PhotoEditorState) -> Unit = { newState ->
    undoStack.add(editorState)
    redoStack.clear()
    editorState = newState
  }

  // Load Bitmap on entry
  LaunchedEffect(sourceUri) {
    isLoadingImage = true
    withContext(Dispatchers.IO) {
      val fullBitmap = PhotoEditorEngine.loadBitmap(context, sourceUri, maxDimension = 2048)
      val smallPreview = fullBitmap?.let {
        val maxPre = 1080
        if (it.width > maxPre || it.height > maxPre) {
          val s = if (it.width > it.height) maxPre.toFloat() / it.width else maxPre.toFloat() / it.height
          Bitmap.createScaledBitmap(it, (it.width * s).toInt(), (it.height * s).toInt(), true)
        } else {
          it
        }
      }
      withContext(Dispatchers.Main) {
        originalBitmap = fullBitmap
        previewBitmap = smallPreview
        isLoadingImage = false
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
        // 1. Top Bar
        EditorTopBar(
          title = editorState.title,
          canUndo = undoStack.isNotEmpty(),
          canRedo = redoStack.isNotEmpty(),
          compareMode = editorState.compareMode,
          onUndo = {
            if (undoStack.isNotEmpty()) {
              val prev = undoStack.removeAt(undoStack.lastIndex)
              redoStack.add(editorState)
              editorState = prev
            }
          },
          onRedo = {
            if (redoStack.isNotEmpty()) {
              val next = redoStack.removeAt(redoStack.lastIndex)
              undoStack.add(editorState)
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
          onOpenAiEditor = {
            onOpenAiEditor?.invoke(sourceUri, title)
          },
          onSaveExport = { showExportDialog = true },
          onClose = {
            if (editorState.hasModifications) {
              onSaveProject(editorState)
            }
            onClose()
          }
        )

        // 2. Interactive Editing Canvas
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          EditorCanvas(
            originalBitmap = originalBitmap,
            previewBitmap = previewBitmap,
            state = editorState,
            onUpdateState = { pushState(it) },
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
                pushState(editorState.copy(drawingStrokes = editorState.drawingStrokes + newStroke))
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
              pushState(editorState.copy(retouchItems = editorState.retouchItems + newItem))
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
          onUpdateState = { pushState(it) },
          retouchRadius = retouchRadius,
          onRetouchRadiusChange = { retouchRadius = it },
          onClearRetouch = {
            pushState(editorState.copy(retouchItems = emptyList()))
          },
          drawingColor = drawingColor,
          drawingStrokeWidth = drawingStrokeWidth,
          onDrawingColorChange = { drawingColor = it },
          onDrawingStrokeWidthChange = { drawingStrokeWidth = it },
          onUndoDrawingStroke = {
            if (editorState.drawingStrokes.isNotEmpty()) {
              pushState(editorState.copy(drawingStrokes = editorState.drawingStrokes.dropLast(1)))
            }
          },
          onClearDrawing = {
            pushState(editorState.copy(drawingStrokes = emptyList()))
          },
          canUndoDrawing = editorState.drawingStrokes.isNotEmpty(),
          onAddShape = { shapeType ->
            val newShape = ShapeStickerItem(type = shapeType)
            pushState(editorState.copy(shapes = editorState.shapes + newShape))
          },
          onAddSticker = { emoji ->
            val newSticker = ShapeStickerItem(type = EditorShapeType.STICKER, stickerSymbol = emoji)
            pushState(editorState.copy(shapes = editorState.shapes + newSticker))
          },
          onClearShapes = {
            pushState(editorState.copy(shapes = emptyList()))
          },
          onAddText = { text ->
            val newText = TextLayerItem(text = text)
            pushState(editorState.copy(textLayers = editorState.textLayers + newText))
          },
          onDeleteText = { id ->
            pushState(editorState.copy(textLayers = editorState.textLayers.filterNot { it.id == id }))
          },
          onAddPrivacyRegion = { type ->
            val newRegion = BlurMosaicRegion(type = type)
            pushState(editorState.copy(blurMosaicRegions = editorState.blurMosaicRegions + newRegion))
          },
          onClearPrivacyRegions = {
            pushState(editorState.copy(blurMosaicRegions = emptyList()))
          }
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
