package com.example.utils.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.model.creative.CollageConfig
import com.example.model.creative.CollageLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CollageEngine {

  /**
   * Generates a composite collage bitmap from a list of input bitmaps or URIs.
   */
  suspend fun createCollageBitmap(
    context: Context,
    imageUris: List<Uri>,
    config: CollageConfig,
    targetDimension: Int = 2048
  ): Bitmap? = withContext(Dispatchers.IO) {
    try {
      if (imageUris.isEmpty()) return@withContext null

      // Determine canvas dimensions based on target dimension and aspect ratio
      val canvasWidth = targetDimension
      val canvasHeight = (targetDimension / config.aspectRatio).toInt()

      val outputBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(outputBitmap)

      // Draw background
      canvas.drawColor(config.backgroundColor)

      // Calculate cell rects based on layout
      val cellRects = calculateCellRects(
        layout = config.layout,
        canvasWidth = canvasWidth.toFloat(),
        canvasHeight = canvasHeight.toFloat(),
        padding = config.paddingDp * (canvasWidth / 400f)
      )

      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.borderColor
        style = Paint.Style.STROKE
        strokeWidth = config.borderWidthDp * (canvasWidth / 400f)
      }

      val cornerRadiusPx = config.cornerRadiusDp * (canvasWidth / 400f)

      for (i in 0 until minOf(imageUris.size, cellRects.size)) {
        val uri = imageUris[i]
        val cellRect = cellRects[i]

        val loadedBmp = loadDownsampledBitmap(context, uri, cellRect.width().toInt(), cellRect.height().toInt())
        if (loadedBmp != null) {
          canvas.save()

          // Clip cell with rounded corners
          val path = Path().apply {
            addRoundRect(cellRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
          }
          canvas.clipPath(path)

          // Center-crop draw image into cellRect
          drawCenterCropped(canvas, loadedBmp, cellRect)

          canvas.restore()

          // Draw border if requested
          if (config.borderWidthDp > 0f) {
            canvas.drawRoundRect(cellRect, cornerRadiusPx, cornerRadiusPx, borderPaint)
          }
        }
      }

      outputBitmap
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Saves collage bitmap to public Pictures/My Gallery AI/Collage/ folder.
   */
  suspend fun saveCollage(
    context: Context,
    bitmap: Bitmap,
    titlePrefix: String = "Collage"
  ): Result<Uri> = withContext(Dispatchers.IO) {
    try {
      val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val outputDir = File(picturesDir, "My Gallery AI/Collage").apply { mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val outputFile = File(outputDir, "${titlePrefix}_$timeStamp.jpg")

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

  private fun calculateCellRects(
    layout: CollageLayout,
    canvasWidth: Float,
    canvasHeight: Float,
    padding: Float
  ): List<RectF> {
    val rects = mutableListOf<RectF>()
    val p = padding
    val doubleP = p * 2

    when (layout) {
      CollageLayout.SPLIT_2_V -> {
        val w = (canvasWidth - p * 3) / 2f
        val h = canvasHeight - doubleP
        rects.add(RectF(p, p, p + w, p + h))
        rects.add(RectF(p * 2 + w, p, canvasWidth - p, p + h))
      }
      CollageLayout.SPLIT_2_H -> {
        val w = canvasWidth - doubleP
        val h = (canvasHeight - p * 3) / 2f
        rects.add(RectF(p, p, p + w, p + h))
        rects.add(RectF(p, p * 2 + h, p + w, canvasHeight - p))
      }
      CollageLayout.GRID_3 -> {
        // 1 big top, 2 smaller bottom
        val topH = (canvasHeight - p * 3) * 0.55f
        val bottomH = (canvasHeight - p * 3) * 0.45f
        val bottomW = (canvasWidth - p * 3) / 2f

        rects.add(RectF(p, p, canvasWidth - p, p + topH))
        rects.add(RectF(p, p * 2 + topH, p + bottomW, canvasHeight - p))
        rects.add(RectF(p * 2 + bottomW, p * 2 + topH, canvasWidth - p, canvasHeight - p))
      }
      CollageLayout.GRID_4 -> {
        // 2x2 grid
        val w = (canvasWidth - p * 3) / 2f
        val h = (canvasHeight - p * 3) / 2f
        rects.add(RectF(p, p, p + w, p + h))
        rects.add(RectF(p * 2 + w, p, canvasWidth - p, p + h))
        rects.add(RectF(p, p * 2 + h, p + w, canvasHeight - p))
        rects.add(RectF(p * 2 + w, p * 2 + h, canvasWidth - p, canvasHeight - p))
      }
      CollageLayout.GRID_6 -> {
        // 2 cols, 3 rows
        val w = (canvasWidth - p * 3) / 2f
        val h = (canvasHeight - p * 4) / 3f
        for (row in 0 until 3) {
          for (col in 0 until 2) {
            val left = p + col * (w + p)
            val top = p + row * (h + p)
            rects.add(RectF(left, top, left + w, top + h))
          }
        }
      }
      CollageLayout.GRID_9 -> {
        // 3x3 grid
        val w = (canvasWidth - p * 4) / 3f
        val h = (canvasHeight - p * 4) / 3f
        for (row in 0 until 3) {
          for (col in 0 until 3) {
            val left = p + col * (w + p)
            val top = p + row * (h + p)
            rects.add(RectF(left, top, left + w, top + h))
          }
        }
      }
    }
    return rects
  }

  private fun drawCenterCropped(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
    val srcW = bitmap.width
    val srcH = bitmap.height
    val destW = destRect.width()
    val destH = destRect.height()

    val srcAspect = srcW.toFloat() / srcH
    val destAspect = destW / destH

    val srcCropRect = if (srcAspect > destAspect) {
      // Source is wider: crop horizontal sides
      val cropW = (srcH * destAspect).toInt()
      val left = (srcW - cropW) / 2
      Rect(left, 0, left + cropW, srcH)
    } else {
      // Source is taller: crop vertical ends
      val cropH = (srcW / destAspect).toInt()
      val top = (srcH - cropH) / 2
      Rect(0, top, srcW, top + cropH)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, srcCropRect, destRect, paint)
  }

  private fun loadDownsampledBitmap(
    context: Context,
    uri: Uri,
    targetWidth: Int,
    targetHeight: Int
  ): Bitmap? {
    return try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
      }

      var sampleSize = 1
      while (options.outWidth / (sampleSize * 2) >= targetWidth &&
        options.outHeight / (sampleSize * 2) >= targetHeight
      ) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
      }
    } catch (_: Exception) {
      null
    }
  }
}
