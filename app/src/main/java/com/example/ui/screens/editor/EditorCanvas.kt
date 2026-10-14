package com.example.ui.screens.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.editor.PhotoEditorEngine
import com.example.model.editor.*
import kotlin.math.max
import kotlin.math.min

enum class CropHandle {
  TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
  LEFT, RIGHT, TOP, BOTTOM, CENTER
}

@Composable
fun EditorCanvas(
  originalBitmap: Bitmap?,
  previewBitmap: Bitmap?,
  state: PhotoEditorState,
  activeStrokePoints: List<DrawingPoint> = emptyList(),
  activeStrokeColor: Long = 0xFFFF3B30,
  activeStrokeWidth: Float = 10f,
  onUpdateState: (PhotoEditorState) -> Unit,
  onAddDrawingPoint: (Float, Float) -> Unit,
  onEndDrawingStroke: () -> Unit,
  onAddRetouchPoint: (Float, Float) -> Unit,
  onMoveSelectedText: (String, Float, Float) -> Unit,
  onMoveSelectedShape: (String, Float, Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  var isSplitDividerDragging by remember { mutableStateOf(false) }
  var activeCropHandle by remember { mutableStateOf<CropHandle?>(null) }
  var selectedTextId by remember { mutableStateOf<String?>(null) }
  var selectedShapeId by remember { mutableStateOf<String?>(null) }
  var selectedPrivacyRegionId by remember { mutableStateOf<String?>(null) }

  val bitmapToDraw = previewBitmap ?: originalBitmap

  val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
    // Only zoom/pan when in neutral navigation (not drawing, cropping, or moving overlays)
    if (state.activeCategory != EditorCategory.DRAW &&
      state.activeCategory != EditorCategory.RETOUCH &&
      state.activeCategory != EditorCategory.CROP &&
      state.activeCategory != EditorCategory.TEXT &&
      state.activeCategory != EditorCategory.SHAPES &&
      state.activeCategory != EditorCategory.PRIVACY &&
      !isSplitDividerDragging
    ) {
      scale = (scale * zoomChange).coerceIn(0.5f, 6f)
      offset += panChange
    }
  }

  // Double tap to reset zoom & pan / Tap for retouch spot
  val tapGestureModifier = Modifier.pointerInput(state.activeCategory, bitmapToDraw) {
    detectTapGestures(
      onDoubleTap = {
        scale = 1f
        offset = Offset.Zero
      },
      onTap = { tapOffset ->
        if (state.activeCategory == EditorCategory.RETOUCH && bitmapToDraw != null) {
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val normX = ((tapOffset.x - imgLeft) / drawWidth).coerceIn(0f, 1f)
          val normY = ((tapOffset.y - imgTop) / drawHeight).coerceIn(0f, 1f)
          onAddRetouchPoint(normX, normY)
        }
      }
    )
  }

  val currentCropTransform by rememberUpdatedState(state.cropTransform)
  val currentOnUpdateState by rememberUpdatedState(onUpdateState)
  val currentPhotoEditorState by rememberUpdatedState(state)

  // Crop Drag Gesture Modifier
  val cropGestureModifier = if (state.activeCategory == EditorCategory.CROP && bitmapToDraw != null) {
    Modifier.pointerInput(bitmapToDraw) {
      detectDragGestures(
        onDragStart = { startOffset ->
          val cur = currentCropTransform
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val cropL = imgLeft + cur.cropLeft * drawWidth
          val cropT = imgTop + cur.cropTop * drawHeight
          val cropR = imgLeft + cur.cropRight * drawWidth
          val cropB = imgTop + cur.cropBottom * drawHeight

          val hitDist = 56.dp.toPx()
          val x = startOffset.x
          val y = startOffset.y

          activeCropHandle = when {
            kotlin.math.hypot((x - cropL), (y - cropT)) < hitDist -> CropHandle.TOP_LEFT
            kotlin.math.hypot((x - cropR), (y - cropT)) < hitDist -> CropHandle.TOP_RIGHT
            kotlin.math.hypot((x - cropL), (y - cropB)) < hitDist -> CropHandle.BOTTOM_LEFT
            kotlin.math.hypot((x - cropR), (y - cropB)) < hitDist -> CropHandle.BOTTOM_RIGHT
            kotlin.math.abs(x - cropL) < hitDist && y in cropT..cropB -> CropHandle.LEFT
            kotlin.math.abs(x - cropR) < hitDist && y in cropT..cropB -> CropHandle.RIGHT
            kotlin.math.abs(y - cropT) < hitDist && x in cropL..cropR -> CropHandle.TOP
            kotlin.math.abs(y - cropB) < hitDist && x in cropL..cropR -> CropHandle.BOTTOM
            x in cropL..cropR && y in cropT..cropB -> CropHandle.CENTER
            else -> null
          }
        },
        onDrag = { change, dragAmount ->
          change.consume()
          val handle = activeCropHandle ?: return@detectDragGestures
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight

          val dx = dragAmount.x / drawWidth
          val dy = dragAmount.y / drawHeight
          val cur = currentCropTransform

          val updated = when (handle) {
            CropHandle.TOP_LEFT -> cur.copy(
              cropLeft = (cur.cropLeft + dx).coerceIn(0f, cur.cropRight - 0.08f),
              cropTop = (cur.cropTop + dy).coerceIn(0f, cur.cropBottom - 0.08f)
            )
            CropHandle.TOP_RIGHT -> cur.copy(
              cropRight = (cur.cropRight + dx).coerceIn(cur.cropLeft + 0.08f, 1f),
              cropTop = (cur.cropTop + dy).coerceIn(0f, cur.cropBottom - 0.08f)
            )
            CropHandle.BOTTOM_LEFT -> cur.copy(
              cropLeft = (cur.cropLeft + dx).coerceIn(0f, cur.cropRight - 0.08f),
              cropBottom = (cur.cropBottom + dy).coerceIn(cur.cropTop + 0.08f, 1f)
            )
            CropHandle.BOTTOM_RIGHT -> cur.copy(
              cropRight = (cur.cropRight + dx).coerceIn(cur.cropLeft + 0.08f, 1f),
              cropBottom = (cur.cropBottom + dy).coerceIn(cur.cropTop + 0.08f, 1f)
            )
            CropHandle.LEFT -> cur.copy(
              cropLeft = (cur.cropLeft + dx).coerceIn(0f, cur.cropRight - 0.08f)
            )
            CropHandle.RIGHT -> cur.copy(
              cropRight = (cur.cropRight + dx).coerceIn(cur.cropLeft + 0.08f, 1f)
            )
            CropHandle.TOP -> cur.copy(
              cropTop = (cur.cropTop + dy).coerceIn(0f, cur.cropBottom - 0.08f)
            )
            CropHandle.BOTTOM -> cur.copy(
              cropBottom = (cur.cropBottom + dy).coerceIn(cur.cropTop + 0.08f, 1f)
            )
            CropHandle.CENTER -> {
              val w = cur.cropRight - cur.cropLeft
              val h = cur.cropBottom - cur.cropTop
              val newL = (cur.cropLeft + dx).coerceIn(0f, 1f - w)
              val newT = (cur.cropTop + dy).coerceIn(0f, 1f - h)
              cur.copy(
                cropLeft = newL,
                cropRight = newL + w,
                cropTop = newT,
                cropBottom = newT + h
              )
            }
          }
          currentOnUpdateState(currentPhotoEditorState.copy(cropTransform = updated))
        },
        onDragEnd = { activeCropHandle = null },
        onDragCancel = { activeCropHandle = null }
      )
    }
  } else Modifier

  // Live Drawing Gesture Modifier (pixel-perfect directly beneath user's finger)
  val drawGestureModifier = if (state.activeCategory == EditorCategory.DRAW && bitmapToDraw != null) {
    Modifier.pointerInput(state.drawingStrokes.size, bitmapToDraw) {
      detectDragGestures(
        onDragStart = { startOffset ->
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val nx = ((startOffset.x - imgLeft) / drawWidth).coerceIn(0f, 1f)
          val ny = ((startOffset.y - imgTop) / drawHeight).coerceIn(0f, 1f)
          onAddDrawingPoint(nx, ny)
        },
        onDrag = { change, _ ->
          change.consume()
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val nx = ((change.position.x - imgLeft) / drawWidth).coerceIn(0f, 1f)
          val ny = ((change.position.y - imgTop) / drawHeight).coerceIn(0f, 1f)
          onAddDrawingPoint(nx, ny)
        },
        onDragEnd = { onEndDrawingStroke() },
        onDragCancel = { onEndDrawingStroke() }
      )
    }
  } else Modifier

  // Text Layer Dragging Modifier (1:1 motion directly under user's finger)
  val textDragModifier = if (state.activeCategory == EditorCategory.TEXT && state.textLayers.isNotEmpty() && bitmapToDraw != null) {
    Modifier.pointerInput(state.textLayers, bitmapToDraw) {
      detectDragGestures(
        onDragStart = { startOffset ->
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val normX = (startOffset.x - imgLeft) / drawWidth
          val normY = (startOffset.y - imgTop) / drawHeight
          selectedTextId = state.textLayers.minByOrNull {
            kotlin.math.hypot(it.normalizedX - normX, it.normalizedY - normY)
          }?.id
        },
        onDrag = { change, dragAmount ->
          change.consume()
          val id = selectedTextId ?: return@detectDragGestures
          val target = state.textLayers.find { it.id == id } ?: return@detectDragGestures

          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight

          val nx = (target.normalizedX + dragAmount.x / drawWidth).coerceIn(0.05f, 0.95f)
          val ny = (target.normalizedY + dragAmount.y / drawHeight).coerceIn(0.05f, 0.95f)
          onMoveSelectedText(id, nx, ny)
        },
        onDragEnd = { selectedTextId = null },
        onDragCancel = { selectedTextId = null }
      )
    }
  } else Modifier

  // Shape / Sticker Dragging Modifier (1:1 motion directly under user's finger)
  val shapeDragModifier = if (state.activeCategory == EditorCategory.SHAPES && state.shapes.isNotEmpty() && bitmapToDraw != null) {
    Modifier.pointerInput(state.shapes, bitmapToDraw) {
      detectDragGestures(
        onDragStart = { startOffset ->
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val normX = (startOffset.x - imgLeft) / drawWidth
          val normY = (startOffset.y - imgTop) / drawHeight
          selectedShapeId = state.shapes.minByOrNull {
            kotlin.math.hypot(it.normalizedX - normX, it.normalizedY - normY)
          }?.id
        },
        onDrag = { change, dragAmount ->
          change.consume()
          val id = selectedShapeId ?: return@detectDragGestures
          val target = state.shapes.find { it.id == id } ?: return@detectDragGestures

          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight

          val nx = (target.normalizedX + dragAmount.x / drawWidth).coerceIn(0.05f, 0.95f)
          val ny = (target.normalizedY + dragAmount.y / drawHeight).coerceIn(0.05f, 0.95f)
          onMoveSelectedShape(id, nx, ny)
        },
        onDragEnd = { selectedShapeId = null },
        onDragCancel = { selectedShapeId = null }
      )
    }
  } else Modifier

  // Privacy Region Dragging Modifier
  val privacyDragModifier = if (state.activeCategory == EditorCategory.PRIVACY && state.blurMosaicRegions.isNotEmpty() && bitmapToDraw != null) {
    Modifier.pointerInput(state.blurMosaicRegions, bitmapToDraw) {
      detectDragGestures(
        onDragStart = { startOffset ->
          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight
          val imgLeft = (canvasWidth - drawWidth) / 2f
          val imgTop = (canvasHeight - drawHeight) / 2f

          val normX = (startOffset.x - imgLeft) / drawWidth
          val normY = (startOffset.y - imgTop) / drawHeight
          selectedPrivacyRegionId = state.blurMosaicRegions.find {
            normX in it.normalizedLeft..it.normalizedRight && normY in it.normalizedTop..it.normalizedBottom
          }?.id ?: state.blurMosaicRegions.lastOrNull()?.id
        },
        onDrag = { change, dragAmount ->
          change.consume()
          val id = selectedPrivacyRegionId ?: return@detectDragGestures
          val target = state.blurMosaicRegions.find { it.id == id } ?: return@detectDragGestures

          val canvasWidth = size.width.toFloat()
          val canvasHeight = size.height.toFloat()
          val imgW = bitmapToDraw.width.toFloat()
          val imgH = bitmapToDraw.height.toFloat()
          val imgRatio = imgW / imgH
          val canvasRatio = canvasWidth / canvasHeight
          val drawWidth = if (imgRatio > canvasRatio) canvasWidth else canvasHeight * imgRatio
          val drawHeight = if (imgRatio > canvasRatio) canvasWidth / imgRatio else canvasHeight

          val dx = dragAmount.x / drawWidth
          val dy = dragAmount.y / drawHeight
          val w = target.normalizedRight - target.normalizedLeft
          val h = target.normalizedBottom - target.normalizedTop
          val newL = (target.normalizedLeft + dx).coerceIn(0f, 1f - w)
          val newT = (target.normalizedTop + dy).coerceIn(0f, 1f - h)
          val updatedList = state.blurMosaicRegions.map {
            if (it.id == id) it.copy(normalizedLeft = newL, normalizedRight = newL + w, normalizedTop = newT, normalizedBottom = newT + h)
            else it
          }
          currentOnUpdateState(currentPhotoEditorState.copy(blurMosaicRegions = updatedList))
        },
        onDragEnd = { selectedPrivacyRegionId = null },
        onDragCancel = { selectedPrivacyRegionId = null }
      )
    }
  } else Modifier

  // Split comparison dragging modifier
  val splitDragModifier = if (state.compareMode == CompareMode.SPLIT_SLIDER) {
    Modifier.pointerInput(Unit) {
      detectDragGestures(
        onDragStart = { isSplitDividerDragging = true },
        onDrag = { change, _ ->
          change.consume()
          val newPos = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
          onUpdateState(state.copy(splitSliderPosition = newPos))
        },
        onDragEnd = { isSplitDividerDragging = false },
        onDragCancel = { isSplitDividerDragging = false }
      )
    }
  } else Modifier

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F1117))
      .then(transformableState.let { Modifier.transformable(it) })
      .then(tapGestureModifier)
      .then(cropGestureModifier)
      .then(drawGestureModifier)
      .then(textDragModifier)
      .then(shapeDragModifier)
      .then(privacyDragModifier)
      .then(splitDragModifier)
  ) {
    if (bitmapToDraw != null && !bitmapToDraw.isRecycled) {
      // Build zero-latency GPU ColorMatrix with Light, Color, and ColorGrading
      val colorMatrix = remember(state.lightAdjustments, state.colorAdjustments, state.colorGrading) {
        val cm = PhotoEditorEngine.createAdjustmentColorMatrix(
          light = state.lightAdjustments,
          color = state.colorAdjustments,
          grading = state.colorGrading
        )
        ColorMatrix(cm.array)
      }

      val imageBitmap = remember(bitmapToDraw) { bitmapToDraw.asImageBitmap() }
      val originalImageBitmap = remember(originalBitmap) { originalBitmap?.asImageBitmap() }

      val flipX = if (state.cropTransform.flipHorizontal) -1f else 1f
      val flipY = if (state.cropTransform.flipVertical) -1f else 1f
      val totalRotation = state.cropTransform.rotationDegrees.toFloat() + state.cropTransform.straightenAngle

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale * flipX,
            scaleY = scale * flipY,
            translationX = offset.x,
            translationY = offset.y,
            rotationZ = totalRotation
          )
      ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val imgWidth = imageBitmap.width.toFloat()
        val imgHeight = imageBitmap.height.toFloat()

        // Fit image inside canvas bounds
        val imgRatio = imgWidth / imgHeight
        val canvasRatio = canvasWidth / canvasHeight

        val drawWidth: Float
        val drawHeight: Float
        if (imgRatio > canvasRatio) {
          drawWidth = canvasWidth
          drawHeight = canvasWidth / imgRatio
        } else {
          drawHeight = canvasHeight
          drawWidth = canvasHeight * imgRatio
        }

        val left = (canvasWidth - drawWidth) / 2f
        val top = (canvasHeight - drawHeight) / 2f
        val dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
        val dstOffset = IntOffset(left.toInt(), top.toInt())

        val isCropped = state.cropTransform.cropLeft > 0f || state.cropTransform.cropTop > 0f ||
            state.cropTransform.cropRight < 1f || state.cropTransform.cropBottom < 1f
        val shouldClip = state.activeCategory != EditorCategory.CROP && isCropped

        val cropL = left + state.cropTransform.cropLeft * drawWidth
        val cropT = top + state.cropTransform.cropTop * drawHeight
        val cropR = left + state.cropTransform.cropRight * drawWidth
        val cropB = top + state.cropTransform.cropBottom * drawHeight

        val clipLeft = if (shouldClip) cropL else 0f
        val clipTop = if (shouldClip) cropT else 0f
        val clipRight = if (shouldClip) cropR else canvasWidth
        val clipBottom = if (shouldClip) cropB else canvasHeight

        clipRect(left = clipLeft, top = clipTop, right = clipRight, bottom = clipBottom) {
          // 1. Draw Image with GPU Color Filter or Compare View
          when (state.compareMode) {
            CompareMode.HOLD_BEFORE -> {
              if (originalImageBitmap != null) {
                drawImage(
                  image = originalImageBitmap,
                  dstOffset = dstOffset,
                  dstSize = dstSize
                )
              }
            }

            CompareMode.SPLIT_SLIDER -> {
              val splitX = canvasWidth * state.splitSliderPosition

              clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasHeight) {
                if (originalImageBitmap != null) {
                  drawImage(
                    image = originalImageBitmap,
                    dstOffset = dstOffset,
                    dstSize = dstSize
                  )
                }
              }

              clipRect(left = splitX, top = 0f, right = canvasWidth, bottom = canvasHeight) {
                drawImage(
                  image = imageBitmap,
                  dstOffset = dstOffset,
                  dstSize = dstSize,
                  colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )
              }

              // Divider Line & Handle
              drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, canvasHeight),
                strokeWidth = 3.dp.toPx()
              )
              drawCircle(
                color = Color(0xFF3B82F6),
                radius = 16.dp.toPx(),
                center = Offset(splitX, canvasHeight / 2f)
              )
              drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(splitX, canvasHeight / 2f)
              )
            }

            else -> {
              drawImage(
                image = imageBitmap,
                dstOffset = dstOffset,
                dstSize = dstSize,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
              )
            }
          }

        // 3. Privacy Blur / Mosaic Regions (Redaction)
        for (region in state.blurMosaicRegions) {
          val rx = left + region.normalizedLeft * drawWidth
          val ry = top + region.normalizedTop * drawHeight
          val rw = (region.normalizedRight - region.normalizedLeft) * drawWidth
          val rh = (region.normalizedBottom - region.normalizedTop) * drawHeight

          if (region.type == BlurMosaicType.MOSAIC) {
            val tileSize = 20.dp.toPx()
            val cols = (rw / tileSize).toInt().coerceAtLeast(1)
            val rows = (rh / tileSize).toInt().coerceAtLeast(1)
            val tileW = rw / cols
            val tileH = rh / rows
            for (c in 0 until cols) {
              for (r in 0 until rows) {
                val shade = if ((c + r) % 2 == 0) Color(0xFF1E2430) else Color(0xFF333D4F)
                drawRect(
                  color = shade.copy(alpha = 0.95f),
                  topLeft = Offset(rx + c * tileW, ry + r * tileH),
                  size = Size(tileW, tileH)
                )
              }
            }
          } else {
            drawRect(
              color = Color(0xFF1E293B).copy(alpha = 0.85f),
              topLeft = Offset(rx, ry),
              size = Size(rw, rh)
            )
          }

          if (state.activeCategory == EditorCategory.PRIVACY) {
            drawRect(
              color = Color(0xFFEF4444),
              topLeft = Offset(rx, ry),
              size = Size(rw, rh),
              style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(color = Color(0xFFEF4444), radius = 5.dp.toPx(), center = Offset(rx, ry))
            drawCircle(color = Color(0xFFEF4444), radius = 5.dp.toPx(), center = Offset(rx + rw, ry))
            drawCircle(color = Color(0xFFEF4444), radius = 5.dp.toPx(), center = Offset(rx, ry + rh))
            drawCircle(color = Color(0xFFEF4444), radius = 5.dp.toPx(), center = Offset(rx + rw, ry + rh))
          }
        }

        // 4. Retouching Markers (Healing spots)
        if (state.activeCategory == EditorCategory.RETOUCH) {
          for (item in state.retouchItems) {
            val cx = left + item.targetX * drawWidth
            val cy = top + item.targetY * drawHeight
            drawCircle(
              color = Color(0xFF10B981).copy(alpha = 0.85f),
              radius = item.radius * (drawWidth / 600f).coerceAtLeast(10f),
              center = Offset(cx, cy),
              style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
              color = Color(0xFF10B981).copy(alpha = 0.25f),
              radius = item.radius * (drawWidth / 600f).coerceAtLeast(10f),
              center = Offset(cx, cy)
            )
          }
        }

        // 5. Drawing Strokes Overlay (Completed strokes + Live active stroke)
        for (stroke in state.drawingStrokes) {
          if (stroke.points.size < 2) continue
          val strokePoints = stroke.points.map { pt ->
            Offset(left + pt.x * drawWidth, top + pt.y * drawHeight)
          }
          drawPoints(
            points = strokePoints,
            pointMode = PointMode.Polygon,
            color = Color(stroke.colorHex.toInt()).copy(alpha = stroke.opacity),
            strokeWidth = stroke.strokeWidth * (drawWidth / 600f).coerceAtLeast(2f),
            cap = StrokeCap.Round
          )
        }

        // Live Active Stroke (beneath finger with 0ms latency)
        if (activeStrokePoints.size >= 2) {
          val livePoints = activeStrokePoints.map { pt ->
            Offset(left + pt.x * drawWidth, top + pt.y * drawHeight)
          }
          drawPoints(
            points = livePoints,
            pointMode = PointMode.Polygon,
            color = Color(activeStrokeColor.toInt()),
            strokeWidth = activeStrokeWidth * (drawWidth / 600f).coerceAtLeast(2f),
            cap = StrokeCap.Round
          )
        }

        // 6. Shapes & Stickers Overlay
        val canvasNative = drawContext.canvas.nativeCanvas
        for (shape in state.shapes) {
          val sx = left + shape.normalizedX * drawWidth
          val sy = top + shape.normalizedY * drawHeight
          val sw = shape.normalizedWidth * drawWidth
          val sh = shape.normalizedHeight * drawHeight
          val halfW = sw / 2f
          val halfH = sh / 2f

          canvasNative.save()
          canvasNative.translate(sx, sy)
          if (shape.rotationDegrees != 0f) canvasNative.rotate(shape.rotationDegrees)

          if (shape.type == EditorShapeType.STICKER) {
            val stickerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
              textAlign = android.graphics.Paint.Align.CENTER
              textSize = sw.coerceAtLeast(36.dp.toPx())
            }
            canvasNative.drawText(shape.stickerSymbol, 0f, stickerPaint.textSize / 3f, stickerPaint)
          } else {
            val shapePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
              color = shape.colorHex.toInt()
              alpha = (shape.opacity * 255).toInt().coerceIn(0, 255)
              strokeWidth = shape.strokeWidth * (drawWidth / 600f).coerceAtLeast(2f)
              style = if (shape.isFilled) android.graphics.Paint.Style.FILL else android.graphics.Paint.Style.STROKE
            }

            when (shape.type) {
              EditorShapeType.RECTANGLE -> {
                canvasNative.drawRect(-halfW, -halfH, halfW, halfH, shapePaint)
              }
              EditorShapeType.CIRCLE -> {
                canvasNative.drawOval(android.graphics.RectF(-halfW, -halfH, halfW, halfH), shapePaint)
              }
              EditorShapeType.LINE -> {
                canvasNative.drawLine(-halfW, 0f, halfW, 0f, shapePaint)
              }
              EditorShapeType.ARROW -> {
                canvasNative.drawLine(-halfW, 0f, halfW, 0f, shapePaint)
                canvasNative.drawLine(halfW - halfW * 0.35f, -halfH * 0.35f, halfW, 0f, shapePaint)
                canvasNative.drawLine(halfW - halfW * 0.35f, halfH * 0.35f, halfW, 0f, shapePaint)
              }
              EditorShapeType.STAR -> {
                val path = android.graphics.Path()
                val rOuter = min(halfW, halfH)
                val rInner = rOuter * 0.45f
                for (i in 0 until 10) {
                  val r = if (i % 2 == 0) rOuter else rInner
                  val angle = i * Math.PI / 5.0 - Math.PI / 2.0
                  val px = (r * Math.cos(angle)).toFloat()
                  val py = (r * Math.sin(angle)).toFloat()
                  if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvasNative.drawPath(path, shapePaint)
              }
              EditorShapeType.HEART -> {
                val path = android.graphics.Path()
                val s = min(halfW, halfH)
                path.moveTo(0f, s * 0.3f)
                path.cubicTo(-s * 0.8f, -s * 0.5f, -s * 0.8f, -s * 0.9f, 0f, -s * 0.3f)
                path.cubicTo(s * 0.8f, -s * 0.9f, s * 0.8f, -s * 0.5f, 0f, s * 0.3f)
                canvasNative.drawPath(path, shapePaint)
              }
              else -> {}
            }
          }

          if (state.activeCategory == EditorCategory.SHAPES) {
            val selPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
              color = 0xFF60A5FA.toInt()
              style = android.graphics.Paint.Style.STROKE
              strokeWidth = 1.5.dp.toPx()
              pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }
            canvasNative.drawRect(-halfW - 6f, -halfH - 6f, halfW + 6f, halfH + 6f, selPaint)
          }

          canvasNative.restore()
        }

        // 7. Text Layers Overlay
        for (layer in state.textLayers) {
          if (layer.text.isBlank()) continue
          val tx = left + layer.normalizedX * drawWidth
          val ty = top + layer.normalizedY * drawHeight
          val fontSize = layer.fontSizeSp * (drawWidth / 400f).coerceAtLeast(14f) * layer.scale

          val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = fontSize
            color = layer.textColorHex.toInt()
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            typeface = when (layer.font) {
              EditorFont.SERIF -> android.graphics.Typeface.SERIF
              EditorFont.MONOSPACE -> android.graphics.Typeface.MONOSPACE
              EditorFont.CURSIVE -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
              EditorFont.SANS_SERIF -> android.graphics.Typeface.SANS_SERIF
            }
            if (layer.isBold && layer.isItalic) {
              typeface = android.graphics.Typeface.create(typeface, android.graphics.Typeface.BOLD_ITALIC)
            } else if (layer.isBold) {
              typeface = android.graphics.Typeface.create(typeface, android.graphics.Typeface.BOLD)
            } else if (layer.isItalic) {
              typeface = android.graphics.Typeface.create(typeface, android.graphics.Typeface.ITALIC)
            }
          }

          val bounds = android.graphics.Rect()
          textPaint.getTextBounds(layer.text, 0, layer.text.length, bounds)

          canvasNative.save()
          canvasNative.translate(tx, ty)
          if (layer.rotationDegrees != 0f) canvasNative.rotate(layer.rotationDegrees)

          // Background Pill
          if (layer.backgroundColorHex != null) {
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
              color = layer.backgroundColorHex.toInt()
              alpha = (layer.opacity * 220).toInt().coerceIn(0, 255)
            }
            val padX = fontSize * 0.45f
            val padY = fontSize * 0.25f
            val bgRect = android.graphics.RectF(
              bounds.left - padX,
              bounds.top - padY,
              bounds.right + padX,
              bounds.bottom + padY
            )
            canvasNative.drawRoundRect(bgRect, padY * 1.5f, padY * 1.5f, bgPaint)
          }

          // Shadow
          if (layer.shadowColorHex != null) {
            textPaint.setShadowLayer(8f, 2f, 4f, layer.shadowColorHex.toInt())
          } else {
            textPaint.clearShadowLayer()
          }

          // Draw Text
          canvasNative.drawText(layer.text, 0f, 0f, textPaint)

          // Selection border when in Text category
          if (state.activeCategory == EditorCategory.TEXT) {
            val selPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
              color = 0xFF3B82F6.toInt()
              style = android.graphics.Paint.Style.STROKE
              strokeWidth = 2.dp.toPx()
            }
            val padX = fontSize * 0.5f
            val padY = fontSize * 0.3f
            canvasNative.drawRoundRect(
              android.graphics.RectF(bounds.left - padX, bounds.top - padY, bounds.right + padX, bounds.bottom + padY),
              8f, 8f, selPaint
            )
          }

          canvasNative.restore()
        }
      }

      // Crop Overlay Guides (drawn outside clipRect so guides are always visible when cropping)
      if (state.activeCategory == EditorCategory.CROP) {
        drawCropGuides(left, top, drawWidth, drawHeight, state.cropTransform)
      }
    }
    }
  }
}

/**
 * Draws Rule-of-Thirds and Boundary Crop Guides
 */
private fun DrawScope.drawCropGuides(
  imageLeft: Float,
  imageTop: Float,
  imageWidth: Float,
  imageHeight: Float,
  crop: CropTransform
) {
  val cropL = imageLeft + crop.cropLeft * imageWidth
  val cropT = imageTop + crop.cropTop * imageHeight
  val cropR = imageLeft + crop.cropRight * imageWidth
  val cropB = imageTop + crop.cropBottom * imageHeight
  val cropW = cropR - cropL
  val cropH = cropB - cropT

  // Dark dim outside crop area
  drawRect(
    color = Color.Black.copy(alpha = 0.6f),
    topLeft = Offset(imageLeft, imageTop),
    size = Size(imageWidth, cropT - imageTop)
  )
  drawRect(
    color = Color.Black.copy(alpha = 0.6f),
    topLeft = Offset(imageLeft, cropB),
    size = Size(imageWidth, imageTop + imageHeight - cropB)
  )
  drawRect(
    color = Color.Black.copy(alpha = 0.6f),
    topLeft = Offset(imageLeft, cropT),
    size = Size(cropL - imageLeft, cropH)
  )
  drawRect(
    color = Color.Black.copy(alpha = 0.6f),
    topLeft = Offset(cropR, cropT),
    size = Size(imageLeft + imageWidth - cropR, cropH)
  )

  // Border rectangle
  drawRect(
    color = Color.White,
    topLeft = Offset(cropL, cropT),
    size = Size(cropW, cropH),
    style = Stroke(width = 2.dp.toPx())
  )

  // Rule of thirds lines
  val thirdW = cropW / 3f
  val thirdH = cropH / 3f

  drawLine(
    color = Color.White.copy(alpha = 0.5f),
    start = Offset(cropL + thirdW, cropT),
    end = Offset(cropL + thirdW, cropB),
    strokeWidth = 1.dp.toPx()
  )
  drawLine(
    color = Color.White.copy(alpha = 0.5f),
    start = Offset(cropL + 2 * thirdW, cropT),
    end = Offset(cropL + 2 * thirdW, cropB),
    strokeWidth = 1.dp.toPx()
  )
  drawLine(
    color = Color.White.copy(alpha = 0.5f),
    start = Offset(cropL, cropT + thirdH),
    end = Offset(cropR, cropT + thirdH),
    strokeWidth = 1.dp.toPx()
  )
  drawLine(
    color = Color.White.copy(alpha = 0.5f),
    start = Offset(cropL, cropT + 2 * thirdH),
    end = Offset(cropR, cropT + 2 * thirdH),
    strokeWidth = 1.dp.toPx()
  )

  // Visible tactile Corner Handles (Thick L-brackets)
  val cornerArm = 20.dp.toPx().coerceAtMost(cropW * 0.3f).coerceAtMost(cropH * 0.3f)
  val cornerThickness = 4.dp.toPx()
  val handleColor = Color.White

  // Top-Left corner
  drawLine(color = handleColor, start = Offset(cropL, cropT), end = Offset(cropL + cornerArm, cropT), strokeWidth = cornerThickness)
  drawLine(color = handleColor, start = Offset(cropL, cropT), end = Offset(cropL, cropT + cornerArm), strokeWidth = cornerThickness)

  // Top-Right corner
  drawLine(color = handleColor, start = Offset(cropR, cropT), end = Offset(cropR - cornerArm, cropT), strokeWidth = cornerThickness)
  drawLine(color = handleColor, start = Offset(cropR, cropT), end = Offset(cropR, cropT + cornerArm), strokeWidth = cornerThickness)

  // Bottom-Left corner
  drawLine(color = handleColor, start = Offset(cropL, cropB), end = Offset(cropL + cornerArm, cropB), strokeWidth = cornerThickness)
  drawLine(color = handleColor, start = Offset(cropL, cropB), end = Offset(cropL, cropB - cornerArm), strokeWidth = cornerThickness)

  // Bottom-Right corner
  drawLine(color = handleColor, start = Offset(cropR, cropB), end = Offset(cropR - cornerArm, cropB), strokeWidth = cornerThickness)
  drawLine(color = handleColor, start = Offset(cropR, cropB), end = Offset(cropR, cropB - cornerArm), strokeWidth = cornerThickness)

  // Visible edge handles (pill bars)
  val edgeArm = 24.dp.toPx().coerceAtMost(cropW * 0.4f)
  val edgeArmV = 24.dp.toPx().coerceAtMost(cropH * 0.4f)
  val midX = (cropL + cropR) / 2f
  val midY = (cropT + cropB) / 2f

  // Top edge pill
  drawLine(color = handleColor, start = Offset(midX - edgeArm / 2f, cropT), end = Offset(midX + edgeArm / 2f, cropT), strokeWidth = cornerThickness)
  // Bottom edge pill
  drawLine(color = handleColor, start = Offset(midX - edgeArm / 2f, cropB), end = Offset(midX + edgeArm / 2f, cropB), strokeWidth = cornerThickness)
  // Left edge pill
  drawLine(color = handleColor, start = Offset(cropL, midY - edgeArmV / 2f), end = Offset(cropL, midY + edgeArmV / 2f), strokeWidth = cornerThickness)
  // Right edge pill
  drawLine(color = handleColor, start = Offset(cropR, midY - edgeArmV / 2f), end = Offset(cropR, midY + edgeArmV / 2f), strokeWidth = cornerThickness)
}
