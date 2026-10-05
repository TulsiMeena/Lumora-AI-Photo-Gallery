package com.example.ui.screens.ai.understanding

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ai.understanding.ImageCropRegion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageRegionSelectorDialog(
  sourceBitmap: Bitmap,
  initialCropRegion: ImageCropRegion? = null,
  onRegionSelected: (ImageCropRegion) -> Unit,
  onDismiss: () -> Unit
) {
  var rotationDegrees by remember { mutableIntStateOf(initialCropRegion?.rotationDegrees ?: 0) }

  // Normalized crop bounds (0f..1f)
  var leftRatio by remember { mutableFloatStateOf(initialCropRegion?.left ?: 0.05f) }
  var topRatio by remember { mutableFloatStateOf(initialCropRegion?.top ?: 0.05f) }
  var rightRatio by remember { mutableFloatStateOf(initialCropRegion?.right ?: 0.95f) }
  var bottomRatio by remember { mutableFloatStateOf(initialCropRegion?.bottom ?: 0.95f) }

  var canvasSize by remember { mutableStateOf(IntSize.Zero) }

  // Pan & Zoom
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0B0D13))
        .testTag("dialog_region_selector"),
      color = Color(0xFF0B0D13)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // TOP BAR
        Surface(
          color = Color(0xFF131722),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(onClick = onDismiss) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Cancel", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Select Region for AI",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Focus on specific question, table or text area",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }

            Button(
              onClick = {
                val region = ImageCropRegion(
                  left = leftRatio.coerceIn(0f, 1f),
                  top = topRatio.coerceIn(0f, 1f),
                  right = rightRatio.coerceIn(0f, 1f),
                  bottom = bottomRatio.coerceIn(0f, 1f),
                  rotationDegrees = rotationDegrees
                )
                onRegionSelected(region)
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_confirm_region")
            ) {
              Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }

        // WORKSPACE CANVAS
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF090B0F))
            .onGloballyPositioned { coordinates ->
              canvasSize = coordinates.size
            },
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                  panOffset = if (zoomScale > 1f) panOffset + pan else Offset.Zero
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Image(
              bitmap = sourceBitmap.asImageBitmap(),
              contentDescription = "Source",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                  scaleX = zoomScale,
                  scaleY = zoomScale,
                  translationX = panOffset.x,
                  translationY = panOffset.y,
                  rotationZ = rotationDegrees.toFloat()
                )
            )

            // Crop Overlay with draggable corners
            if (canvasSize.width > 0 && canvasSize.height > 0) {
              val w = canvasSize.width.toFloat()
              val h = canvasSize.height.toFloat()

              Canvas(
                modifier = Modifier
                  .fillMaxSize()
                  .pointerInput(w, h) {
                    detectDragGestures { change, dragAmount ->
                      change.consume()
                      val dx = dragAmount.x / w
                      val dy = dragAmount.y / h
                      // Adjust right and bottom
                      rightRatio = (rightRatio + dx).coerceIn(leftRatio + 0.1f, 1f)
                      bottomRatio = (bottomRatio + dy).coerceIn(topRatio + 0.1f, 1f)
                    }
                  }
              ) {
                val rectX = leftRatio * size.width
                val rectY = topRatio * size.height
                val rectW = (rightRatio - leftRatio) * size.width
                val rectH = (bottomRatio - topRatio) * size.height

                // Dimmed surrounding background
                drawRect(
                  color = Color.Black.copy(alpha = 0.55f),
                  size = size
                )

                // Clear selection cutout
                drawRect(
                  color = Color.Transparent,
                  topLeft = Offset(rectX, rectY),
                  size = Size(rectW, rectH),
                  blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // Selection Border
                drawRect(
                  color = Color(0xFF38BDF8),
                  topLeft = Offset(rectX, rectY),
                  size = Size(rectW, rectH),
                  style = Stroke(width = 3f)
                )

                // Corner Handles
                val handleRadius = 12f
                drawCircle(Color.White, radius = handleRadius, center = Offset(rectX, rectY))
                drawCircle(Color.White, radius = handleRadius, center = Offset(rectX + rectW, rectY))
                drawCircle(Color.White, radius = handleRadius, center = Offset(rectX, rectY + rectH))
                drawCircle(Color(0xFF38BDF8), radius = handleRadius + 2f, center = Offset(rectX + rectW, rectY + rectH))
              }
            }
          }

          // Tip pill
          Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 16.dp)
          ) {
            Text(
              text = "Drag bottom-right corner to resize • Pinch to zoom",
              color = Color(0xFFE2E8F0),
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }
        }

        // BOTTOM CONTROLS
        Surface(
          color = Color(0xFF131722),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Reset to Full
            TextButton(
              onClick = {
                leftRatio = 0f
                topRatio = 0f
                rightRatio = 1f
                bottomRatio = 1f
                rotationDegrees = 0
                zoomScale = 1f
                panOffset = Offset.Zero
              }
            ) {
              Icon(Icons.Rounded.CropFree, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Full Image", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }

            // Quick Question 5 Preset (Center rectangle)
            OutlinedButton(
              onClick = {
                leftRatio = 0.1f
                topRatio = 0.25f
                rightRatio = 0.9f
                bottomRatio = 0.65f
              },
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Focus Portion (e.g. Q5)", fontSize = 12.sp)
            }

            // Rotate 90°
            IconButton(
              onClick = {
                rotationDegrees = (rotationDegrees + 90) % 360
              }
            ) {
              Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90", tint = Color.White)
            }
          }
        }
      }
    }
  }
}
