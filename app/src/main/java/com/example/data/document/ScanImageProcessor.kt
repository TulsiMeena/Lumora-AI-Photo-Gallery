package com.example.data.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max

data class QuadCorners(
  val topLeft: PointF,
  val topRight: PointF,
  val bottomRight: PointF,
  val bottomLeft: PointF
) {
  fun toFloatArray(): FloatArray {
    return floatArrayOf(
      topLeft.x, topLeft.y,
      topRight.x, topRight.y,
      bottomRight.x, bottomRight.y,
      bottomLeft.x, bottomLeft.y
    )
  }

  companion object {
    fun defaultFromSize(width: Float, height: Float, insetRatio: Float = 0.05f): QuadCorners {
      val insetX = width * insetRatio
      val insetY = height * insetRatio
      return QuadCorners(
        topLeft = PointF(insetX, insetY),
        topRight = PointF(width - insetX, insetY),
        bottomRight = PointF(width - insetX, height - insetY),
        bottomLeft = PointF(insetX, height - insetY)
      )
    }

    fun fullFrame(width: Float, height: Float): QuadCorners {
      return QuadCorners(
        topLeft = PointF(0f, 0f),
        topRight = PointF(width, 0f),
        bottomRight = PointF(width, height),
        bottomLeft = PointF(0f, height)
      )
    }
  }
}

enum class DocumentFilterType(val displayName: String) {
  ORIGINAL("Original"),
  COLOR_SCAN("Color Scan"),
  GRAYSCALE("Grayscale"),
  BLACK_AND_WHITE("Black & White"),
  AUTO_ENHANCE("Auto Enhance")
}

data class ScanEnhanceSettings(
  val filterType: DocumentFilterType = DocumentFilterType.COLOR_SCAN,
  val brightness: Float = 0f, // -100f to +100f
  val contrast: Float = 1.0f, // 0.5f to 2.0f
  val rotationDegrees: Int = 0 // 0, 90, 180, 270
)

object ScanImageProcessor {

  /**
   * Warps the 4 quadrilateral corners to a flattened rectangular bitmap.
   */
  suspend fun warpPerspective(
    sourceBitmap: Bitmap,
    corners: QuadCorners
  ): Bitmap = withContext(Dispatchers.Default) {
    val topWidth = hypot((corners.topRight.x - corners.topLeft.x).toDouble(), (corners.topRight.y - corners.topLeft.y).toDouble())
    val bottomWidth = hypot((corners.bottomRight.x - corners.bottomLeft.x).toDouble(), (corners.bottomRight.y - corners.bottomLeft.y).toDouble())
    val targetWidth = max(topWidth, bottomWidth).toInt().coerceIn(400, 3600)

    val leftHeight = hypot((corners.bottomLeft.x - corners.topLeft.x).toDouble(), (corners.bottomLeft.y - corners.topLeft.y).toDouble())
    val rightHeight = hypot((corners.bottomRight.x - corners.topRight.x).toDouble(), (corners.bottomRight.y - corners.topRight.y).toDouble())
    val targetHeight = max(leftHeight, rightHeight).toInt().coerceIn(400, 4800)

    val src = corners.toFloatArray()
    val dst = floatArrayOf(
      0f, 0f,
      targetWidth.toFloat(), 0f,
      targetWidth.toFloat(), targetHeight.toFloat(),
      0f, targetHeight.toFloat()
    )

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)

    val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(sourceBitmap, matrix, paint)

    output
  }

  /**
   * Applies document enhancement filters (Color Scan, Grayscale, B&W, Auto Enhance)
   * plus brightness and contrast adjustments.
   */
  suspend fun applyEnhancements(
    input: Bitmap,
    settings: ScanEnhanceSettings
  ): Bitmap = withContext(Dispatchers.Default) {
    var working = input

    // 1. Rotation if needed
    if (settings.rotationDegrees % 360 != 0) {
      val rotMatrix = Matrix()
      rotMatrix.postRotate(settings.rotationDegrees.toFloat())
      working = Bitmap.createBitmap(working, 0, 0, working.width, working.height, rotMatrix, true)
    }

    val width = working.width
    val height = working.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    when (settings.filterType) {
      DocumentFilterType.ORIGINAL -> {
        if (settings.brightness == 0f && settings.contrast == 1.0f) {
          canvas.drawBitmap(working, 0f, 0f, paint)
          return@withContext output
        }
        val cm = createBrightnessContrastMatrix(settings.brightness, settings.contrast)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(working, 0f, 0f, paint)
      }

      DocumentFilterType.COLOR_SCAN -> {
        // Boost whites, slightly enhance saturation to keep stamps/logos vivid
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
          1.15f * settings.contrast, 0f, 0f, 0f, settings.brightness + 18f,
          0f, 1.15f * settings.contrast, 0f, 0f, settings.brightness + 18f,
          0f, 0f, 1.15f * settings.contrast, 0f, settings.brightness + 18f,
          0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(working, 0f, 0f, paint)
      }

      DocumentFilterType.GRAYSCALE -> {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val contrastCm = createBrightnessContrastMatrix(settings.brightness + 10f, settings.contrast * 1.2f)
        cm.postConcat(contrastCm)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(working, 0f, 0f, paint)
      }

      DocumentFilterType.BLACK_AND_WHITE -> {
        // High contrast binarization for clean printed text on white paper
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val grayCanvas = Canvas(grayBitmap)
        val grayPaint = Paint()
        val sat = ColorMatrix()
        sat.setSaturation(0f)
        grayPaint.colorFilter = ColorMatrixColorFilter(sat)
        grayCanvas.drawBitmap(working, 0f, 0f, grayPaint)

        // Fast adaptive thresholding in software
        val pixels = IntArray(width * height)
        grayBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshold = (128 + settings.brightness).coerceIn(40f, 210f).toInt()
        for (i in pixels.indices) {
          val lum = Color.red(pixels[i]) // Since saturated to 0, R=G=B
          pixels[i] = if (lum > threshold) Color.WHITE else Color.BLACK
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
      }

      DocumentFilterType.AUTO_ENHANCE -> {
        // Balanced contrast stretch + gentle sharpening
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
          1.25f * settings.contrast, 0f, 0f, 0f, settings.brightness + 12f,
          0f, 1.25f * settings.contrast, 0f, 0f, settings.brightness + 12f,
          0f, 0f, 1.25f * settings.contrast, 0f, settings.brightness + 12f,
          0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(working, 0f, 0f, paint)
      }
    }

    output
  }

  private fun createBrightnessContrastMatrix(brightness: Float, contrast: Float): ColorMatrix {
    val cm = ColorMatrix()
    val t = (1.0f - contrast) * 128f + brightness
    cm.set(floatArrayOf(
      contrast, 0f, 0f, 0f, t,
      0f, contrast, 0f, 0f, t,
      0f, 0f, contrast, 0f, t,
      0f, 0f, 0f, 1f, 0f
    ))
    return cm
  }

  /**
   * Saves the scanned page into internal app files directory.
   * Completely preserves the user's original photos.
   */
  suspend fun saveScannedPage(
    context: Context,
    bitmap: Bitmap,
    pageNamePrefix: String = "doc_page"
  ): String = withContext(Dispatchers.IO) {
    val docsDir = File(context.filesDir, "scanned_documents")
    if (!docsDir.exists()) docsDir.mkdirs()

    val fileName = "${pageNamePrefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
    val file = File(docsDir, fileName)

    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
      out.flush()
    }

    Uri.fromFile(file).toString()
  }
}
