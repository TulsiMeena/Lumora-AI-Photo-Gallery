package com.example.ui.screens.resize.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GalleryTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CropRatioOption(val label: String, val ratio: Float?)

val CROP_RATIOS = listOf(
  CropRatioOption("Free", null),
  CropRatioOption("Original", -1f),
  CropRatioOption("1:1", 1.0f),
  CropRatioOption("3.5:4.5 (Passport)", 3.5f / 4.5f),
  CropRatioOption("2:2 (US Visa)", 1.0f),
  CropRatioOption("3:4", 3f / 4f),
  CropRatioOption("4:3", 4f / 3f),
  CropRatioOption("4:5", 4f / 5f),
  CropRatioOption("9:16", 9f / 16f),
  CropRatioOption("16:9", 16f / 9f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropBeforeResizeDialog(
  sourceBitmap: Bitmap,
  onCropApplied: (Bitmap) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  var currentBitmap by remember { mutableStateOf(sourceBitmap) }
  var selectedRatioOption by remember { mutableStateOf(CROP_RATIOS[0]) }

  // Crop box normalized coordinates (0f..1f)
  var cropLeft by remember { mutableFloatStateOf(0.05f) }
  var cropTop by remember { mutableFloatStateOf(0.05f) }
  var cropRight by remember { mutableFloatStateOf(0.95f) }
  var cropBottom by remember { mutableFloatStateOf(0.95f) }

  fun applyRatio(ratioOption: CropRatioOption) {
    selectedRatioOption = ratioOption
    val targetRatio = when {
      ratioOption.ratio == null -> null
      ratioOption.ratio == -1f -> currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
      else -> ratioOption.ratio
    }

    if (targetRatio != null) {
      val centerNormX = (cropLeft + cropRight) / 2f
      val centerNormY = (cropTop + cropBottom) / 2f

      val bmpAspect = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
      var newWidthNorm = 0.8f
      var newHeightNorm = newWidthNorm * (bmpAspect / targetRatio)

      if (newHeightNorm > 0.9f) {
        newHeightNorm = 0.9f
        newWidthNorm = newHeightNorm * (targetRatio / bmpAspect)
      }

      cropLeft = (centerNormX - newWidthNorm / 2f).coerceIn(0.02f, 0.98f)
      cropRight = (centerNormX + newWidthNorm / 2f).coerceIn(0.02f, 0.98f)
      cropTop = (centerNormY - newHeightNorm / 2f).coerceIn(0.02f, 0.98f)
      cropBottom = (centerNormY + newHeightNorm / 2f).coerceIn(0.02f, 0.98f)
    }
  }

  fun rotate90() {
    val matrix = Matrix().apply { postRotate(90f) }
    val rotated = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
    currentBitmap = rotated
    cropLeft = 0.05f
    cropTop = 0.05f
    cropRight = 0.95f
    cropBottom = 0.95f
  }

  fun performCrop() {
    val pixelLeft = (cropLeft * currentBitmap.width).roundToInt().coerceIn(0, currentBitmap.width - 1)
    val pixelTop = (cropTop * currentBitmap.height).roundToInt().coerceIn(0, currentBitmap.height - 1)
    val pixelRight = (cropRight * currentBitmap.width).roundToInt().coerceIn(pixelLeft + 1, currentBitmap.width)
    val pixelBottom = (cropBottom * currentBitmap.height).roundToInt().coerceIn(pixelTop + 1, currentBitmap.height)

    val cropW = max(1, pixelRight - pixelLeft)
    val cropH = max(1, pixelBottom - pixelTop)

    val cropped = Bitmap.createBitmap(currentBitmap, pixelLeft, pixelTop, cropW, cropH)
    onCropApplied(cropped)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Crop Before Resize",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText
          )
        },
        navigationIcon = {
          IconButton(onClick = onDismiss) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
          }
        },
        actions = {
          IconButton(onClick = { rotate90() }) {
            Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90", tint = colors.primaryText)
          }
          Button(
            onClick = { performCrop() },
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(end = 8.dp).testTag("btn_apply_crop")
          ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Apply")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.cardBackground)
      )
    },
    containerColor = Color.Black
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Interactive Crop Canvas
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .background(Color.Black),
        contentAlignment = Alignment.Center
      ) {
        val containerW = maxWidth.value
        val containerH = maxHeight.value

        val bmpW = currentBitmap.width.toFloat()
        val bmpH = currentBitmap.height.toFloat()
        val bmpAspect = bmpW / bmpH
        val containerAspect = containerW / containerH

        val drawW: Float
        val drawH: Float
        if (bmpAspect > containerAspect) {
          drawW = containerW * 0.92f
          drawH = drawW / bmpAspect
        } else {
          drawH = containerH * 0.88f
          drawW = drawH * bmpAspect
        }

        Box(
          modifier = Modifier
            .size(drawW.dp, drawH.dp)
            .border(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
          // Bitmap Rendering
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
              image = currentBitmap.asImageBitmap(),
              dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
            )
          }

          // Crop Scrim & Handles
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(selectedRatioOption) {
                detectDragGestures { change, dragAmount ->
                  change.consume()
                  val deltaNormX = dragAmount.x / size.width
                  val deltaNormY = dragAmount.y / size.height

                  val touchNormX = change.position.x / size.width
                  val touchNormY = change.position.y / size.height

                  // Check if dragging left/right/top/bottom edge or whole box
                  val distL = kotlin.math.abs(touchNormX - cropLeft)
                  val distR = kotlin.math.abs(touchNormX - cropRight)
                  val distT = kotlin.math.abs(touchNormY - cropTop)
                  val distB = kotlin.math.abs(touchNormY - cropBottom)

                  val threshold = 0.12f

                  if (distL < threshold && distL < distR) {
                    cropLeft = (cropLeft + deltaNormX).coerceIn(0.01f, cropRight - 0.1f)
                  } else if (distR < threshold) {
                    cropRight = (cropRight + deltaNormX).coerceIn(cropLeft + 0.1f, 0.99f)
                  }

                  if (distT < threshold && distT < distB) {
                    cropTop = (cropTop + deltaNormY).coerceIn(0.01f, cropBottom - 0.1f)
                  } else if (distB < threshold) {
                    cropBottom = (cropBottom + deltaNormY).coerceIn(cropTop + 0.1f, 0.99f)
                  }

                  // Move whole box if center drag
                  if (distL >= threshold && distR >= threshold && distT >= threshold && distB >= threshold) {
                    val boxW = cropRight - cropLeft
                    val boxH = cropBottom - cropTop
                    val newL = (cropLeft + deltaNormX).coerceIn(0f, 1f - boxW)
                    val newT = (cropTop + deltaNormY).coerceIn(0f, 1f - boxH)
                    cropLeft = newL
                    cropRight = newL + boxW
                    cropTop = newT
                    cropBottom = newT + boxH
                  }
                }
              }
          ) {
            val leftPx = cropLeft * size.width
            val topPx = cropTop * size.height
            val rightPx = cropRight * size.width
            val bottomPx = cropBottom * size.height
            val widthPx = rightPx - leftPx
            val heightPx = bottomPx - topPx

            // Dark Scrim around crop box
            val scrimColor = Color.Black.copy(alpha = 0.55f)
            drawRect(color = scrimColor, topLeft = Offset.Zero, size = Size(size.width, topPx))
            drawRect(color = scrimColor, topLeft = Offset(0f, bottomPx), size = Size(size.width, size.height - bottomPx))
            drawRect(color = scrimColor, topLeft = Offset(0f, topPx), size = Size(leftPx, heightPx))
            drawRect(color = scrimColor, topLeft = Offset(rightPx, topPx), size = Size(size.width - rightPx, heightPx))

            // White crop box border
            drawRect(
              color = Color.White,
              topLeft = Offset(leftPx, topPx),
              size = Size(widthPx, heightPx),
              style = Stroke(width = 2.dp.toPx())
            )

            // 3x3 Grid Lines
            val colStep = widthPx / 3f
            val rowStep = heightPx / 3f
            val gridColor = Color.White.copy(alpha = 0.4f)
            val gridStroke = Stroke(width = 1.dp.toPx())

            drawLine(gridColor, Offset(leftPx + colStep, topPx), Offset(leftPx + colStep, bottomPx), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(leftPx + 2 * colStep, topPx), Offset(leftPx + 2 * colStep, bottomPx), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(leftPx, topPx + rowStep), Offset(rightPx, topPx + rowStep), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(leftPx, topPx + 2 * rowStep), Offset(rightPx, topPx + 2 * rowStep), strokeWidth = gridStroke.width)
          }
        }
      }

      // Aspect Ratio Selector Bar
      Surface(
        color = colors.cardBackground,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
          Text(
            text = "Aspect Ratio Lock",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CROP_RATIOS.forEach { option ->
              val isSelected = selectedRatioOption == option
              FilterChip(
                selected = isSelected,
                onClick = { applyRatio(option) },
                label = {
                  Text(
                    text = option.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                  selectedLabelColor = colors.accent
                )
              )
            }
          }
        }
      }
    }
  }
}
