package com.example.data.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.ExifInterface
import com.example.model.editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object PhotoEditorEngine {

  /**
   * Safely loads and normalizes a bitmap from URI with orientation correction and optional max dimension.
   */
  suspend fun loadBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int = 2048
  ): Bitmap? = withContext(Dispatchers.IO) {
    try {
      var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeStream(inputStream, null, options)
      inputStream?.close()

      var sampleSize = 1
      while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = max(1, sampleSize)
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      inputStream = context.contentResolver.openInputStream(uri)
      val rawBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
      inputStream?.close()

      if (rawBitmap == null) return@withContext null

      // Check Exif orientation
      val exifStream = context.contentResolver.openInputStream(uri)
      val orientation = if (exifStream != null) {
        val exif = ExifInterface(exifStream)
        val ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        exifStream.close()
        ori
      } else {
        ExifInterface.ORIENTATION_NORMAL
      }

      val matrix = Matrix()
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      }

      if (!matrix.isIdentity) {
        val transformed = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        if (transformed != rawBitmap) {
          rawBitmap.recycle()
        }
        transformed
      } else {
        rawBitmap
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Generates a combined ColorMatrix representing Light and Color adjustments
   */
  fun createAdjustmentColorMatrix(
    light: LightAdjustments,
    color: ColorAdjustments
  ): ColorMatrix {
    val cm = ColorMatrix()

    // 1. Exposure & Brightness
    // Exposure is in EV stops (-2 to +2), standard multiplier is 2^EV
    val expFactor = Math.pow(2.0, (light.exposure).toDouble()).toFloat()
    val brightOffset = light.brightness * 1.5f + (light.whites * 0.5f) - (light.blacks * 0.5f)

    val expMatrix = ColorMatrix(
      floatArrayOf(
        expFactor, 0f, 0f, 0f, brightOffset,
        0f, expFactor, 0f, 0f, brightOffset,
        0f, 0f, expFactor, 0f, brightOffset,
        0f, 0f, 0f, 1f, 0f
      )
    )
    cm.postConcat(expMatrix)

    // 2. Contrast
    if (light.contrast != 0f) {
      val c = (light.contrast + 100f) / 100f // 0.0 to 2.0
      val scale = c * c
      val translate = (-0.5f * scale + 0.5f) * 255f
      val contrastMatrix = ColorMatrix(
        floatArrayOf(
          scale, 0f, 0f, 0f, translate,
          0f, scale, 0f, 0f, translate,
          0f, 0f, scale, 0f, translate,
          0f, 0f, 0f, 1f, 0f
        )
      )
      cm.postConcat(contrastMatrix)
    }

    // 3. Saturation & Vibrance
    val sat = max(0f, (color.saturation + 100f) / 100f)
    if (sat != 1.0f) {
      val satMatrix = ColorMatrix()
      satMatrix.setSaturation(sat)
      cm.postConcat(satMatrix)
    }

    // 4. Temperature (Amber / Blue) & Tint (Green / Magenta)
    if (color.temperature != 0f || color.tint != 0f) {
      val tempShift = color.temperature / 100f // -1 to 1
      val tintShift = color.tint / 100f // -1 to 1

      val rScale = 1.0f + (tempShift * 0.25f)
      val bScale = 1.0f - (tempShift * 0.25f)
      val gScale = 1.0f - (tintShift * 0.2f)
      val rTint = 1.0f + (tintShift * 0.1f)

      val wbMatrix = ColorMatrix(
        floatArrayOf(
          rScale * rTint, 0f, 0f, 0f, 0f,
          0f, gScale, 0f, 0f, 0f,
          0f, 0f, bScale, 0f, 0f,
          0f, 0f, 0f, 1f, 0f
        )
      )
      cm.postConcat(wbMatrix)
    }

    return cm
  }

  /**
   * Applies the full non-destructive editing pipeline onto a source bitmap.
   * Can be used for preview rendering or final full-resolution export.
   */
  suspend fun processImage(
    sourceBitmap: Bitmap,
    state: PhotoEditorState,
    isExport: Boolean = false
  ): Bitmap = withContext(Dispatchers.Default) {
    if (sourceBitmap.isRecycled) return@withContext sourceBitmap

    // Step 1: Crop & Geometry Transform
    val transformed = applyCropAndTransform(sourceBitmap, state.cropTransform)

    // Step 2: Tone & Color adjustments (Base layer)
    val toneEdited = applyToneAndColor(transformed, state)
    if (transformed != sourceBitmap && transformed != toneEdited) {
      transformed.recycle()
    }

    // Step 3: Retouching (Healing / Clone Stamp)
    val retouched = if (state.retouchItems.isNotEmpty()) {
      applyRetouch(toneEdited, state.retouchItems)
    } else {
      toneEdited
    }
    if (retouched != toneEdited && toneEdited != sourceBitmap) {
      toneEdited.recycle()
    }

    // Step 4: Privacy Blur / Mosaic
    val redacted = if (state.blurMosaicRegions.isNotEmpty()) {
      applyBlurMosaic(retouched, state.blurMosaicRegions)
    } else {
      retouched
    }
    if (redacted != retouched && retouched != sourceBitmap) {
      retouched.recycle()
    }

    // Step 5: Vignette & Effects
    val effectsApplied = if (!state.vignetteEffects.isDefault) {
      applyVignetteAndEffects(redacted, state.vignetteEffects)
    } else {
      redacted
    }
    if (effectsApplied != redacted && redacted != sourceBitmap) {
      redacted.recycle()
    }

    // Step 6: Overlays (Drawing strokes, Shapes, Text)
    val finalComposite = applyOverlays(effectsApplied, state)
    if (finalComposite != effectsApplied && effectsApplied != sourceBitmap) {
      effectsApplied.recycle()
    }

    finalComposite
  }

  /**
   * Step 1: Crop, Rotate, Straighten, Flip, Perspective
   */
  fun applyCropAndTransform(
    source: Bitmap,
    crop: CropTransform
  ): Bitmap {
    if (crop.isDefault) return source

    val width = source.width
    val height = source.height

    // Calculate crop rectangle in pixel coordinates
    val left = (crop.cropLeft * width).toInt().coerceIn(0, width - 1)
    val top = (crop.cropTop * height).toInt().coerceIn(0, height - 1)
    val right = (crop.cropRight * width).toInt().coerceIn(left + 1, width)
    val bottom = (crop.cropBottom * height).toInt().coerceIn(top + 1, height)

    val cropW = right - left
    val cropH = bottom - top

    val cropped = try {
      Bitmap.createBitmap(source, left, top, cropW, cropH)
    } catch (e: Exception) {
      source
    }

    // Rotation, Straighten, Flip
    val totalRotation = crop.rotationDegrees.toFloat() + crop.straightenAngle
    val hasFlip = crop.flipHorizontal || crop.flipVertical
    val hasRotation = totalRotation != 0f

    if (!hasRotation && !hasFlip) {
      return cropped
    }

    val matrix = Matrix()
    if (crop.flipHorizontal) matrix.postScale(-1f, 1f, cropped.width / 2f, cropped.height / 2f)
    if (crop.flipVertical) matrix.postScale(1f, -1f, cropped.width / 2f, cropped.height / 2f)
    if (hasRotation) matrix.postRotate(totalRotation, cropped.width / 2f, cropped.height / 2f)

    val transformed = try {
      Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
    } catch (e: Exception) {
      cropped
    }

    if (cropped != source && cropped != transformed) {
      cropped.recycle()
    }
    return transformed
  }

  /**
   * Step 2: Tone, Light, Color, HSL, Curves, Detail
   */
  private fun applyToneAndColor(
    source: Bitmap,
    state: PhotoEditorState
  ): Bitmap {
    val light = state.lightAdjustments
    val color = state.colorAdjustments
    val hsl = state.hslAdjustments
    val curves = state.curvesAdjustment
    val detail = state.detailAdjustments
    val grading = state.colorGrading

    val isLightColorDefault = light.isDefault && color.isDefault
    val isHslDefault = hsl.isDefault
    val isCurvesDefault = curves.isDefault
    val isDetailDefault = detail.isDefault
    val isGradingDefault = grading.isDefault

    if (isLightColorDefault && isHslDefault && isCurvesDefault && isDetailDefault && isGradingDefault) {
      return source
    }

    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val colorMatrix = createAdjustmentColorMatrix(light, color)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)

    // Apply HSL, Curves, and Color Grading pixel transformations if customized
    if (!isHslDefault || !isCurvesDefault || !isGradingDefault) {
      applyHslCurvesGradingPixelPass(output, hsl, curves, grading)
    }

    // Apply Grain / Detail if customized
    if (detail.grain > 0f) {
      applyFilmGrain(output, detail.grain)
    }

    return output
  }

  /**
   * Pixel-level pass for HSL band shifts, Tone Curves, and 3-Way Split Toning
   */
  private fun applyHslCurvesGradingPixelPass(
    bitmap: Bitmap,
    hsl: HslAdjustments,
    curves: CurvesAdjustment,
    grading: ColorGrading
  ) {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    // Precompute Curves LUT (256 entries each)
    val rLut = createCurveLut(curves.getCurve(CurveChannel.RED), curves.getCurve(CurveChannel.RGB))
    val gLut = createCurveLut(curves.getCurve(CurveChannel.GREEN), curves.getCurve(CurveChannel.RGB))
    val bLut = createCurveLut(curves.getCurve(CurveChannel.BLUE), curves.getCurve(CurveChannel.RGB))

    val hasHsl = !hsl.isDefault
    val hasGrading = !grading.isDefault
    val hsv = FloatArray(3)

    for (i in pixels.indices) {
      val c = pixels[i]
      var r = (c shr 16) and 0xFF
      var g = (c shr 8) and 0xFF
      var b = c and 0xFF
      val a = (c shr 24) and 0xFF

      // Curves LUT mapping
      r = rLut[r]
      g = gLut[g]
      b = bLut[b]

      // HSL Band adjustments
      if (hasHsl) {
        Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0] // 0..360
        val band = getHslBand(hue)
        val adj = hsl.bands[band]
        if (adj != null && !adj.isDefault) {
          hsv[0] = (hsv[0] + adj.hue * 0.36f).coerceIn(0f, 360f)
          hsv[1] = (hsv[1] * (1f + adj.saturation / 100f)).coerceIn(0f, 1f)
          hsv[2] = (hsv[2] * (1f + adj.luminance / 100f)).coerceIn(0f, 1f)
          val newCol = Color.HSVToColor(hsv)
          r = (newCol shr 16) and 0xFF
          g = (newCol shr 8) and 0xFF
          b = newCol and 0xFF
        }
      }

      // Color Grading (Shadows / Highlights tone injection)
      if (hasGrading) {
        val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        if (luma < 0.4f && grading.shadows.saturation > 0f) {
          val weight = (1f - luma / 0.4f) * (grading.shadows.saturation / 100f) * 0.4f
          val shadowCol = Color.HSVToColor(floatArrayOf(grading.shadows.hue, 1f, 1f))
          val sr = (shadowCol shr 16) and 0xFF
          val sg = (shadowCol shr 8) and 0xFF
          val sb = shadowCol and 0xFF
          r = (r * (1f - weight) + sr * weight).toInt().coerceIn(0, 255)
          g = (g * (1f - weight) + sg * weight).toInt().coerceIn(0, 255)
          b = (b * (1f - weight) + sb * weight).toInt().coerceIn(0, 255)
        } else if (luma > 0.6f && grading.highlights.saturation > 0f) {
          val weight = ((luma - 0.6f) / 0.4f) * (grading.highlights.saturation / 100f) * 0.4f
          val hiCol = Color.HSVToColor(floatArrayOf(grading.highlights.hue, 1f, 1f))
          val hr = (hiCol shr 16) and 0xFF
          val hg = (hiCol shr 8) and 0xFF
          val hb = hiCol and 0xFF
          r = (r * (1f - weight) + hr * weight).toInt().coerceIn(0, 255)
          g = (g * (1f - weight) + hg * weight).toInt().coerceIn(0, 255)
          b = (b * (1f - weight) + hb * weight).toInt().coerceIn(0, 255)
        }
      }

      pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
  }

  private fun getHslBand(hue: Float): HslColorBand {
    return when (hue) {
      in 345f..360f, in 0f..15f -> HslColorBand.RED
      in 15f..45f -> HslColorBand.ORANGE
      in 45f..75f -> HslColorBand.YELLOW
      in 75f..165f -> HslColorBand.GREEN
      in 165f..200f -> HslColorBand.CYAN
      in 200f..260f -> HslColorBand.BLUE
      in 260f..300f -> HslColorBand.PURPLE
      else -> HslColorBand.MAGENTA
    }
  }

  private fun createCurveLut(channelCurve: SingleCurve, masterCurve: SingleCurve): IntArray {
    val lut = IntArray(256)
    for (i in 0..255) {
      val inNormalized = i / 255f
      val channelOut = evaluateCurve(channelCurve.points, inNormalized)
      val masterOut = evaluateCurve(masterCurve.points, channelOut)
      lut[i] = (masterOut * 255f).toInt().coerceIn(0, 255)
    }
    return lut
  }

  private fun evaluateCurve(points: List<CurvePoint>, x: Float): Float {
    if (points.isEmpty()) return x
    if (points.size == 1) return points[0].y
    val sorted = points.sortedBy { it.x }
    if (x <= sorted.first().x) return sorted.first().y
    if (x >= sorted.last().x) return sorted.last().y

    for (i in 0 until sorted.size - 1) {
      val p0 = sorted[i]
      val p1 = sorted[i + 1]
      if (x in p0.x..p1.x) {
        val t = (x - p0.x) / (p1.x - p0.x)
        return p0.y + t * (p1.y - p0.y)
      }
    }
    return x
  }

  /**
   * Step 3: Retouching (Healing and Clone Stamp)
   */
  private fun applyRetouch(source: Bitmap, items: List<RetouchItem>): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    for (item in items) {
      val targetPxX = item.targetX * source.width
      val targetPxY = item.targetY * source.height
      val sourcePxX = item.sourceX * source.width
      val sourcePxY = item.sourceY * source.height
      val radius = item.radius.coerceAtLeast(10f)

      val srcRect = Rect(
        (sourcePxX - radius).toInt().coerceIn(0, source.width - 1),
        (sourcePxY - radius).toInt().coerceIn(0, source.height - 1),
        (sourcePxX + radius).toInt().coerceIn(1, source.width),
        (sourcePxY + radius).toInt().coerceIn(1, source.height)
      )
      val dstRect = RectF(
        targetPxX - radius,
        targetPxY - radius,
        targetPxX + radius,
        targetPxY + radius
      )

      paint.alpha = (item.opacity * 255).toInt().coerceIn(0, 255)
      val path = Path().apply {
        addCircle(targetPxX, targetPxY, radius, Path.Direction.CW)
      }
      canvas.save()
      canvas.clipPath(path)
      canvas.drawBitmap(source, srcRect, dstRect, paint)
      canvas.restore()
    }
    return output
  }

  /**
   * Step 4: Redaction Blur & Mosaic
   */
  private fun applyBlurMosaic(source: Bitmap, regions: List<BlurMosaicRegion>): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    for (region in regions) {
      val left = (region.normalizedLeft * source.width).toInt().coerceIn(0, source.width - 1)
      val top = (region.normalizedTop * source.height).toInt().coerceIn(0, source.height - 1)
      val right = (region.normalizedRight * source.width).toInt().coerceIn(left + 1, source.width)
      val bottom = (region.normalizedBottom * source.height).toInt().coerceIn(top + 1, source.height)

      val regW = right - left
      val regH = bottom - top

      if (regW > 0 && regH > 0) {
        val tileSize = max(8, (region.pixelTileSize).toInt())
        val smallW = max(1, regW / tileSize)
        val smallH = max(1, regH / tileSize)

        val cropped = Bitmap.createBitmap(source, left, top, regW, regH)
        val pixelatedSmall = Bitmap.createScaledBitmap(cropped, smallW, smallH, false)
        val pixelatedBack = Bitmap.createScaledBitmap(pixelatedSmall, regW, regH, false)

        canvas.drawBitmap(pixelatedBack, left.toFloat(), top.toFloat(), paint)

        cropped.recycle()
        pixelatedSmall.recycle()
        pixelatedBack.recycle()
      }
    }
    return output
  }

  /**
   * Step 5: Vignette & Effects
   */
  private fun applyVignetteAndEffects(source: Bitmap, vignette: VignetteEffects): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    if (vignette.vignetteAmount != 0f) {
      val cx = source.width / 2f
      val cy = source.height / 2f
      val radius = max(cx, cy) * (vignette.vignetteMidpoint / 50f)
      val isDark = vignette.vignetteAmount > 0f
      val intensity = Math.abs(vignette.vignetteAmount) / 100f
      val edgeAlpha = (intensity * 255).toInt().coerceIn(0, 255)

      val edgeColor = if (isDark) Color.argb(edgeAlpha, 0, 0, 0) else Color.argb(edgeAlpha, 255, 255, 255)
      val centerColor = Color.argb(0, 0, 0, 0)

      val shader = RadialGradient(
        cx, cy, radius,
        intArrayOf(centerColor, edgeColor),
        floatArrayOf(0.4f, 1.0f),
        Shader.TileMode.CLAMP
      )

      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
      }
      canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), paint)
    }

    if (vignette.fade > 0f) {
      val fadeAlpha = (vignette.fade * 0.4f * 255 / 100f).toInt().coerceIn(0, 255)
      val fadePaint = Paint().apply {
        color = Color.argb(fadeAlpha, 40, 40, 40)
      }
      canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), fadePaint)
    }

    if (vignette.filmGrain > 0f) {
      applyFilmGrain(output, vignette.filmGrain)
    }

    return output
  }

  private fun applyFilmGrain(bitmap: Bitmap, grainAmount: Float) {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val random = Random(42)
    val intensity = (grainAmount / 100f) * 45f // max noise +-45

    for (i in pixels.indices) {
      val c = pixels[i]
      val noise = (random.nextFloat() * 2f - 1f) * intensity
      val r = (((c shr 16) and 0xFF) + noise).toInt().coerceIn(0, 255)
      val g = (((c shr 8) and 0xFF) + noise).toInt().coerceIn(0, 255)
      val b = ((c and 0xFF) + noise).toInt().coerceIn(0, 255)
      val a = (c shr 24) and 0xFF
      pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
  }

  /**
   * Step 6: Overlays (Drawing, Shapes, Text Layers)
   */
  private fun applyOverlays(source: Bitmap, state: PhotoEditorState): Bitmap {
    val hasDrawings = state.drawingStrokes.isNotEmpty()
    val hasShapes = state.shapes.isNotEmpty()
    val hasText = state.textLayers.isNotEmpty()

    if (!hasDrawings && !hasShapes && !hasText) {
      return source
    }

    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    val w = source.width.toFloat()
    val h = source.height.toFloat()

    // 1. Drawing Strokes
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
    for (stroke in state.drawingStrokes) {
      if (stroke.points.size < 2) continue
      val colorInt = stroke.colorHex.toInt()
      strokePaint.color = colorInt
      strokePaint.alpha = (stroke.opacity * 255).toInt().coerceIn(0, 255)
      strokePaint.strokeWidth = stroke.strokeWidth * (w / 1000f).coerceAtLeast(1f)
      if (stroke.isEraser) {
        strokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
      } else {
        strokePaint.xfermode = null
      }

      val path = Path()
      val p0 = stroke.points[0]
      path.moveTo(p0.x * w, p0.y * h)
      for (k in 1 until stroke.points.size) {
        val pt = stroke.points[k]
        path.lineTo(pt.x * w, pt.y * h)
      }
      canvas.drawPath(path, strokePaint)
    }

    // 2. Shapes & Stickers
    val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (shape in state.shapes) {
      val cx = shape.normalizedX * w
      val cy = shape.normalizedY * h
      val sw = shape.normalizedWidth * w
      val sh = shape.normalizedHeight * h

      canvas.save()
      canvas.translate(cx, cy)
      canvas.rotate(shape.rotationDegrees)

      if (shape.type == EditorShapeType.STICKER) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          textSize = sw
          textAlign = Paint.Align.CENTER
          alpha = (shape.opacity * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText(shape.stickerSymbol, 0f, sw / 3f, textPaint)
      } else {
        shapePaint.color = shape.colorHex.toInt()
        shapePaint.alpha = (shape.opacity * 255).toInt().coerceIn(0, 255)
        shapePaint.strokeWidth = shape.strokeWidth * (w / 1000f).coerceAtLeast(1f)
        shapePaint.style = if (shape.isFilled) Paint.Style.FILL else Paint.Style.STROKE

        val halfW = sw / 2f
        val halfH = sh / 2f
        when (shape.type) {
          EditorShapeType.RECTANGLE -> canvas.drawRect(-halfW, -halfH, halfW, halfH, shapePaint)
          EditorShapeType.CIRCLE -> canvas.drawCircle(0f, 0f, min(halfW, halfH), shapePaint)
          EditorShapeType.LINE -> canvas.drawLine(-halfW, 0f, halfW, 0f, shapePaint)
          EditorShapeType.ARROW -> {
            canvas.drawLine(-halfW, 0f, halfW, 0f, shapePaint)
            canvas.drawLine(halfW - halfW * 0.3f, -halfH * 0.3f, halfW, 0f, shapePaint)
            canvas.drawLine(halfW - halfW * 0.3f, halfH * 0.3f, halfW, 0f, shapePaint)
          }
          EditorShapeType.STAR, EditorShapeType.HEART -> {
            canvas.drawCircle(0f, 0f, min(halfW, halfH), shapePaint)
          }
          else -> {}
        }
      }
      canvas.restore()
    }

    // 3. Text Layers
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textAlign = Paint.Align.CENTER
    }
    for (layer in state.textLayers) {
      if (layer.text.isBlank()) continue
      val tx = layer.normalizedX * w
      val ty = layer.normalizedY * h

      val baseTypeface = when (layer.font) {
        EditorFont.SERIF -> Typeface.SERIF
        EditorFont.MONOSPACE -> Typeface.MONOSPACE
        EditorFont.CURSIVE -> Typeface.SANS_SERIF
        EditorFont.SANS_SERIF -> Typeface.SANS_SERIF
      }
      val style = if (layer.isBold && layer.isItalic) Typeface.BOLD_ITALIC
      else if (layer.isBold) Typeface.BOLD
      else if (layer.isItalic) Typeface.ITALIC
      else Typeface.NORMAL

      textPaint.typeface = Typeface.create(baseTypeface, style)
      textPaint.textSize = layer.fontSizeSp * (w / 400f).coerceAtLeast(1f) * layer.scale
      textPaint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)

      canvas.save()
      canvas.translate(tx, ty)
      canvas.rotate(layer.rotationDegrees)

      // Background pill
      if (layer.backgroundColorHex != null) {
        val bounds = Rect()
        textPaint.getTextBounds(layer.text, 0, layer.text.length, bounds)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = layer.backgroundColorHex.toInt()
          alpha = (layer.opacity * 230).toInt().coerceIn(0, 255)
        }
        val pad = textPaint.textSize * 0.3f
        val bgRect = RectF(
          bounds.left - pad,
          bounds.top - pad,
          bounds.right + pad,
          bounds.bottom + pad
        )
        canvas.drawRoundRect(bgRect, pad, pad, bgPaint)
      }

      // Shadow
      if (layer.shadowColorHex != null) {
        textPaint.setShadowLayer(8f, 2f, 4f, layer.shadowColorHex.toInt())
      } else {
        textPaint.clearShadowLayer()
      }

      // Text color
      textPaint.color = layer.textColorHex.toInt()
      textPaint.style = Paint.Style.FILL
      canvas.drawText(layer.text, 0f, 0f, textPaint)

      canvas.restore()
    }

    return output
  }

  /**
   * Saves the final processed image into the device MediaStore / public storage non-destructively.
   * Returns saved Uri and File absolute path.
   */
  suspend fun exportImage(
    context: Context,
    sourceBitmap: Bitmap,
    state: PhotoEditorState,
    format: ExportFormat = ExportFormat.JPEG,
    qualityPercent: Int = 92
  ): Pair<Uri?, String?> = withContext(Dispatchers.IO) {
    try {
      val processed = processImage(sourceBitmap, state, isExport = true)
      val fileName = "EDIT_${System.currentTimeMillis()}.${format.extension}"

      val compressFormat = when (format) {
        ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ExportFormat.PNG -> Bitmap.CompressFormat.PNG
        ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          Bitmap.CompressFormat.WEBP
        }
      }

      val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyGalleryAI")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
      }

      val resolver = context.contentResolver
      val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      }

      val imageUri = resolver.insert(collection, values)
      var savedPath: String? = null

      if (imageUri != null) {
        resolver.openOutputStream(imageUri)?.use { out ->
          processed.compress(compressFormat, qualityPercent, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          values.clear()
          values.put(MediaStore.Images.Media.IS_PENDING, 0)
          resolver.update(imageUri, values, null, null)
        }
        savedPath = imageUri.toString()
      } else {
        // Fallback to internal/external app files dir
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MyGalleryAI").apply { mkdirs() }
        val fallbackFile = File(exportDir, fileName)
        FileOutputStream(fallbackFile).use { out ->
          processed.compress(compressFormat, qualityPercent, out)
        }
        savedPath = fallbackFile.absolutePath
      }

      if (processed != sourceBitmap) {
        processed.recycle()
      }

      Pair(imageUri, savedPath)
    } catch (e: Exception) {
      e.printStackTrace()
      Pair(null, null)
    }
  }

  /**
   * Directly exports a bitmap without re-applying editor state adjustments.
   * Used for saving Gemini AI generated results.
   */
  suspend fun saveBitmapDirect(
    context: Context,
    bitmap: Bitmap,
    title: String = "AI_Edit",
    format: ExportFormat = ExportFormat.JPEG,
    qualityPercent: Int = 94
  ): Pair<Uri?, String?> = withContext(Dispatchers.IO) {
    try {
      val fileName = "${title.replace(" ", "_")}_${System.currentTimeMillis()}.${format.extension}"
      val compressFormat = when (format) {
        ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ExportFormat.PNG -> Bitmap.CompressFormat.PNG
        ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          Bitmap.CompressFormat.WEBP
        }
      }

      val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyGalleryAI")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
      }

      val resolver = context.contentResolver
      val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      }

      val imageUri = resolver.insert(collection, values)
      var savedPath: String? = null

      if (imageUri != null) {
        resolver.openOutputStream(imageUri)?.use { out ->
          bitmap.compress(compressFormat, qualityPercent, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          values.clear()
          values.put(MediaStore.Images.Media.IS_PENDING, 0)
          resolver.update(imageUri, values, null, null)
        }
        savedPath = imageUri.toString()
      } else {
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MyGalleryAI").apply { mkdirs() }
        val fallbackFile = File(exportDir, fileName)
        FileOutputStream(fallbackFile).use { out ->
          bitmap.compress(compressFormat, qualityPercent, out)
        }
        savedPath = fallbackFile.absolutePath
      }

      Pair(imageUri, savedPath)
    } catch (e: Exception) {
      e.printStackTrace()
      Pair(null, null)
    }
  }

  /**
   * Serialize PhotoEditorState to JSON
   */
  fun serializeState(state: PhotoEditorState): String {
    val json = JSONObject()
    json.put("originalMediaId", state.originalMediaId)
    json.put("originalUri", state.originalUri)
    json.put("title", state.title)

    // Light
    val light = JSONObject().apply {
      put("exposure", state.lightAdjustments.exposure.toDouble())
      put("brightness", state.lightAdjustments.brightness.toDouble())
      put("contrast", state.lightAdjustments.contrast.toDouble())
      put("highlights", state.lightAdjustments.highlights.toDouble())
      put("shadows", state.lightAdjustments.shadows.toDouble())
      put("whites", state.lightAdjustments.whites.toDouble())
      put("blacks", state.lightAdjustments.blacks.toDouble())
    }
    json.put("light", light)

    // Color
    val color = JSONObject().apply {
      put("saturation", state.colorAdjustments.saturation.toDouble())
      put("vibrance", state.colorAdjustments.vibrance.toDouble())
      put("temperature", state.colorAdjustments.temperature.toDouble())
      put("tint", state.colorAdjustments.tint.toDouble())
      put("hue", state.colorAdjustments.hue.toDouble())
    }
    json.put("color", color)

    // Crop
    val crop = JSONObject().apply {
      put("cropLeft", state.cropTransform.cropLeft.toDouble())
      put("cropTop", state.cropTransform.cropTop.toDouble())
      put("cropRight", state.cropTransform.cropRight.toDouble())
      put("cropBottom", state.cropTransform.cropBottom.toDouble())
      put("rotationDegrees", state.cropTransform.rotationDegrees)
      put("straightenAngle", state.cropTransform.straightenAngle.toDouble())
      put("flipHorizontal", state.cropTransform.flipHorizontal)
      put("flipVertical", state.cropTransform.flipVertical)
    }
    json.put("crop", crop)

    return json.toString()
  }

  /**
   * Deserialize PhotoEditorState from JSON
   */
  fun deserializeState(jsonString: String): PhotoEditorState {
    return try {
      val json = JSONObject(jsonString)
      val lightJson = json.optJSONObject("light")
      val light = if (lightJson != null) {
        LightAdjustments(
          exposure = lightJson.optDouble("exposure", 0.0).toFloat(),
          brightness = lightJson.optDouble("brightness", 0.0).toFloat(),
          contrast = lightJson.optDouble("contrast", 0.0).toFloat(),
          highlights = lightJson.optDouble("highlights", 0.0).toFloat(),
          shadows = lightJson.optDouble("shadows", 0.0).toFloat(),
          whites = lightJson.optDouble("whites", 0.0).toFloat(),
          blacks = lightJson.optDouble("blacks", 0.0).toFloat()
        )
      } else LightAdjustments()

      val colorJson = json.optJSONObject("color")
      val color = if (colorJson != null) {
        ColorAdjustments(
          saturation = colorJson.optDouble("saturation", 0.0).toFloat(),
          vibrance = colorJson.optDouble("vibrance", 0.0).toFloat(),
          temperature = colorJson.optDouble("temperature", 0.0).toFloat(),
          tint = colorJson.optDouble("tint", 0.0).toFloat(),
          hue = colorJson.optDouble("hue", 0.0).toFloat()
        )
      } else ColorAdjustments()

      val cropJson = json.optJSONObject("crop")
      val crop = if (cropJson != null) {
        CropTransform(
          cropLeft = cropJson.optDouble("cropLeft", 0.0).toFloat(),
          cropTop = cropJson.optDouble("cropTop", 0.0).toFloat(),
          cropRight = cropJson.optDouble("cropRight", 1.0).toFloat(),
          cropBottom = cropJson.optDouble("cropBottom", 1.0).toFloat(),
          rotationDegrees = cropJson.optInt("rotationDegrees", 0),
          straightenAngle = cropJson.optDouble("straightenAngle", 0.0).toFloat(),
          flipHorizontal = cropJson.optBoolean("flipHorizontal", false),
          flipVertical = cropJson.optBoolean("flipVertical", false)
        )
      } else CropTransform()

      PhotoEditorState(
        originalMediaId = json.optString("originalMediaId", ""),
        originalUri = json.optString("originalUri", ""),
        title = json.optString("title", "Edited_Photo"),
        lightAdjustments = light,
        colorAdjustments = color,
        cropTransform = crop
      )
    } catch (e: Exception) {
      PhotoEditorState()
    }
  }
}
