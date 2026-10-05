package com.example.ui.screens.resize.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PassportGuideOverlay(
  modifier: Modifier = Modifier,
  guideColor: Color = Color(0xFF38BDF8)
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Head oval occupies ~70-75% of height and 55% of width
    val ovalW = w * 0.52f
    val ovalH = h * 0.70f
    val ovalLeft = (w - ovalW) / 2f
    val ovalTop = h * 0.10f

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val guideStroke = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect)
    val solidStroke = Stroke(width = 1.5.dp.toPx())

    // 1. Biometric Head Oval
    drawOval(
      color = guideColor,
      topLeft = Offset(ovalLeft, ovalTop),
      size = Size(ovalW, ovalH),
      style = guideStroke
    )

    // 2. Vertical Central Symmetry Axis
    drawLine(
      color = guideColor.copy(alpha = 0.6f),
      start = Offset(w / 2f, 0f),
      end = Offset(w / 2f, h),
      strokeWidth = 1.dp.toPx(),
      pathEffect = dashEffect
    )

    // 3. Eye Level Guideline (~45% from top)
    val eyeY = h * 0.45f
    drawLine(
      color = guideColor.copy(alpha = 0.85f),
      start = Offset(ovalLeft - 16f, eyeY),
      end = Offset(ovalLeft + ovalW + 16f, eyeY),
      strokeWidth = 1.5.dp.toPx()
    )

    // 4. Chin Guideline (~80% from top)
    val chinY = ovalTop + ovalH
    drawLine(
      color = guideColor.copy(alpha = 0.85f),
      start = Offset(ovalLeft + (ovalW * 0.2f), chinY),
      end = Offset(ovalLeft + (ovalW * 0.8f), chinY),
      strokeWidth = 1.5.dp.toPx()
    )

    // 5. Crown Guideline (~10% from top)
    drawLine(
      color = guideColor.copy(alpha = 0.6f),
      start = Offset(ovalLeft + (ovalW * 0.25f), ovalTop),
      end = Offset(ovalLeft + (ovalW * 0.75f), ovalTop),
      strokeWidth = 1.dp.toPx()
    )
  }
}
