package com.example.utils.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.model.MediaItem
import com.example.model.creative.ContactSheetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ContactSheetEngine {

  /**
   * Generates a Contact Sheet Bitmap containing a grid of labeled thumbnails.
   */
  suspend fun createContactSheetBitmap(
    context: Context,
    items: List<MediaItem>,
    config: ContactSheetConfig,
    pageWidth: Int = 2480, // A4-like resolution at 300 DPI (approx 2480 x 3508)
    pageHeight: Int = 3508
  ): Bitmap? = withContext(Dispatchers.IO) {
    try {
      if (items.isEmpty()) return@withContext null

      val outputBitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(outputBitmap)
      canvas.drawColor(config.backgroundColor)

      val margin = pageWidth * 0.04f
      val headerH = pageHeight * 0.07f

      // Draw Header Banner
      val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.textColor
        textSize = pageWidth * 0.024f
        isFakeBoldText = true
      }
      canvas.drawText(config.headerTitle, margin, margin + headerH * 0.45f, headerPaint)

      val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = pageWidth * 0.012f
      }
      val dateStr = "Generated on ${SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date())} • ${items.size} photos"
      canvas.drawText(dateStr, margin, margin + headerH * 0.75f, datePaint)

      // Divider line
      val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
      }
      canvas.drawLine(margin, margin + headerH, pageWidth - margin, margin + headerH, linePaint)

      // Grid Layout Calculations
      val cols = config.columns.coerceIn(2, 4)
      val availableW = pageWidth - (margin * 2)
      val cellSpacing = pageWidth * 0.02f
      val cellW = (availableW - (cellSpacing * (cols - 1))) / cols
      val thumbH = cellW * 0.75f // 4:3 thumbnail aspect
      val labelH = cellW * 0.35f
      val totalCellH = thumbH + labelH

      val startY = margin + headerH + cellSpacing

      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.textColor
        textSize = cellW * 0.065f
      }

      val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = cellW * 0.055f
      }

      for (i in items.indices) {
        val row = i / cols
        val col = i % cols

        val left = margin + col * (cellW + cellSpacing)
        val top = startY + row * (totalCellH + cellSpacing)

        if (top + totalCellH > pageHeight - margin) break // Fit single page

        val thumbRect = RectF(left, top, left + cellW, top + thumbH)

        // Draw thumbnail background placeholder
        val bgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        canvas.drawRect(thumbRect, bgPaint)

        // Load thumbnail
        val item = items[i]
        val bmp = loadThumbnail(context, item, cellW.toInt(), thumbH.toInt())
        if (bmp != null) {
          drawCenterCropped(canvas, bmp, thumbRect)
        }

        // Thumbnail border
        val borderPaint = Paint().apply {
          color = Color.parseColor("#CBD5E1")
          style = Paint.Style.STROKE
          strokeWidth = 1.5f
        }
        canvas.drawRect(thumbRect, borderPaint)

        // Metadata Labels
        var currentLabelY = top + thumbH + (cellW * 0.08f)

        if (config.showFilename) {
          val cleanTitle = if (item.title.length > 20) item.title.take(18) + "..." else item.title
          canvas.drawText(cleanTitle, left + 4f, currentLabelY, textPaint)
          currentLabelY += cellW * 0.07f
        }

        val metaTokens = mutableListOf<String>()
        if (config.showDate && item.dateTaken.isNotBlank()) metaTokens.add(item.dateTaken)
        if (config.showDimensions && item.width > 0 && item.height > 0) metaTokens.add("${item.width}x${item.height}")
        if (config.showFileSize && item.sizeBytes > 0) metaTokens.add(item.formattedSize)

        if (metaTokens.isNotEmpty()) {
          val metaLine = metaTokens.joinToString(" • ")
          val cleanMeta = if (metaLine.length > 26) metaLine.take(24) + "..." else metaLine
          canvas.drawText(cleanMeta, left + 4f, currentLabelY, metaPaint)
        }
      }

      outputBitmap
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Generates a Contact Sheet as a multi-page PDF document.
   */
  suspend fun createContactSheetPdf(
    context: Context,
    items: List<MediaItem>,
    config: ContactSheetConfig
  ): Result<File> = withContext(Dispatchers.IO) {
    try {
      val doc = PdfDocument()
      val pageWidth = 595 // A4 points (72 DPI standard)
      val pageHeight = 842

      val cols = config.columns.coerceIn(2, 4)
      val margin = 28f
      val headerH = 45f
      val cellSpacing = 14f

      val availableW = pageWidth - (margin * 2)
      val cellW = (availableW - (cellSpacing * (cols - 1))) / cols
      val thumbH = cellW * 0.75f
      val labelH = cellW * 0.32f
      val totalCellH = thumbH + labelH

      val availableH = pageHeight - (margin * 2) - headerH - cellSpacing
      val rowsPerPage = (availableH / (totalCellH + cellSpacing)).toInt().coerceAtLeast(1)
      val itemsPerPage = rowsPerPage * cols

      val totalPages = (items.size + itemsPerPage - 1) / itemsPerPage

      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.textColor
        textSize = 8f
      }

      val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 6.5f
      }

      for (pageNum in 0 until totalPages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum + 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(config.backgroundColor)

        // Header
        val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = config.textColor
          textSize = 14f
          isFakeBoldText = true
        }
        canvas.drawText("${config.headerTitle} (Page ${pageNum + 1}/$totalPages)", margin, margin + 20f, hPaint)
        canvas.drawLine(margin, margin + headerH, pageWidth - margin, margin + headerH, Paint().apply { color = Color.LTGRAY })

        val startIndex = pageNum * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, items.size)

        for (i in startIndex until endIndex) {
          val idxOnPage = i - startIndex
          val row = idxOnPage / cols
          val col = idxOnPage % cols

          val left = margin + col * (cellW + cellSpacing)
          val top = margin + headerH + cellSpacing + row * (totalCellH + cellSpacing)
          val thumbRect = RectF(left, top, left + cellW, top + thumbH)

          val item = items[i]
          val bmp = loadThumbnail(context, item, cellW.toInt() * 2, thumbH.toInt() * 2)
          if (bmp != null) {
            drawCenterCropped(canvas, bmp, thumbRect)
          }

          canvas.drawRect(thumbRect, Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE })

          var currentY = top + thumbH + 10f
          val cleanTitle = if (item.title.length > 18) item.title.take(16) + "..." else item.title
          canvas.drawText(cleanTitle, left + 2f, currentY, textPaint)
          currentY += 8f

          val metaStr = "${item.dateTaken} • ${item.formattedSize}"
          canvas.drawText(metaStr, left + 2f, currentY, metaPaint)
        }

        doc.finishPage(page)
      }

      val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
      val outputDir = File(documentsDir, "My Gallery AI/ContactSheet").apply { mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val outputFile = File(outputDir, "ContactSheet_$timeStamp.pdf")

      FileOutputStream(outputFile).use { fos ->
        doc.writeTo(fos)
      }
      doc.close()

      MediaScannerConnection.scanFile(
        context,
        arrayOf(outputFile.absolutePath),
        arrayOf("application/pdf"),
        null
      )

      Result.success(outputFile)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  /**
   * Saves contact sheet bitmap to Pictures/My Gallery AI/ContactSheet/
   */
  suspend fun saveContactSheetBitmap(
    context: Context,
    bitmap: Bitmap
  ): Result<Uri> = withContext(Dispatchers.IO) {
    try {
      val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val outputDir = File(picturesDir, "My Gallery AI/ContactSheet").apply { mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val outputFile = File(outputDir, "ContactSheet_$timeStamp.jpg")

      FileOutputStream(outputFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
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

  private fun drawCenterCropped(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
    val srcW = bitmap.width
    val srcH = bitmap.height
    val destW = destRect.width()
    val destH = destRect.height()

    val srcAspect = srcW.toFloat() / srcH
    val destAspect = destW / destH

    val srcCropRect = if (srcAspect > destAspect) {
      val cropW = (srcH * destAspect).toInt()
      val left = (srcW - cropW) / 2
      Rect(left, 0, left + cropW, srcH)
    } else {
      val cropH = (srcW / destAspect).toInt()
      val top = (srcH - cropH) / 2
      Rect(0, top, srcW, top + cropH)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, srcCropRect, destRect, paint)
  }

  private fun loadThumbnail(
    context: Context,
    item: MediaItem,
    targetWidth: Int,
    targetHeight: Int
  ): Bitmap? {
    return try {
      val uriStr = item.uri ?: return null
      val uri = Uri.parse(uriStr)
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
        inPreferredConfig = Bitmap.Config.RGB_565
      }

      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
      }
    } catch (_: Exception) {
      null
    }
  }
}
