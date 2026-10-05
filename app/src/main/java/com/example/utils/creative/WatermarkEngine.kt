package com.example.utils.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.model.creative.SignatureConfig
import com.example.model.creative.WatermarkConfig
import com.example.model.creative.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatermarkEngine {

  /**
   * Applies text or logo watermark onto a bitmap and returns a new watermarked Bitmap.
   */
  fun applyWatermark(
    bitmap: Bitmap,
    config: WatermarkConfig,
    logoBitmap: Bitmap? = null
  ): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val scaleFactor = result.width.toFloat() / 1000f
    val marginPx = config.marginDp * scaleFactor

    if (config.tileRepeat) {
      drawTiledWatermark(canvas, result.width, result.height, config, scaleFactor)
      return result
    }

    if (logoBitmap != null) {
      drawLogoWatermark(canvas, result.width, result.height, logoBitmap, config, marginPx, scaleFactor)
    } else if (config.text.isNotBlank()) {
      drawTextWatermark(canvas, result.width, result.height, config, marginPx, scaleFactor)
    }

    return result
  }

  /**
   * Applies handwritten signature or imported signature onto a bitmap.
   */
  fun applySignature(
    bitmap: Bitmap,
    config: SignatureConfig,
    importedSignatureBitmap: Bitmap? = null
  ): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val scaleFactor = result.width.toFloat() / 1000f
    val marginPx = config.marginDp * scaleFactor

    if (importedSignatureBitmap != null) {
      val targetW = (importedSignatureBitmap.width * config.scale * scaleFactor).toInt().coerceAtLeast(50)
      val targetH = (importedSignatureBitmap.height * config.scale * scaleFactor).toInt().coerceAtLeast(30)
      val scaledSig = Bitmap.createScaledBitmap(importedSignatureBitmap, targetW, targetH, true)

      val (x, y) = calculatePosition(
        position = config.position,
        canvasW = result.width,
        canvasH = result.height,
        elementW = targetW,
        elementH = targetH,
        margin = marginPx
      )

      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
      }

      canvas.save()
      if (config.rotation != 0f) {
        canvas.rotate(config.rotation, x + targetW / 2f, y + targetH / 2f)
      }
      canvas.drawBitmap(scaledSig, x.toFloat(), y.toFloat(), paint)
      canvas.restore()
    } else if (config.strokes.isNotEmpty()) {
      // Render drawn signature strokes
      val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
      }

      // Compute bounding box of strokes
      var minX = Float.MAX_VALUE
      var minY = Float.MAX_VALUE
      var maxX = Float.MIN_VALUE
      var maxY = Float.MIN_VALUE

      for (stroke in config.strokes) {
        for (pt in stroke.points) {
          if (pt.x < minX) minX = pt.x
          if (pt.y < minY) minY = pt.y
          if (pt.x > maxX) maxX = pt.x
          if (pt.y > maxY) maxY = pt.y
        }
      }

      val strokeW = (maxX - minX).coerceAtLeast(10f)
      val strokeH = (maxY - minY).coerceAtLeast(10f)

      val scaledW = strokeW * config.scale * scaleFactor
      val scaledH = strokeH * config.scale * scaleFactor

      val (startX, startY) = calculatePosition(
        position = config.position,
        canvasW = result.width,
        canvasH = result.height,
        elementW = scaledW.toInt(),
        elementH = scaledH.toInt(),
        margin = marginPx
      )

      canvas.save()
      canvas.translate(startX.toFloat() - minX * config.scale * scaleFactor, startY.toFloat() - minY * config.scale * scaleFactor)
      canvas.scale(config.scale * scaleFactor, config.scale * scaleFactor)

      for (stroke in config.strokes) {
        if (stroke.points.size > 1) {
          sigPaint.color = stroke.color
          sigPaint.strokeWidth = stroke.strokeWidth
          val path = Path()
          path.moveTo(stroke.points[0].x, stroke.points[0].y)
          for (i in 1 until stroke.points.size) {
            path.lineTo(stroke.points[i].x, stroke.points[i].y)
          }
          canvas.drawPath(path, sigPaint)
        }
      }
      canvas.restore()
    }

    return result
  }

  /**
   * Saves watermarked or signed bitmap to public gallery folder.
   */
  suspend fun saveWatermarkedBitmap(
    context: Context,
    bitmap: Bitmap,
    prefix: String = "WM"
  ): Result<Uri> = withContext(Dispatchers.IO) {
    try {
      val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val outputDir = File(picturesDir, "My Gallery AI/Watermark").apply { mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val outputFile = File(outputDir, "${prefix}_$timeStamp.jpg")

      FileOutputStream(outputFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        fos.flush()
      }

      MediaScannerConnection.scanFile(
        context,
        arrayOf(outputFile.absolutePath),
        arrayOf("image/jpeg"),
        null
      )

      Result.success(Uri.fromFile(outputFile))
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  private fun drawTextWatermark(
    canvas: Canvas,
    width: Int,
    height: Int,
    config: WatermarkConfig,
    margin: Float,
    scaleFactor: Float
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.textColor
      textSize = config.fontSizeSp * scaleFactor
      alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
      if (config.hasShadow) {
        setShadowLayer(4f * scaleFactor, 2f * scaleFactor, 2f * scaleFactor, Color.argb(180, 0, 0, 0))
      }
    }

    val bounds = Rect()
    paint.getTextBounds(config.text, 0, config.text.length, bounds)
    val textW = bounds.width()
    val textH = bounds.height()

    val (x, y) = calculatePosition(
      position = config.position,
      canvasW = width,
      canvasH = height,
      elementW = textW,
      elementH = textH,
      margin = margin
    )

    canvas.save()
    if (config.rotation != 0f) {
      canvas.rotate(config.rotation, x + textW / 2f, y + textH / 2f)
    }
    canvas.drawText(config.text, x.toFloat(), y.toFloat() + textH, paint)
    canvas.restore()
  }

  private fun drawLogoWatermark(
    canvas: Canvas,
    width: Int,
    height: Int,
    logo: Bitmap,
    config: WatermarkConfig,
    margin: Float,
    scaleFactor: Float
  ) {
    val targetW = (width * 0.18f).toInt().coerceAtLeast(80)
    val targetH = (logo.height * (targetW.toFloat() / logo.width)).toInt()
    val scaledLogo = Bitmap.createScaledBitmap(logo, targetW, targetH, true)

    val (x, y) = calculatePosition(
      position = config.position,
      canvasW = width,
      canvasH = height,
      elementW = targetW,
      elementH = targetH,
      margin = margin
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
    }

    canvas.save()
    if (config.rotation != 0f) {
      canvas.rotate(config.rotation, x + targetW / 2f, y + targetH / 2f)
    }
    canvas.drawBitmap(scaledLogo, x.toFloat(), y.toFloat(), paint)
    canvas.restore()
  }

  private fun drawTiledWatermark(
    canvas: Canvas,
    width: Int,
    height: Int,
    config: WatermarkConfig,
    scaleFactor: Float
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.textColor
      textSize = (config.fontSizeSp * 0.8f) * scaleFactor
      alpha = ((config.opacity * 0.45f) * 255).toInt().coerceIn(0, 255)
    }

    val bounds = Rect()
    paint.getTextBounds(config.text, 0, config.text.length, bounds)
    val textW = bounds.width() + 100 * scaleFactor
    val textH = bounds.height() + 80 * scaleFactor

    canvas.save()
    canvas.rotate(-30f, width / 2f, height / 2f)

    var y = -height.toFloat()
    while (y < height * 2) {
      var x = -width.toFloat()
      while (x < width * 2) {
        canvas.drawText(config.text, x, y, paint)
        x += textW
      }
      y += textH
    }
    canvas.restore()
  }

  private fun calculatePosition(
    position: WatermarkPosition,
    canvasW: Int,
    canvasH: Int,
    elementW: Int,
    elementH: Int,
    margin: Float
  ): Pair<Int, Int> {
    val m = margin.toInt()
    val x = when (position) {
      WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> m
      WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER, WatermarkPosition.BOTTOM_CENTER -> (canvasW - elementW) / 2
      WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> canvasW - elementW - m
      WatermarkPosition.TILED -> m
    }

    val y = when (position) {
      WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT -> m
      WatermarkPosition.CENTER -> (canvasH - elementH) / 2
      WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT -> canvasH - elementH - m
      WatermarkPosition.TILED -> m
    }

    return Pair(x.coerceAtLeast(0), y.coerceAtLeast(0))
  }
}
