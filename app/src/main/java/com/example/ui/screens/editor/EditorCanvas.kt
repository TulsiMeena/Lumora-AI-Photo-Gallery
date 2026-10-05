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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.editor.PhotoEditorEngine
import com.example.model.editor.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun EditorCanvas(
  originalBitmap: Bitmap?,
  previewBitmap: Bitmap?,
  state: PhotoEditorState,
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

  val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
    // Only zoom/pan if not actively drawing or adjusting handles
    if (state.activeCategory != EditorCategory.DRAW &&
      state.activeCategory != EditorCategory.RETOUCH &&
      !isSplitDividerDragging
    ) {
      scale = (scale * zoomChange).coerceIn(0.5f, 6f)
      offset += panChange
    }
  }

  // Double tap to reset zoom & pan
  val tapGestureModifier = Modifier.pointerInput(Unit) {
    detectTapGestures(
      onDoubleTap = {
        scale = 1f
        offset = Offset.Zero
      },
      onTap = { tapOffset ->
        if (state.activeCategory == EditorCategory.RETOUCH) {
          // Normalize coordinates (0f..1f)
          val normX = (tapOffset.x / size.width).coerceIn(0f, 1f)
          val normY = (tapOffset.y / size.height).coerceIn(0f, 1f)
          onAddRetouchPoint(normX, normY)
        }
      }
    )
  }

  // Drawing gesture modifier
  val drawGestureModifier = if (state.activeCategory == EditorCategory.DRAW) {
    Modifier.pointerInput(state.drawingStrokes.size) {
      detectDragGestures(
        onDragStart = { startOffset ->
          val nx = (startOffset.x / size.width).coerceIn(0f, 1f)
          val ny = (startOffset.y / size.height).coerceIn(0f, 1f)
          onAddDrawingPoint(nx, ny)
        },
        onDrag = { change, _ ->
          change.consume()
          val nx = (change.position.x / size.width).coerceIn(0f, 1f)
          val ny = (change.position.y / size.height).coerceIn(0f, 1f)
          onAddDrawingPoint(nx, ny)
        },
        onDragEnd = {
          onEndDrawingStroke()
        },
        onDragCancel = {
          onEndDrawingStroke()
        }
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
      .then(drawGestureModifier)
      .then(splitDragModifier)
  ) {
    val bitmapToDraw = previewBitmap ?: originalBitmap

    if (bitmapToDraw != null && !bitmapToDraw.isRecycled) {
      // Build real-time compose ColorMatrix from state
      val colorMatrix = remember(state.lightAdjustments, state.colorAdjustments) {
        val cm = PhotoEditorEngine.createAdjustmentColorMatrix(state.lightAdjustments, state.colorAdjustments)
        ColorMatrix(cm.array)
      }

      val imageBitmap = remember(bitmapToDraw) { bitmapToDraw.asImageBitmap() }
      val originalImageBitmap = remember(originalBitmap) { originalBitmap?.asImageBitmap() }

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y,
            rotationZ = state.cropTransform.straightenAngle
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

        // 1. Draw Before / After or Split View
        when (state.compareMode) {
          CompareMode.HOLD_BEFORE -> {
            // Draw pure original without adjustments
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

            // Draw Original (Before) on Left
            clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasHeight) {
              if (originalImageBitmap != null) {
                drawImage(
                  image = originalImageBitmap,
                  dstOffset = dstOffset,
                  dstSize = dstSize
                )
              }
            }

            // Draw Edited (After) on Right
            clipRect(left = splitX, top = 0f, right = canvasWidth, bottom = canvasHeight) {
              drawImage(
                image = imageBitmap,
                dstOffset = dstOffset,
                dstSize = dstSize,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
              )
            }

            // Draw Divider Line & Knob
            drawLine(
              color = Color.White,
              start = Offset(splitX, 0f),
              end = Offset(splitX, canvasHeight),
              strokeWidth = 3.dp.toPx()
            )
            drawCircle(
              color = Color(0xFF3B82F6),
              radius = 18.dp.toPx(),
              center = Offset(splitX, canvasHeight / 2f)
            )
            drawCircle(
              color = Color.White,
              radius = 8.dp.toPx(),
              center = Offset(splitX, canvasHeight / 2f)
            )
          }

          else -> {
            // Standard Live Render
            drawImage(
              image = imageBitmap,
              dstOffset = dstOffset,
              dstSize = dstSize,
              colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
          }
        }

        // 2. Crop Overlay Guides
        if (state.activeCategory == EditorCategory.CROP) {
          drawCropGuides(left, top, drawWidth, drawHeight, state.cropTransform)
        }

        // 3. Drawing Strokes Overlay
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

        // 4. Retouching Markers Overlay
        if (state.activeCategory == EditorCategory.RETOUCH) {
          for (item in state.retouchItems) {
            val cx = left + item.targetX * drawWidth
            val cy = top + item.targetY * drawHeight
            drawCircle(
              color = Color(0xFF10B981).copy(alpha = 0.8f),
              radius = item.radius * (drawWidth / 600f).coerceAtLeast(10f),
              center = Offset(cx, cy),
              style = Stroke(width = 2.dp.toPx())
            )
          }
        }

        // 5. Privacy Mosaic Region Overlay
        if (state.activeCategory == EditorCategory.PRIVACY) {
          for (region in state.blurMosaicRegions) {
            val rx = left + region.normalizedLeft * drawWidth
            val ry = top + region.normalizedTop * drawHeight
            val rw = (region.normalizedRight - region.normalizedLeft) * drawWidth
            val rh = (region.normalizedBottom - region.normalizedTop) * drawHeight
            drawRect(
              color = Color(0xFFEF4444).copy(alpha = 0.35f),
              topLeft = Offset(rx, ry),
              size = Size(rw, rh)
            )
            drawRect(
              color = Color(0xFFEF4444),
              topLeft = Offset(rx, ry),
              size = Size(rw, rh),
              style = Stroke(width = 2.dp.toPx())
            )
          }
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
}
