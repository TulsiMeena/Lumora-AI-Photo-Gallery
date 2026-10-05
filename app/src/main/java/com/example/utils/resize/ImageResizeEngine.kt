package com.example.utils.resize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.example.model.resize.ImageMetadataInfo
import com.example.model.resize.OutputFormat
import com.example.model.resize.PhotoSheetConfig
import com.example.model.resize.PhotoSheetPaperSize
import com.example.model.resize.ResizeMode
import com.example.model.resize.ResizeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageResizeEngine {

  const val MAX_SAFE_DIMENSION = 8192

  // --- Dimension & Unit Conversions ---

  fun toPixels(value: Double, unit: ResizeUnit, dpi: Int): Int {
    val effectiveDpi = if (dpi > 0) dpi else 300
    val px = when (unit) {
      ResizeUnit.PIXELS -> value
      ResizeUnit.INCHES -> value * effectiveDpi
      ResizeUnit.CENTIMETERS -> (value / 2.54) * effectiveDpi
      ResizeUnit.MILLIMETERS -> (value / 25.4) * effectiveDpi
    }
    return max(1, px.roundToInt())
  }

  fun fromPixels(pixels: Int, targetUnit: ResizeUnit, dpi: Int): Double {
    val effectiveDpi = if (dpi > 0) dpi else 300
    return when (targetUnit) {
      ResizeUnit.PIXELS -> pixels.toDouble()
      ResizeUnit.INCHES -> pixels.toDouble() / effectiveDpi
      ResizeUnit.CENTIMETERS -> (pixels.toDouble() / effectiveDpi) * 2.54
      ResizeUnit.MILLIMETERS -> (pixels.toDouble() / effectiveDpi) * 25.4
    }
  }

  fun formatUnitValue(value: Double, unit: ResizeUnit): String {
    return when (unit) {
      ResizeUnit.PIXELS -> "%.0f".format(value)
      ResizeUnit.INCHES -> "%.2f".format(value)
      ResizeUnit.CENTIMETERS -> "%.2f".format(value)
      ResizeUnit.MILLIMETERS -> "%.1f".format(value)
    }
  }

  // --- Safe Image Metadata Reading ---

  suspend fun readImageMetadata(context: Context, uri: Uri): ImageMetadataInfo = withContext(Dispatchers.IO) {
    var width = 1920
    var height = 1080
    var mimeType = "image/jpeg"
    var sizeBytes = 0L
    var existingDpi = 72
    var fileName = "image.jpg"
    var hasAlpha = false

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "image.jpg"
          if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
        }
      }
    } catch (_: Exception) {}

    try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
      }
      if (options.outWidth > 0 && options.outHeight > 0) {
        width = options.outWidth
        height = options.outHeight
      }
      if (!options.outMimeType.isNullOrBlank()) {
        mimeType = options.outMimeType
      }
    } catch (_: Exception) {}

    // Check EXIF orientation and DPI
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
          val temp = width
          width = height
          height = temp
        }
        val xRes = exif.getAttributeInt("XResolution", 0)
        if (xRes > 0) existingDpi = xRes
      }
    } catch (_: Exception) {}

    if (mimeType.contains("png", ignoreCase = true) || mimeType.contains("webp", ignoreCase = true)) {
      hasAlpha = true
    }

    val aspectGcd = gcd(width, height)
    val aspectStr = if (aspectGcd > 0) "${width / aspectGcd}:${height / aspectGcd}" else "Custom"

    val formatDisplayName = when {
      mimeType.contains("png", ignoreCase = true) -> "PNG"
      mimeType.contains("webp", ignoreCase = true) -> "WebP"
      else -> "JPEG"
    }

    ImageMetadataInfo(
      fileName = fileName,
      formatName = formatDisplayName,
      widthPx = width,
      heightPx = height,
      sizeBytes = sizeBytes,
      existingDpi = existingDpi,
      aspectRatioStr = aspectStr,
      hasAlpha = hasAlpha
    )
  }

  private fun gcd(a: Int, b: Int): Int {
    var n1 = a
    var n2 = b
    while (n2 != 0) {
      val t = n2
      n2 = n1 % n2
      n1 = t
    }
    return n1
  }

  // --- Safe Bitmap Decoding with InSampleSize ---

  suspend fun loadSafeBitmap(context: Context, uri: Uri, maxTargetDim: Int = 4096): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }

      val origW = boundsOptions.outWidth
      val origH = boundsOptions.outHeight
      if (origW <= 0 || origH <= 0) return@withContext null

      var sampleSize = 1
      while ((origW / sampleSize) > maxTargetDim * 1.5 || (origH / sampleSize) > maxTargetDim * 1.5) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      val rawBitmap: Bitmap = (context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      }) ?: return@withContext null

      // Check EXIF rotation and rotate if necessary
      val orientation = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val exif = ExifInterface(stream)
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
      } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
      }

      rotateBitmapIfNeeded(rawBitmap, orientation)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      else -> return bitmap
    }
    return try {
      val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      if (rotated != bitmap) bitmap.recycle()
      rotated
    } catch (_: Exception) {
      bitmap
    }
  }

  // --- Core Resize Execution ---

  suspend fun resizeBitmap(
    source: Bitmap,
    targetWidth: Int,
    targetHeight: Int,
    mode: ResizeMode,
    backgroundColor: Int = Color.WHITE
  ): Bitmap = withContext(Dispatchers.Default) {
    val clampedW = targetWidth.coerceIn(1, MAX_SAFE_DIMENSION)
    val clampedH = targetHeight.coerceIn(1, MAX_SAFE_DIMENSION)

    when (mode) {
      ResizeMode.STRETCH -> {
        Bitmap.createScaledBitmap(source, clampedW, clampedH, true)
      }

      ResizeMode.FIT, ResizeMode.ORIGINAL_RATIO -> {
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val targetRatio = clampedW.toFloat() / clampedH.toFloat()

        var drawW = clampedW
        var drawH = clampedH
        if (srcRatio > targetRatio) {
          // Source is wider, scale based on target width
          drawH = max(1, (clampedW / srcRatio).roundToInt())
        } else {
          // Source is taller, scale based on target height
          drawW = max(1, (clampedH * srcRatio).roundToInt())
        }

        if (mode == ResizeMode.ORIGINAL_RATIO) {
          // In original ratio, result canvas has exact scaled dimensions
          Bitmap.createScaledBitmap(source, drawW, drawH, true)
        } else {
          // FIT: places image centered inside clampedW x clampedH canvas
          val output = Bitmap.createBitmap(clampedW, clampedH, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(output)
          canvas.drawColor(backgroundColor)
          val scaled = Bitmap.createScaledBitmap(source, drawW, drawH, true)
          val left = (clampedW - drawW) / 2f
          val top = (clampedH - drawH) / 2f
          canvas.drawBitmap(scaled, left, top, Paint(Paint.FILTER_BITMAP_FLAG))
          if (scaled != source) scaled.recycle()
          output
        }
      }

      ResizeMode.FILL -> {
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val targetRatio = clampedW.toFloat() / clampedH.toFloat()

        var cropSrcRect: Rect
        if (srcRatio > targetRatio) {
          // Source is wider than target: crop left & right
          val newSrcW = (source.height * targetRatio).roundToInt()
          val offsetX = (source.width - newSrcW) / 2
          cropSrcRect = Rect(offsetX, 0, offsetX + newSrcW, source.height)
        } else {
          // Source is taller than target: crop top & bottom
          val newSrcH = (source.width / targetRatio).roundToInt()
          val offsetY = (source.height - newSrcH) / 2
          cropSrcRect = Rect(0, offsetY, source.width, offsetY + newSrcH)
        }

        val output = Bitmap.createBitmap(clampedW, clampedH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        val dstRect = Rect(0, 0, clampedW, clampedH)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, cropSrcRect, dstRect, paint)
        output
      }

      ResizeMode.CROP_TO_SIZE -> {
        // Crop centered or exact aspect ratio then scale
        val output = Bitmap.createBitmap(clampedW, clampedH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        val srcRect = Rect(0, 0, source.width, source.height)
        val dstRect = Rect(0, 0, clampedW, clampedH)
        canvas.drawBitmap(source, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        output
      }
    }
  }

  // --- Passport Biometric Background Adjustment ---

  suspend fun applyPassportBackground(
    source: Bitmap,
    backgroundColor: Int
  ): Bitmap = withContext(Dispatchers.Default) {
    if (backgroundColor == Color.TRANSPARENT) return@withContext source
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(backgroundColor)
    canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
    output
  }

  // --- Output File Size Estimation ---

  fun estimateFileSize(width: Int, height: Int, format: OutputFormat, quality: Int): Long {
    val pixels = width.toLong() * height.toLong()
    return when (format) {
      OutputFormat.JPEG -> {
        val qFactor = quality / 100.0
        // Realistic empirical formula: ~0.15 - 0.35 bytes per pixel at 90% quality
        max(8 * 1024L, (pixels * 3.0 * (qFactor * 0.18)).toLong())
      }
      OutputFormat.PNG -> {
        // Lossless: ~1.2 - 2.0 bytes per pixel depending on complexity
        max(12 * 1024L, (pixels * 1.5).toLong())
      }
      OutputFormat.WEBP -> {
        val qFactor = quality / 100.0
        max(6 * 1024L, (pixels * 3.0 * (qFactor * 0.12)).toLong())
      }
    }
  }

  // --- Saving Resized Image to Storage ---

  suspend fun saveResizedImage(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    format: OutputFormat,
    quality: Int,
    dpi: Int,
    preserveMetadata: Boolean = false,
    originalUri: Uri? = null,
    backgroundColorForJpeg: Int = Color.WHITE
  ): File = withContext(Dispatchers.IO) {
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "My Gallery AI/Resized")
    if (!dir.exists()) dir.mkdirs()

    val cleanName = if (fileName.endsWith(format.extension, ignoreCase = true)) {
      fileName
    } else {
      "${fileName.substringBeforeLast('.')}${format.extension}"
    }

    var targetFile = File(dir, cleanName)
    var counter = 1
    val baseName = cleanName.substringBeforeLast('.')
    while (targetFile.exists()) {
      targetFile = File(dir, "${baseName}_$counter${format.extension}")
      counter++
    }

    // Prepare bitmap for format (flatten transparency if JPEG)
    val bitmapToSave = if (format == OutputFormat.JPEG && bitmap.hasAlpha()) {
      val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
      val canvas = Canvas(flattened)
      canvas.drawColor(backgroundColorForJpeg)
      canvas.drawBitmap(bitmap, 0f, 0f, null)
      flattened
    } else {
      bitmap
    }

    FileOutputStream(targetFile).use { out ->
      when (format) {
        OutputFormat.JPEG -> bitmapToSave.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
        OutputFormat.PNG -> bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
        OutputFormat.WEBP -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val compressFormat = if (quality >= 100) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
            bitmapToSave.compress(compressFormat, quality.coerceIn(1, 100), out)
          } else {
            @Suppress("DEPRECATION")
            bitmapToSave.compress(Bitmap.CompressFormat.WEBP, quality.coerceIn(1, 100), out)
          }
        }
      }
    }

    if (bitmapToSave != bitmap) {
      bitmapToSave.recycle()
    }

    // Write DPI & EXIF metadata if possible
    try {
      val exif = ExifInterface(targetFile.absolutePath)
      exif.setAttribute("XResolution", "$dpi/1")
      exif.setAttribute("YResolution", "$dpi/1")
      exif.setAttribute("ResolutionUnit", "2") // 2 = Inches
      exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
      exif.setAttribute(ExifInterface.TAG_SOFTWARE, "My Gallery AI Image Studio")

      if (preserveMetadata && originalUri != null) {
        try {
          context.contentResolver.openInputStream(originalUri)?.use { origStream ->
            val origExif = ExifInterface(origStream)
            val date = origExif.getAttribute(ExifInterface.TAG_DATETIME)
            if (date != null) exif.setAttribute(ExifInterface.TAG_DATETIME, date)
            val make = origExif.getAttribute(ExifInterface.TAG_MAKE)
            if (make != null) exif.setAttribute(ExifInterface.TAG_MAKE, make)
            val model = origExif.getAttribute(ExifInterface.TAG_MODEL)
            if (model != null) exif.setAttribute(ExifInterface.TAG_MODEL, model)
          }
        } catch (_: Exception) {}
      }
      exif.saveAttributes()
    } catch (_: Exception) {}

    // Notify MediaScanner
    try {
      MediaScannerConnection.scanFile(
        context,
        arrayOf(targetFile.absolutePath),
        arrayOf(format.mimeType),
        null
      )
    } catch (_: Exception) {}

    targetFile
  }

  // --- Multi-Copy Passport Photo Sheet Generation ---

  suspend fun generatePassportPhotoSheetBitmap(
    photo: Bitmap,
    config: PhotoSheetConfig
  ): Bitmap = withContext(Dispatchers.Default) {
    val dpi = config.targetDpi
    // Paper dimensions in pixels @ 300 DPI
    val paperWPx = if (config.isLandscape) {
      toPixels(config.paperSize.heightMm, ResizeUnit.MILLIMETERS, dpi)
    } else {
      toPixels(config.paperSize.widthMm, ResizeUnit.MILLIMETERS, dpi)
    }
    val paperHPx = if (config.isLandscape) {
      toPixels(config.paperSize.widthMm, ResizeUnit.MILLIMETERS, dpi)
    } else {
      toPixels(config.paperSize.heightMm, ResizeUnit.MILLIMETERS, dpi)
    }

    val photoWPx = toPixels(config.photoWidthMm, ResizeUnit.MILLIMETERS, dpi)
    val photoHPx = toPixels(config.photoHeightMm, ResizeUnit.MILLIMETERS, dpi)
    val spacingPx = toPixels(config.spacingMm, ResizeUnit.MILLIMETERS, dpi)
    val marginPx = toPixels(config.marginMm, ResizeUnit.MILLIMETERS, dpi)

    val sheetBitmap = Bitmap.createBitmap(paperWPx, paperHPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(sheetBitmap)
    canvas.drawColor(Color.WHITE)

    // Calculate maximum columns and rows that fit within margins
    val availWPx = paperWPx - (2 * marginPx)
    val availHPx = paperHPx - (2 * marginPx)
    val maxCols = max(1, (availWPx + spacingPx) / (photoWPx + spacingPx))
    val maxRows = max(1, (availHPx + spacingPx) / (photoHPx + spacingPx))

    val scaledPhoto = Bitmap.createScaledBitmap(photo, photoWPx, photoHPx, true)

    val targetCopies = config.copiesCount.coerceIn(1, maxCols * maxRows)

    // Calculate grid layout centered on paper
    val actualCols = min(targetCopies, maxCols)
    val actualRows = min((targetCopies + actualCols - 1) / actualCols, maxRows)

    val gridTotalW = (actualCols * photoWPx) + ((actualCols - 1) * spacingPx)
    val startX = (paperWPx - gridTotalW) / 2f
    val startY = marginPx.toFloat()

    val cutLinePaint = Paint().apply {
      color = Color.LTGRAY
      style = Paint.Style.STROKE
      strokeWidth = max(1f, (dpi / 150f))
      pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    var drawnCount = 0
    for (r in 0 until actualRows) {
      for (c in 0 until actualCols) {
        if (drawnCount >= targetCopies) break
        val x = startX + c * (photoWPx + spacingPx)
        val y = startY + r * (photoHPx + spacingPx)

        canvas.drawBitmap(scaledPhoto, x, y, Paint(Paint.FILTER_BITMAP_FLAG))

        if (config.drawCutLines) {
          canvas.drawRect(x, y, x + photoWPx, y + photoHPx, cutLinePaint)
        }
        drawnCount++
      }
      if (drawnCount >= targetCopies) break
    }

    if (scaledPhoto != photo) scaledPhoto.recycle()
    sheetBitmap
  }

  suspend fun generatePassportPhotoSheetPdf(
    context: Context,
    photo: Bitmap,
    config: PhotoSheetConfig,
    outputFile: File
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      // 1 mm = 72 / 25.4 points in standard PDF
      val mmToPts = 72.0 / 25.4
      val pageW = if (config.isLandscape) {
        (config.paperSize.heightMm * mmToPts).roundToInt()
      } else {
        (config.paperSize.widthMm * mmToPts).roundToInt()
      }
      val pageH = if (config.isLandscape) {
        (config.paperSize.widthMm * mmToPts).roundToInt()
      } else {
        (config.paperSize.heightMm * mmToPts).roundToInt()
      }

      val pdfDoc = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
      val page = pdfDoc.startPage(pageInfo)
      val canvas = page.canvas

      canvas.drawColor(Color.WHITE)

      val photoW = (config.photoWidthMm * mmToPts).toFloat()
      val photoH = (config.photoHeightMm * mmToPts).toFloat()
      val spacing = (config.spacingMm * mmToPts).toFloat()
      val margin = (config.marginMm * mmToPts).toFloat()

      val availW = pageW - (2 * margin)
      val availH = pageH - (2 * margin)
      val maxCols = max(1, ((availW + spacing) / (photoW + spacing)).toInt())
      val maxRows = max(1, ((availH + spacing) / (photoH + spacing)).toInt())

      val targetCopies = config.copiesCount.coerceIn(1, maxCols * maxRows)
      val actualCols = min(targetCopies, maxCols)
      val actualRows = min((targetCopies + actualCols - 1) / actualCols, maxRows)

      val gridTotalW = (actualCols * photoW) + ((actualCols - 1) * spacing)
      val startX = (pageW - gridTotalW) / 2f
      val startY = margin

      val cutPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
      }

      val srcRect = Rect(0, 0, photo.width, photo.height)

      var drawnCount = 0
      for (r in 0 until actualRows) {
        for (c in 0 until actualCols) {
          if (drawnCount >= targetCopies) break
          val x = startX + c * (photoW + spacing)
          val y = startY + r * (photoH + spacing)

          val dstRect = RectF(x, y, x + photoW, y + photoH)
          canvas.drawBitmap(photo, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

          if (config.drawCutLines) {
            canvas.drawRect(dstRect, cutPaint)
          }
          drawnCount++
        }
        if (drawnCount >= targetCopies) break
      }

      pdfDoc.finishPage(page)
      FileOutputStream(outputFile).use { out ->
        pdfDoc.writeTo(out)
      }
      pdfDoc.close()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
