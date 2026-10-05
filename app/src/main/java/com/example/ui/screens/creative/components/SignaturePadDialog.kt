package com.example.ui.screens.creative.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.creative.SignatureConfig
import com.example.model.creative.SignaturePoint
import com.example.model.creative.SignatureStroke
import com.example.ui.theme.GalleryTheme

@Composable
fun SignaturePadDialog(
  onSaveSignature: (SignatureConfig) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val strokes = remember { mutableStateListOf<SignatureStroke>() }
  var currentPoints = remember { mutableStateListOf<SignaturePoint>() }

  val penColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFF38BDF8),
    Color(0xFFF59E0B),
    Color(0xFFEF4444)
  )
  var selectedColor by remember { mutableStateOf(Color.White) }
  var strokeWidth by remember { mutableFloatStateOf(6f) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = colors.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .testTag("dialog_signature_pad")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Draw, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Draw Your Signature",
              color = colors.primaryText,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(
            onClick = {
              strokes.clear()
              currentPoints.clear()
            },
            enabled = strokes.isNotEmpty() || currentPoints.isNotEmpty()
          ) {
            Icon(Icons.Rounded.Delete, contentDescription = "Clear", tint = colors.error)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Drawing Canvas
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .pointerInput(selectedColor, strokeWidth) {
              detectDragGestures(
                onDragStart = { offset ->
                  currentPoints.clear()
                  currentPoints.add(SignaturePoint(offset.x, offset.y))
                },
                onDrag = { change, _ ->
                  change.consume()
                  currentPoints.add(SignaturePoint(change.position.x, change.position.y))
                },
                onDragEnd = {
                  if (currentPoints.isNotEmpty()) {
                    strokes.add(
                      SignatureStroke(
                        points = currentPoints.toList(),
                        color = selectedColor.toArgb(),
                        strokeWidth = strokeWidth
                      )
                    )
                    currentPoints.clear()
                  }
                },
                onDragCancel = {
                  currentPoints.clear()
                }
              )
            }
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            // Completed strokes
            for (stroke in strokes) {
              if (stroke.points.size > 1) {
                val path = Path()
                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                  path.lineTo(stroke.points[i].x, stroke.points[i].y)
                }
                drawPath(
                  path = path,
                  color = Color(stroke.color),
                  style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                  )
                )
              }
            }

            // In-progress stroke
            if (currentPoints.size > 1) {
              val currentPath = Path()
              currentPath.moveTo(currentPoints[0].x, currentPoints[0].y)
              for (i in 1 until currentPoints.size) {
                currentPath.lineTo(currentPoints[i].x, currentPoints[i].y)
              }
              drawPath(
                path = currentPath,
                color = selectedColor,
                style = Stroke(
                  width = strokeWidth,
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round
                )
              )
            }
          }

          if (strokes.isEmpty() && currentPoints.isEmpty()) {
            Text(
              text = "Sign here using your finger or stylus",
              color = Color.Gray,
              fontSize = 13.sp,
              modifier = Modifier.align(Alignment.Center)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Color Palette Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Pen Color:", color = colors.secondaryText, fontSize = 12.sp)

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            penColors.forEach { col ->
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(col)
                  .border(
                    width = if (selectedColor == col) 2.5.dp else 1.dp,
                    color = if (selectedColor == col) colors.accent else Color.Gray,
                    shape = CircleShape
                  )
                  .clickable { selectedColor = col }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", color = colors.secondaryText)
          }

          Button(
            onClick = {
              if (strokes.isNotEmpty()) {
                onSaveSignature(SignatureConfig(strokes = strokes.toList()))
              }
            },
            enabled = strokes.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier.weight(1.3f).testTag("btn_save_signature")
          ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Signature", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
