package com.example.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.editor.*
import kotlin.math.roundToInt

/**
 * Reusable labeled slider with zero detent and live value display
 */
@Composable
fun EditorSlider(
  label: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  displayFormatter: (Float) -> String = { "${it.roundToInt()}" },
  onReset: (() -> Unit)? = null
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = label,
        color = Color(0xFFE2E8F0),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = displayFormatter(value),
          color = if (value != 0f) Color(0xFF60A5FA) else Color(0xFF94A3B8),
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        if (value != 0f && onReset != null) {
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Rounded.RestartAlt,
            contentDescription = "Reset $label",
            tint = Color(0xFF94A3B8),
            modifier = Modifier
              .size(16.dp)
              .clickable { onReset() }
          )
        }
      }
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color(0xFF3B82F6),
        inactiveTrackColor = Color(0xFF334155)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(32.dp)
        .testTag("slider_${label.lowercase().replace(" ", "_")}")
    )
  }
}

/**
 * 1. CROP & TRANSFORM PANEL
 */
@Composable
fun CropTransformPanel(
  crop: CropTransform,
  onCropChange: (CropTransform) -> Unit,
  onResetCrop: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 12.dp)
  ) {
    // Aspect Ratio chips
    Text(
      text = "ASPECT RATIO",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      CropAspectRatio.values().forEach { ratio ->
        val isSelected = crop.aspectRatio == ratio
        FilterChip(
          selected = isSelected,
          onClick = {
            onCropChange(crop.copy(aspectRatio = ratio))
          },
          label = { Text(ratio.displayName, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF3B82F6),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF1E293B),
            labelColor = Color(0xFFCBD5E1)
          ),
          border = null
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Quick Rotation & Flip actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      OutlinedButton(
        onClick = {
          val next = (crop.rotationDegrees + 90) % 360
          onCropChange(crop.copy(rotationDegrees = next))
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)))
      ) {
        Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90°", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("+90°", fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = { onCropChange(crop.copy(flipHorizontal = !crop.flipHorizontal)) },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (crop.flipHorizontal) Color(0xFF60A5FA) else Color.White),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)))
      ) {
        Icon(Icons.Rounded.Flip, contentDescription = "Flip H", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Flip H", fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = { onCropChange(crop.copy(flipVertical = !crop.flipVertical)) },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (crop.flipVertical) Color(0xFF60A5FA) else Color.White),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)))
      ) {
        Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = "Flip V", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Flip V", fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Straighten Slider (-45 to +45)
    EditorSlider(
      label = "Straighten Angle",
      value = crop.straightenAngle,
      valueRange = -45f..45f,
      onValueChange = { onCropChange(crop.copy(straightenAngle = it)) },
      displayFormatter = { "${it.roundToInt()}°" },
      onReset = { onCropChange(crop.copy(straightenAngle = 0f)) }
    )

    // Reset button
    if (!crop.isDefault) {
      TextButton(
        onClick = onResetCrop,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(top = 4.dp)
      ) {
        Text("Reset All Transformations", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 2. LIGHT ADJUSTMENT PANEL
 */
@Composable
fun LightAdjustPanel(
  light: LightAdjustments,
  onLightChange: (LightAdjustments) -> Unit,
  onReset: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 8.dp)
  ) {
    EditorSlider(
      label = "Exposure",
      value = light.exposure,
      valueRange = -2f..2f,
      onValueChange = { onLightChange(light.copy(exposure = it)) },
      displayFormatter = { String.format(java.util.Locale.US, "%+.1f EV", it) },
      onReset = { onLightChange(light.copy(exposure = 0f)) }
    )
    EditorSlider(
      label = "Brightness",
      value = light.brightness,
      valueRange = -100f..100f,
      onValueChange = { onLightChange(light.copy(brightness = it)) },
      onReset = { onLightChange(light.copy(brightness = 0f)) }
    )
    EditorSlider(
      label = "Contrast",
      value = light.contrast,
      valueRange = -100f..100f,
      onValueChange = { onLightChange(light.copy(contrast = it)) },
      onReset = { onLightChange(light.copy(contrast = 0f)) }
    )
    EditorSlider(
      label = "Highlights",
      value = light.highlights,
      valueRange = -100f..100f,
      onValueChange = { onLightChange(light.copy(highlights = it)) },
      onReset = { onLightChange(light.copy(highlights = 0f)) }
    )
    EditorSlider(
      label = "Shadows",
      value = light.shadows,
      valueRange = -100f..100f,
      onValueChange = { onLightChange(light.copy(shadows = it)) },
      onReset = { onLightChange(light.copy(shadows = 0f)) }
    )
    EditorSlider(
      label = "Whites",
      value = light.whites,
      valueRange = -100f..100f,
      onValueChange = { onLightChange(light.copy(whites = it)) },
      onReset = { onLightChange(light.copy(whites = 0f)) }
    )
    EditorSlider(
      label = "Blacks",
      value = light.blacks,
      valueRange = -100f..100f,
      onValueChange = { onLightChange(light.copy(blacks = it)) },
      onReset = { onLightChange(light.copy(blacks = 0f)) }
    )

    if (!light.isDefault) {
      TextButton(
        onClick = onReset,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Reset Light Adjustments", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 3. COLOR ADJUSTMENT PANEL
 */
@Composable
fun ColorAdjustPanel(
  color: ColorAdjustments,
  onColorChange: (ColorAdjustments) -> Unit,
  onReset: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 8.dp)
  ) {
    EditorSlider(
      label = "Temperature (Cool / Warm)",
      value = color.temperature,
      valueRange = -100f..100f,
      onValueChange = { onColorChange(color.copy(temperature = it)) },
      onReset = { onColorChange(color.copy(temperature = 0f)) }
    )
    EditorSlider(
      label = "Tint (Green / Magenta)",
      value = color.tint,
      valueRange = -100f..100f,
      onValueChange = { onColorChange(color.copy(tint = it)) },
      onReset = { onColorChange(color.copy(tint = 0f)) }
    )
    EditorSlider(
      label = "Saturation",
      value = color.saturation,
      valueRange = -100f..100f,
      onValueChange = { onColorChange(color.copy(saturation = it)) },
      onReset = { onColorChange(color.copy(saturation = 0f)) }
    )
    EditorSlider(
      label = "Vibrance",
      value = color.vibrance,
      valueRange = -100f..100f,
      onValueChange = { onColorChange(color.copy(vibrance = it)) },
      onReset = { onColorChange(color.copy(vibrance = 0f)) }
    )
    EditorSlider(
      label = "Hue Shift",
      value = color.hue,
      valueRange = -180f..180f,
      onValueChange = { onColorChange(color.copy(hue = it)) },
      displayFormatter = { "${it.roundToInt()}°" },
      onReset = { onColorChange(color.copy(hue = 0f)) }
    )

    if (!color.isDefault) {
      TextButton(
        onClick = onReset,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Reset Color Adjustments", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 4. HSL PANEL (8 Color Ranges)
 */
@Composable
fun HslPanel(
  hsl: HslAdjustments,
  onHslChange: (HslAdjustments) -> Unit,
  onReset: () -> Unit
) {
  var selectedBand by remember { mutableStateOf(HslColorBand.RED) }
  val currentAdj = hsl.bands[selectedBand] ?: HslBandAdjustment()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 10.dp)
  ) {
    // 8 Color swatch tabs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      HslColorBand.values().forEach { band ->
        val isSelected = band == selectedBand
        val bandAdj = hsl.bands[band] ?: HslBandAdjustment()
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable { selectedBand = band }
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(band.baseColorHex))
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape
              )
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = band.displayName,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }

    Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

    // Sliders for selected band
    EditorSlider(
      label = "${selectedBand.displayName} Hue",
      value = currentAdj.hue,
      valueRange = -100f..100f,
      onValueChange = { v ->
        onHslChange(hsl.updateBand(selectedBand) { it.copy(hue = v) })
      },
      onReset = {
        onHslChange(hsl.updateBand(selectedBand) { it.copy(hue = 0f) })
      }
    )

    EditorSlider(
      label = "${selectedBand.displayName} Saturation",
      value = currentAdj.saturation,
      valueRange = -100f..100f,
      onValueChange = { v ->
        onHslChange(hsl.updateBand(selectedBand) { it.copy(saturation = v) })
      },
      onReset = {
        onHslChange(hsl.updateBand(selectedBand) { it.copy(saturation = 0f) })
      }
    )

    EditorSlider(
      label = "${selectedBand.displayName} Luminance",
      value = currentAdj.luminance,
      valueRange = -100f..100f,
      onValueChange = { v ->
        onHslChange(hsl.updateBand(selectedBand) { it.copy(luminance = v) })
      },
      onReset = {
        onHslChange(hsl.updateBand(selectedBand) { it.copy(luminance = 0f) })
      }
    )

    if (!hsl.isDefault) {
      TextButton(
        onClick = onReset,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Reset All HSL Bands", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 5. CURVES PANEL (Interactive Spline Graph)
 */
@Composable
fun CurvesPanel(
  curves: CurvesAdjustment,
  onCurvesChange: (CurvesAdjustment) -> Unit,
  onReset: () -> Unit
) {
  var activeChannel by remember { mutableStateOf(CurveChannel.RGB) }
  val currentCurve = curves.getCurve(activeChannel)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Channel selector buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      CurveChannel.values().forEach { channel ->
        val isSelected = channel == activeChannel
        OutlinedButton(
          onClick = { activeChannel = channel },
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(channel.colorHex).copy(alpha = 0.25f) else Color.Transparent,
            contentColor = Color(channel.colorHex)
          ),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
              if (isSelected) Color(channel.colorHex) else Color(0xFF334155)
            )
          )
        ) {
          Text(channel.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Interactive Curve Canvas (200x200 dp)
    Box(
      modifier = Modifier
        .size(220.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF0F172A))
        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
        .pointerInput(activeChannel, currentCurve) {
          detectTapGestures(
            onTap = { tapOffset ->
              val nx = (tapOffset.x / size.width).coerceIn(0f, 1f)
              val ny = 1f - (tapOffset.y / size.height).coerceIn(0f, 1f)
              // Add point if not close to existing
              val updated = (currentCurve.points + CurvePoint(nx, ny)).sortedBy { it.x }
              onCurvesChange(curves.updateCurve(activeChannel, SingleCurve(updated)))
            }
          )
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Grid lines
        for (i in 1..3) {
          val lineX = w * (i / 4f)
          val lineY = h * (i / 4f)
          drawLine(Color(0xFF1E293B), Offset(lineX, 0f), Offset(lineX, h), 1.dp.toPx())
          drawLine(Color(0xFF1E293B), Offset(0f, lineY), Offset(w, lineY), 1.dp.toPx())
        }

        // Diagonal baseline
        drawLine(Color(0xFF334155), Offset(0f, h), Offset(w, 0f), 1.dp.toPx())

        // Curve path
        val sorted = currentCurve.points.sortedBy { it.x }
        val curvePath = Path()
        for (step in 0..100) {
          val t = step / 100f
          val px = t * w
          // Linear interpolation for preview
          val py = (1f - evaluateCurvePoints(sorted, t)) * h
          if (step == 0) curvePath.moveTo(px, py) else curvePath.lineTo(px, py)
        }

        drawPath(
          path = curvePath,
          color = Color(activeChannel.colorHex),
          style = Stroke(width = 2.5.dp.toPx())
        )

        // Draw control points
        for (pt in currentCurve.points) {
          val cx = pt.x * w
          val cy = (1f - pt.y) * h
          drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(cx, cy))
          drawCircle(Color(activeChannel.colorHex), radius = 4.dp.toPx(), center = Offset(cx, cy))
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Tap graph to add control points", color = Color(0xFF64748B), fontSize = 11.sp)

    // Reset button
    TextButton(
      onClick = {
        onCurvesChange(curves.updateCurve(activeChannel, SingleCurve()))
      }
    ) {
      Text("Reset ${activeChannel.displayName} Curve", color = Color(0xFFEF4444), fontSize = 12.sp)
    }
  }
}

private fun evaluateCurvePoints(points: List<CurvePoint>, x: Float): Float {
  if (points.isEmpty()) return x
  if (points.size == 1) return points[0].y
  if (x <= points.first().x) return points.first().y
  if (x >= points.last().x) return points.last().y

  for (i in 0 until points.size - 1) {
    val p0 = points[i]
    val p1 = points[i + 1]
    if (x in p0.x..p1.x) {
      val t = (x - p0.x) / (p1.x - p0.x)
      return p0.y + t * (p1.y - p0.y)
    }
  }
  return x
}

/**
 * 6. DETAIL & EFFECTS PANEL
 */
@Composable
fun DetailEffectsPanel(
  detail: DetailAdjustments,
  vignette: VignetteEffects,
  onDetailChange: (DetailAdjustments) -> Unit,
  onVignetteChange: (VignetteEffects) -> Unit,
  onReset: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 8.dp)
  ) {
    Text(
      text = "SHARPNESS & CLARITY",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    EditorSlider(
      label = "Sharpness",
      value = detail.sharpness,
      valueRange = 0f..100f,
      onValueChange = { onDetailChange(detail.copy(sharpness = it)) },
      onReset = { onDetailChange(detail.copy(sharpness = 0f)) }
    )
    EditorSlider(
      label = "Clarity",
      value = detail.clarity,
      valueRange = -100f..100f,
      onValueChange = { onDetailChange(detail.copy(clarity = it)) },
      onReset = { onDetailChange(detail.copy(clarity = 0f)) }
    )
    EditorSlider(
      label = "Texture",
      value = detail.texture,
      valueRange = -100f..100f,
      onValueChange = { onDetailChange(detail.copy(texture = it)) },
      onReset = { onDetailChange(detail.copy(texture = 0f)) }
    )
    EditorSlider(
      label = "Dehaze",
      value = detail.dehaze,
      valueRange = -100f..100f,
      onValueChange = { onDetailChange(detail.copy(dehaze = it)) },
      onReset = { onDetailChange(detail.copy(dehaze = 0f)) }
    )
    EditorSlider(
      label = "Noise Reduction",
      value = detail.noiseReduction,
      valueRange = 0f..100f,
      onValueChange = { onDetailChange(detail.copy(noiseReduction = it)) },
      onReset = { onDetailChange(detail.copy(noiseReduction = 0f)) }
    )

    Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

    Text(
      text = "VIGNETTE & FILM EFFECTS",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    EditorSlider(
      label = "Vignette Amount",
      value = vignette.vignetteAmount,
      valueRange = -100f..100f,
      onValueChange = { onVignetteChange(vignette.copy(vignetteAmount = it)) },
      onReset = { onVignetteChange(vignette.copy(vignetteAmount = 0f)) }
    )
    EditorSlider(
      label = "Fade",
      value = vignette.fade,
      valueRange = 0f..100f,
      onValueChange = { onVignetteChange(vignette.copy(fade = it)) },
      onReset = { onVignetteChange(vignette.copy(fade = 0f)) }
    )
    EditorSlider(
      label = "Film Grain",
      value = vignette.filmGrain,
      valueRange = 0f..100f,
      onValueChange = { onVignetteChange(vignette.copy(filmGrain = it)) },
      onReset = { onVignetteChange(vignette.copy(filmGrain = 0f)) }
    )

    if (!detail.isDefault || !vignette.isDefault) {
      TextButton(
        onClick = onReset,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Reset Details & Effects", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 7. COLOR GRADING (3-Way Split Toning)
 */
@Composable
fun ColorGradingPanel(
  grading: ColorGrading,
  onGradingChange: (ColorGrading) -> Unit,
  onReset: () -> Unit
) {
  var selectedWheel by remember { mutableStateOf("Shadows") }
  val activeWheel = when (selectedWheel) {
    "Shadows" -> grading.shadows
    "Midtones" -> grading.midtones
    else -> grading.highlights
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 10.dp)
  ) {
    // 3 tabs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      listOf("Shadows", "Midtones", "Highlights").forEach { tone ->
        val isSelected = tone == selectedWheel
        FilterChip(
          selected = isSelected,
          onClick = { selectedWheel = tone },
          label = { Text(tone, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF3B82F6),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF1E293B),
            labelColor = Color(0xFFCBD5E1)
          ),
          border = null
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    EditorSlider(
      label = "$selectedWheel Hue",
      value = activeWheel.hue,
      valueRange = 0f..360f,
      onValueChange = { h ->
        val updated = activeWheel.copy(hue = h)
        onGradingChange(
          when (selectedWheel) {
            "Shadows" -> grading.copy(shadows = updated)
            "Midtones" -> grading.copy(midtones = updated)
            else -> grading.copy(highlights = updated)
          }
        )
      },
      displayFormatter = { "${it.roundToInt()}°" }
    )

    EditorSlider(
      label = "$selectedWheel Saturation",
      value = activeWheel.saturation,
      valueRange = 0f..100f,
      onValueChange = { s ->
        val updated = activeWheel.copy(saturation = s)
        onGradingChange(
          when (selectedWheel) {
            "Shadows" -> grading.copy(shadows = updated)
            "Midtones" -> grading.copy(midtones = updated)
            else -> grading.copy(highlights = updated)
          }
        )
      }
    )

    EditorSlider(
      label = "Global Balance (Shadow vs Highlight)",
      value = grading.balance,
      valueRange = -100f..100f,
      onValueChange = { onGradingChange(grading.copy(balance = it)) },
      onReset = { onGradingChange(grading.copy(balance = 0f)) }
    )

    if (!grading.isDefault) {
      TextButton(
        onClick = onReset,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Reset Color Grading", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 8. RETOUCH / HEALING PANEL
 */
@Composable
fun RetouchHealingPanel(
  retouchItems: List<RetouchItem>,
  currentRadius: Float,
  onRadiusChange: (Float) -> Unit,
  onClearAll: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(
      text = "SPOT HEALING & BLEMISH REMOVAL",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Tap any blemish or unwanted spot on the photo to heal it seamlessly.",
      color = Color(0xFFCBD5E1),
      fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    EditorSlider(
      label = "Brush Radius",
      value = currentRadius,
      valueRange = 10f..80f,
      onValueChange = onRadiusChange,
      displayFormatter = { "${it.roundToInt()} px" }
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "${retouchItems.size} spots healed",
        color = Color(0xFF10B981),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
      )
      if (retouchItems.isNotEmpty()) {
        TextButton(onClick = onClearAll) {
          Text("Clear All Spots", color = Color(0xFFEF4444), fontSize = 12.sp)
        }
      }
    }
  }
}

/**
 * 9. BLUR / MOSAIC PANEL
 */
@Composable
fun BlurMosaicPanel(
  regions: List<BlurMosaicRegion>,
  onAddRegion: (BlurMosaicType) -> Unit,
  onClearRegions: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(
      text = "PRIVACY REDACTION & MOSAIC",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Pixelate or blur faces, ID numbers, addresses, and sensitive details.",
      color = Color(0xFFCBD5E1),
      fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = { onAddRegion(BlurMosaicType.MOSAIC) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Pixelate", fontSize = 12.sp)
      }

      Button(
        onClick = { onAddRegion(BlurMosaicType.BLUR) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Rounded.BlurOn, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Blur", fontSize = 12.sp)
      }
    }

    if (regions.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(
        onClick = onClearRegions,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Clear All Privacy Regions (${regions.size})", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 10. DRAWING PANEL
 */
@Composable
fun DrawingPanel(
  currentColor: Long,
  strokeWidth: Float,
  onColorChange: (Long) -> Unit,
  onStrokeWidthChange: (Float) -> Unit,
  onUndoStroke: () -> Unit,
  onClearDrawing: () -> Unit,
  canUndoStroke: Boolean
) {
  val colors = listOf(
    0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759,
    0xFF00C7BE, 0xFF30B0C7, 0xFF007AFF, 0xFF5856D6,
    0xFFAF52DE, 0xFFFF2D55, 0xFFFFFFFF, 0xFF000000
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(
      text = "COLOR PALETTE",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      colors.forEach { col ->
        val isSelected = col == currentColor
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(col.toInt()))
            .border(
              width = if (isSelected) 3.dp else 1.dp,
              color = if (isSelected) Color.White else Color(0xFF334155),
              shape = CircleShape
            )
            .clickable { onColorChange(col) }
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    EditorSlider(
      label = "Stroke Width",
      value = strokeWidth,
      valueRange = 2f..40f,
      onValueChange = onStrokeWidthChange,
      displayFormatter = { "${it.roundToInt()} px" }
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      OutlinedButton(
        onClick = onUndoStroke,
        enabled = canUndoStroke,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)))
      ) {
        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo Stroke", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Undo Stroke", fontSize = 12.sp)
      }

      TextButton(onClick = onClearDrawing) {
        Text("Clear Drawing", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 11. TEXT LAYER PANEL
 */
@Composable
fun TextLayerPanel(
  textLayers: List<TextLayerItem>,
  onAddText: (String) -> Unit,
  onDeleteText: (String) -> Unit
) {
  var newTextValue by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(
      text = "ADD TEXT OVERLAY",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = newTextValue,
        onValueChange = { newTextValue = it },
        placeholder = { Text("Enter caption or text...", color = Color(0xFF64748B), fontSize = 13.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Color(0xFF3B82F6),
          unfocusedBorderColor = Color(0xFF334155),
          focusedContainerColor = Color(0xFF1E293B),
          unfocusedContainerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier.weight(1f)
      )

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = {
          if (newTextValue.isNotBlank()) {
            onAddText(newTextValue.trim())
            newTextValue = ""
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
      ) {
        Text("Add")
      }
    }

    if (textLayers.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      Text("Active Text Titles (${textLayers.size}):", color = Color(0xFF94A3B8), fontSize = 12.sp)
      Spacer(modifier = Modifier.height(6.dp))
      textLayers.forEach { layer ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(layer.text, color = Color.White, fontSize = 13.sp, maxLines = 1)
          IconButton(
            onClick = { onDeleteText(layer.id) },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete Text", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

/**
 * 12. SHAPES & STICKERS PANEL
 */
@Composable
fun ShapesStickersPanel(
  shapes: List<ShapeStickerItem>,
  onAddShape: (EditorShapeType) -> Unit,
  onAddSticker: (String) -> Unit,
  onClearShapes: () -> Unit
) {
  val stickers = listOf("⭐", "❤️", "🔥", "🏆", "✈️", "☀️", "🌸", "⚡", "💡", "📸", "🎉", "👑")

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(
      text = "ADD SHAPE",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        EditorShapeType.RECTANGLE to "Rectangle",
        EditorShapeType.CIRCLE to "Circle",
        EditorShapeType.ARROW to "Arrow",
        EditorShapeType.LINE to "Line"
      ).forEach { (shapeType, label) ->
        OutlinedButton(
          onClick = { onAddShape(shapeType) },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)))
        ) {
          Text(label, fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "ADD STICKER",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      stickers.forEach { emoji ->
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .clickable { onAddSticker(emoji) },
          contentAlignment = Alignment.Center
        ) {
          Text(emoji, fontSize = 20.sp)
        }
      }
    }

    if (shapes.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(
        onClick = onClearShapes,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Clear All Shapes (${shapes.size})", color = Color(0xFFEF4444), fontSize = 12.sp)
      }
    }
  }
}

/**
 * 13. LAYERS PANEL (Visibility & Opacity)
 */
@Composable
fun LayersPanel(
  layers: List<EditorLayer>,
  onToggleVisibility: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Text(
      text = "EDITING LAYERS",
      color = Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    layers.forEach { layer ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = when (layer.type) {
              LayerType.IMAGE_BASE -> Icons.Rounded.Image
              LayerType.ADJUSTMENT -> Icons.Rounded.Tune
              LayerType.DRAWING -> Icons.Rounded.Brush
              LayerType.TEXT -> Icons.Rounded.TextFields
              LayerType.SHAPE -> Icons.Rounded.Category
              LayerType.RETOUCH -> Icons.Rounded.Healing
              LayerType.PRIVACY -> Icons.Rounded.VisibilityOff
            },
            contentDescription = null,
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(layer.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        IconButton(
          onClick = { onToggleVisibility(layer.id) },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = if (layer.isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            contentDescription = "Toggle Visibility",
            tint = if (layer.isVisible) Color.White else Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
