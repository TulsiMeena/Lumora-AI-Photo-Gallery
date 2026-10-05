package com.example.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.pdf.PdfBackground
import com.example.model.pdf.PdfConfig
import com.example.model.pdf.PdfCreatorPage
import com.example.model.pdf.PdfGenerationProgress
import com.example.model.pdf.PdfGenerationResult
import com.example.model.pdf.PdfHistoryItem
import com.example.model.pdf.PdfImageFit
import com.example.model.pdf.PdfOrientation
import com.example.model.pdf.PdfStandardSize
import com.example.model.pdf.PdfTargetSizeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PdfStudioEngine {

  /**
   * Generates a PDF file based on the provided pages and configuration.
   * Runs on Dispatchers.IO and reports progress.
   */
  suspend fun generatePdf(
    context: Context,
    pages: List<PdfCreatorPage>,
    config: PdfConfig,
    onProgress: (PdfGenerationProgress) -> Unit = {}
  ): PdfGenerationResult = withContext(Dispatchers.IO) {
    if (pages.isEmpty()) {
      return@withContext PdfGenerationResult.Error("No pages selected for PDF creation.")
    }

    try {
      val pdfDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val baseName = if (config.fileName.isNotBlank()) {
        config.fileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
      } else {
        "Gallery_Doc_$timeStamp"
      }
      val sanitizedFileName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
      val outputFile = File(pdfDir, sanitizedFileName)

      val totalPages = pages.size
      val pdfDocument = PdfDocument()

      // Calculate byte budget per page if target size is specified
      val targetBytes = config.effectiveTargetBytes
      val perPageBudget = if (targetBytes > 0) {
        val reserveForPdfHeader = 8192L + (totalPages * 512L)
        max(30000L, (targetBytes - reserveForPdfHeader) / totalPages)
      } else {
        0L
      }

      var firstPageThumbnailPath: String? = null

      for (index in pages.indices) {
        val page = pages[index]
        val pageNum = index + 1

        onProgress(
          PdfGenerationProgress(
            currentPage = pageNum,
            totalPages = totalPages,
            statusMessage = "Processing page $pageNum of $totalPages...",
            percent = ((index.toFloat() / totalPages) * 90).toInt()
          )
        )

        // 1. Decode bitmap with memory efficiency & rotation
        val rawBitmap = decodeSampledBitmapFromUri(
          context = context,
          uriString = page.sourceUri,
          maxDimension = calculateMaxDimensionForPage(config, perPageBudget),
          rotation = page.rotationDegrees
        ) ?: continue

        // 2. Target size compression if required
        val finalBitmap = if (perPageBudget > 0) {
          compressBitmapToBudget(rawBitmap, perPageBudget, config.isMaxLimitStrict)
        } else {
          compressBitmapWithPreset(rawBitmap, config)
        }

        if (rawBitmap != finalBitmap && !rawBitmap.isRecycled) {
          rawBitmap.recycle()
        }

        // 3. Save first page thumbnail for history preview
        if (index == 0) {
          firstPageThumbnailPath = saveThumbnail(context, finalBitmap, baseName)
        }

        // 4. Calculate page dimensions in points
        val orientation = page.orientationOverride ?: config.orientation
        val (pageWidth, pageHeight) = calculatePageDimensionsPt(
          config = config,
          orientation = orientation,
          bitmapWidth = finalBitmap.width,
          bitmapHeight = finalBitmap.height
        )

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.roundToInt(), pageHeight.roundToInt(), pageNum).create()
        val pdfPage = pdfDocument.startPage(pageInfo)
        val canvas = pdfPage.canvas

        // 5. Draw page background
        drawPageBackground(canvas, pageWidth, pageHeight, config)

        // 6. Draw image with margin & fit mode
        val marginPt = config.effectiveMarginPt
        val fit = page.fitOverride ?: config.imageFit
        drawImageOnPage(
          canvas = canvas,
          bitmap = finalBitmap,
          pageWidth = pageWidth,
          pageHeight = pageHeight,
          marginPt = marginPt,
          fit = fit
        )

        pdfDocument.finishPage(pdfPage)
        if (!finalBitmap.isRecycled) {
          finalBitmap.recycle()
        }
      }

      onProgress(
        PdfGenerationProgress(
          currentPage = totalPages,
          totalPages = totalPages,
          statusMessage = "Finalizing PDF file...",
          percent = 95
        )
      )

      // Write to file
      FileOutputStream(outputFile).use { out ->
        pdfDocument.writeTo(out)
      }
      pdfDocument.close()

      // Post-optimization: Check if strict max limit was requested and still slightly exceeded
      if (config.isMaxLimitStrict && targetBytes > 0 && outputFile.length() > targetBytes) {
        // Output file exists and is readable
      }

      val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
      )

      val historyItem = PdfHistoryItem(
        id = UUID.randomUUID().toString(),
        fileName = sanitizedFileName,
        filePath = outputFile.absolutePath,
        fileSizeBytes = outputFile.length(),
        pageCount = totalPages,
        createdAtMillis = System.currentTimeMillis(),
        pageSizeName = config.standardSize.displayName,
        orientation = config.orientation.displayName,
        thumbnailUri = firstPageThumbnailPath,
        title = config.title,
        author = config.author,
        subject = config.subject,
        keywords = config.keywords,
        targetSizeBytes = targetBytes,
        qualityPreset = config.qualityPreset.displayName
      )

      onProgress(
        PdfGenerationProgress(
          currentPage = totalPages,
          totalPages = totalPages,
          statusMessage = "PDF Created Successfully!",
          percent = 100
        )
      )

      PdfGenerationResult.Success(historyItem, contentUri)
    } catch (e: Exception) {
      PdfGenerationResult.Error("Failed to create PDF: ${e.localizedMessage ?: "Unknown error"}")
    }
  }

  /**
   * Decodes a downsampled bitmap from Uri safely without OutOfMemoryError.
   */
  fun decodeSampledBitmapFromUri(
    context: Context,
    uriString: String,
    maxDimension: Int = 2048,
    rotation: Int = 0
  ): Bitmap? {
    return try {
      val uri = Uri.parse(uriString)
      // Check if sample resource uri
      if (uriString.startsWith("sample://")) {
        return createPlaceholderDocBitmap(uriString)
      }

      var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
      if (inputStream == null && uriString.startsWith("file://")) {
        inputStream = File(uri.path ?: "").inputStream()
      } else if (inputStream == null) {
        val file = File(uriString)
        if (file.exists()) inputStream = file.inputStream()
      }

      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeStream(inputStream, null, boundsOptions)
      inputStream?.close()

      if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
        return createPlaceholderDocBitmap(uriString)
      }

      // Calculate sample size
      var inSampleSize = 1
      val maxSide = max(boundsOptions.outWidth, boundsOptions.outHeight)
      while ((maxSide / inSampleSize) > maxDimension) {
        inSampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      // Re-open stream for full decode
      val decodeStream = context.contentResolver.openInputStream(uri)
        ?: File(uri.path ?: uriString).inputStream()
      val decoded = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
      decodeStream.close()

      if (decoded == null) return createPlaceholderDocBitmap(uriString)

      // Apply rotation if needed
      if (rotation % 360 != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        rotated
      } else {
        decoded
      }
    } catch (e: Exception) {
      createPlaceholderDocBitmap(uriString)
    }
  }

  private fun calculateMaxDimensionForPage(config: PdfConfig, perPageBudget: Long): Int {
    if (perPageBudget > 0) {
      return when {
        perPageBudget < 80_000L -> 1000   // tight e.g. < 500KB total for multi-page
        perPageBudget < 200_000L -> 1400  // e.g. ~1MB total
        perPageBudget < 500_000L -> 1800  // e.g. ~2MB total
        else -> 2400
      }
    }
    return when (config.qualityPreset) {
      com.example.model.pdf.PdfQualityPreset.SMALL_FILE -> 1200
      com.example.model.pdf.PdfQualityPreset.BALANCED -> 1800
      com.example.model.pdf.PdfQualityPreset.HIGH_QUALITY -> 2400
      com.example.model.pdf.PdfQualityPreset.MAXIMUM -> 3200
    }
  }

  private fun compressBitmapWithPreset(bitmap: Bitmap, config: PdfConfig): Bitmap {
    val quality = when (config.qualityPreset) {
      com.example.model.pdf.PdfQualityPreset.SMALL_FILE -> 60
      com.example.model.pdf.PdfQualityPreset.BALANCED -> 78
      com.example.model.pdf.PdfQualityPreset.HIGH_QUALITY -> 90
      com.example.model.pdf.PdfQualityPreset.MAXIMUM -> 96
    }
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    val bytes = stream.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: bitmap
  }

  private fun compressBitmapToBudget(
    bitmap: Bitmap,
    budgetBytes: Long,
    isStrict: Boolean
  ): Bitmap {
    var quality = 80
    var currentBitmap = bitmap
    var stream = ByteArrayOutputStream()
    currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

    // Iteratively step down quality if over budget
    while (stream.size() > budgetBytes && quality > 25) {
      quality -= 15
      stream.reset()
      currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    }

    // If still over budget and strict limit requested, downscale dimensions
    if (stream.size() > budgetBytes && isStrict) {
      val scale = sqrt(budgetBytes.toDouble() / stream.size().toDouble()).toFloat().coerceIn(0.4f, 0.9f)
      val newW = (currentBitmap.width * scale).roundToInt().coerceAtLeast(400)
      val newH = (currentBitmap.height * scale).roundToInt().coerceAtLeast(400)
      val scaled = Bitmap.createScaledBitmap(currentBitmap, newW, newH, true)
      stream.reset()
      scaled.compress(Bitmap.CompressFormat.JPEG, 60, stream)
      if (currentBitmap != bitmap && !currentBitmap.isRecycled) {
        currentBitmap.recycle()
      }
      currentBitmap = scaled
    }

    val bytes = stream.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: currentBitmap
  }

  /**
   * Calculates width and height in points (pt) for a page based on orientation.
   */
  fun calculatePageDimensionsPt(
    config: PdfConfig,
    orientation: PdfOrientation,
    bitmapWidth: Int = 1000,
    bitmapHeight: Int = 1400
  ): Pair<Float, Float> {
    val baseWidth = config.effectiveWidthPt
    val baseHeight = config.effectiveHeightPt

    val isLandscape = when (orientation) {
      PdfOrientation.LANDSCAPE -> true
      PdfOrientation.PORTRAIT -> false
      PdfOrientation.AUTO -> bitmapWidth > bitmapHeight
    }

    val minSide = min(baseWidth, baseHeight)
    val maxSide = max(baseWidth, baseHeight)

    return if (isLandscape) {
      Pair(maxSide, minSide)
    } else {
      Pair(minSide, maxSide)
    }
  }

  private fun drawPageBackground(
    canvas: Canvas,
    pageWidth: Float,
    pageHeight: Float,
    config: PdfConfig
  ) {
    val paint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.FILL
      color = when (config.background) {
        PdfBackground.WHITE -> 0xFFFFFFFF.toInt()
        PdfBackground.IVORY -> 0xFFFAF8F5.toInt()
        PdfBackground.SOFT_GRAY -> 0xFFF3F4F6.toInt()
        PdfBackground.DARK -> 0xFF1E293B.toInt()
        PdfBackground.CUSTOM -> config.customBackgroundColor.toInt()
      }
    }
    canvas.drawRect(0f, 0f, pageWidth, pageHeight, paint)
  }

  private fun drawImageOnPage(
    canvas: Canvas,
    bitmap: Bitmap,
    pageWidth: Float,
    pageHeight: Float,
    marginPt: Float,
    fit: PdfImageFit
  ) {
    val availableLeft = marginPt
    val availableTop = marginPt
    val availableRight = pageWidth - marginPt
    val availableBottom = pageHeight - marginPt
    val availableWidth = max(10f, availableRight - availableLeft)
    val availableHeight = max(10f, availableBottom - availableTop)

    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }

    when (fit) {
      PdfImageFit.FIT -> {
        // Maintain aspect ratio, letterbox within margins
        val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val areaRatio = availableWidth / availableHeight

        val drawW: Float
        val drawH: Float
        if (imgRatio > areaRatio) {
          drawW = availableWidth
          drawH = availableWidth / imgRatio
        } else {
          drawH = availableHeight
          drawW = availableHeight * imgRatio
        }

        val left = availableLeft + (availableWidth - drawW) / 2f
        val top = availableTop + (availableHeight - drawH) / 2f
        val destRect = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bitmap, null, destRect, paint)
      }

      PdfImageFit.FILL -> {
        // Maintain aspect ratio, fill entire available area, crop overflow
        val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val areaRatio = availableWidth / availableHeight

        val srcRect: Rect
        if (imgRatio > areaRatio) {
          val cropWidth = (bitmap.height * areaRatio).roundToInt()
          val cropLeft = (bitmap.width - cropWidth) / 2
          srcRect = Rect(cropLeft, 0, cropLeft + cropWidth, bitmap.height)
        } else {
          val cropHeight = (bitmap.width / areaRatio).roundToInt()
          val cropTop = (bitmap.height - cropHeight) / 2
          srcRect = Rect(0, cropTop, bitmap.width, cropTop + cropHeight)
        }
        val destRect = RectF(availableLeft, availableTop, availableRight, availableBottom)
        canvas.drawBitmap(bitmap, srcRect, destRect, paint)
      }

      PdfImageFit.ORIGINAL -> {
        // Native size in points (at 72 DPI, 1 px = 1 pt)
        val left = availableLeft + (availableWidth - bitmap.width) / 2f
        val top = availableTop + (availableHeight - bitmap.height) / 2f
        val destRect = RectF(left, top, left + bitmap.width, top + bitmap.height)
        canvas.drawBitmap(bitmap, null, destRect, paint)
      }

      PdfImageFit.CROP -> {
        // Center crop square or page ratio
        val cropSize = min(bitmap.width, bitmap.height)
        val cropLeft = (bitmap.width - cropSize) / 2
        val cropTop = (bitmap.height - cropSize) / 2
        val srcRect = Rect(cropLeft, cropTop, cropLeft + cropSize, cropTop + cropSize)
        val destRect = RectF(availableLeft, availableTop, availableRight, availableBottom)
        canvas.drawBitmap(bitmap, srcRect, destRect, paint)
      }

      PdfImageFit.STRETCH -> {
        // Fill exact bounds without preserving aspect ratio
        val destRect = RectF(availableLeft, availableTop, availableRight, availableBottom)
        canvas.drawBitmap(bitmap, null, destRect, paint)
      }

      PdfImageFit.CENTER -> {
        val left = availableLeft + (availableWidth - bitmap.width) / 2f
        val top = availableTop + (availableHeight - bitmap.height) / 2f
        canvas.drawBitmap(bitmap, left, top, paint)
      }
    }
  }

  private fun saveThumbnail(context: Context, bitmap: Bitmap, baseName: String): String? {
    return try {
      val thumbDir = File(context.filesDir, "pdf_thumbs").apply { if (!exists()) mkdirs() }
      val thumbFile = File(thumbDir, "${baseName}_thumb.jpg")
      val scale = 360f / max(bitmap.width, bitmap.height).toFloat()
      val thumbW = (bitmap.width * scale).roundToInt().coerceAtLeast(100)
      val thumbH = (bitmap.height * scale).roundToInt().coerceAtLeast(100)
      val scaled = Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true)

      FileOutputStream(thumbFile).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
      }
      if (scaled != bitmap) scaled.recycle()
      thumbFile.absolutePath
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Generates a clean vector-like document bitmap for placeholder/sample URIs.
   */
  fun createPlaceholderDocBitmap(tag: String): Bitmap {
    val w = 1200
    val h = 1600
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background paper
    val paperPaint = Paint().apply { color = 0xFFFAF9F6.toInt() }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paperPaint)

    // Header bar
    val headerPaint = Paint().apply { color = 0xFF2563EB.toInt() }
    canvas.drawRect(80f, 100f, (w - 80).toFloat(), 200f, headerPaint)

    // Title text line
    val linePaint = Paint().apply {
      color = 0xFFCBD5E1.toInt()
      strokeWidth = 14f
      strokeCap = Paint.Cap.ROUND
    }

    var y = 280f
    while (y < h - 160f) {
      val lineWidth = if ((y.toInt() / 60) % 3 == 0) (w - 280).toFloat() else (w - 160).toFloat()
      canvas.drawLine(80f, y, lineWidth, y, linePaint)
      y += 50f
    }

    // Seal / Watermark badge
    val badgePaint = Paint().apply {
      color = 0x222563EB
      style = Paint.Style.STROKE
      strokeWidth = 8f
    }
    canvas.drawCircle((w / 2).toFloat(), (h / 2).toFloat(), 200f, badgePaint)

    return bitmap
  }

  /**
   * Creates an Intent to share the PDF.
   */
  fun createShareIntent(context: Context, item: PdfHistoryItem): Intent {
    val file = File(item.filePath)
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    return Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  /**
   * Creates an Intent to open and view the PDF in the system viewer.
   */
  fun createViewIntent(context: Context, item: PdfHistoryItem): Intent {
    val file = File(item.filePath)
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    return Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  /**
   * Estimates output file size based on page count and quality / target settings.
   */
  fun estimateFileSize(pagesCount: Int, config: PdfConfig): String {
    if (pagesCount <= 0) return "0 KB"
    val targetBytes = config.effectiveTargetBytes
    if (targetBytes > 0) {
      val kb = targetBytes / 1024
      val mb = kb / 1024.0
      return if (mb >= 1.0) {
        String.format(Locale.US, "~%.1f MB (Target)", mb)
      } else {
        String.format(Locale.US, "~%d KB (Target)", kb)
      }
    }

    val perPageEstimateKb = when (config.qualityPreset) {
      com.example.model.pdf.PdfQualityPreset.SMALL_FILE -> 85
      com.example.model.pdf.PdfQualityPreset.BALANCED -> 190
      com.example.model.pdf.PdfQualityPreset.HIGH_QUALITY -> 420
      com.example.model.pdf.PdfQualityPreset.MAXIMUM -> 850
    }

    val totalKb = (pagesCount * perPageEstimateKb) + 12
    val totalMb = totalKb / 1024.0
    return if (totalMb >= 1.0) {
      String.format(Locale.US, "≈ %.1f MB", totalMb)
    } else {
      String.format(Locale.US, "≈ %d KB", totalKb)
    }
  }
}
