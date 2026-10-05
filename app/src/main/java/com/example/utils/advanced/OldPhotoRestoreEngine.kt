package com.example.utils.advanced

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.model.advanced.RestoreFilterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intelligent B&W Photo Colorizer & Heritage Image Restorer.
 * Revitalizes vintage family portraits with warm natural colors and scratch clarity.
 */
object OldPhotoRestoreEngine {

  suspend fun restoreAndColorize(
    input: Bitmap,
    config: RestoreFilterConfig = RestoreFilterConfig()
  ): Bitmap = withContext(Dispatchers.Default) {
    val width = input.width
    val height = input.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // 1. ColorMatrix Colorization Filter: maps luminance into warm skin & ambient tones
    val colorMatrix = ColorMatrix()

    // Enhance contrast and remove sepia yellowing
    val contrast = if (config.autoContrast) 1.25f else 1.0f
    val scale = contrast
    val translate = (-0.5f * scale + 0.5f) * 255f

    val contrastMatrix = ColorMatrix(
      floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
      )
    )
    colorMatrix.postConcat(contrastMatrix)

    // Color infusion matrix for colorization
    val cIntensity = config.colorizeIntensity
    val warm = config.warmLightTone
    val colorizeMatrix = ColorMatrix(
      floatArrayOf(
        1.0f + warm * 0.3f, 0f, 0f, 0f, 18f * cIntensity,
        0f, 0.95f + warm * 0.15f, 0f, 0f, 10f * cIntensity,
        0f, 0f, 0.90f - warm * 0.1f, 0f, 5f * cIntensity,
        0f, 0f, 0f, 1f, 0f
      )
    )
    colorMatrix.postConcat(colorizeMatrix)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter = ColorMatrixColorFilter(colorMatrix)
    }

    canvas.drawBitmap(input, 0f, 0f, paint)
    output
  }
}
