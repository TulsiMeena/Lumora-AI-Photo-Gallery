package com.example.utils.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.model.MediaItem
import com.example.model.compression.CompressionConfig
import com.example.model.compression.CompressionMode
import com.example.model.compression.TargetSizePreset
import com.example.model.creative.BatchAdjustmentConfig
import com.example.model.creative.BatchJob
import com.example.model.creative.BatchJobStatus
import com.example.model.creative.BatchOperation
import com.example.model.creative.BatchRenameConfig
import com.example.model.creative.BatchResizeParams
import com.example.model.creative.ContactSheetConfig
import com.example.model.creative.SignatureConfig
import com.example.model.creative.WatermarkConfig
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode
import com.example.utils.compression.ImageCompressionEngine
import com.example.utils.resize.ImageResizeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object BatchProcessingEngine {

  /**
   * Executes a batch job in the background and reports live progress.
   */
  suspend fun executeBatchJob(
    context: Context,
    items: List<MediaItem>,
    operation: BatchOperation,
    renameConfig: BatchRenameConfig? = null,
    resizeConfig: BatchResizeParams? = null,
    compressionConfig: CompressionConfig? = null,
    watermarkConfig: WatermarkConfig? = null,
    signatureConfig: SignatureConfig? = null,
    adjustmentConfig: BatchAdjustmentConfig? = null,
    contactSheetConfig: ContactSheetConfig? = null,
    onProgress: (BatchJob) -> Unit
  ): BatchJob = withContext(Dispatchers.IO) {
    val jobId = UUID.randomUUID().toString()
    var completedCount = 0
    var failedCount = 0
    val outputUris = mutableListOf<String>()
    val errorDetails = mutableListOf<String>()

    var currentJob = BatchJob(
      id = jobId,
      operation = operation,
      totalItems = items.size,
      completedItems = 0,
      failedItems = 0,
      status = BatchJobStatus.RUNNING,
      startTimeMillis = System.currentTimeMillis()
    )
    onProgress(currentJob)

    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val batchOutputDir = File(picturesDir, "My Gallery AI/Batch_${operation.name}").apply { mkdirs() }

    // Contact Sheet operation combines all in one pass
    if (operation == BatchOperation.CONTACT_SHEET) {
      val cfg = contactSheetConfig ?: ContactSheetConfig()
      val bmp = ContactSheetEngine.createContactSheetBitmap(context, items, cfg)
      if (bmp != null) {
        val saveResult = ContactSheetEngine.saveContactSheetBitmap(context, bmp)
        if (saveResult.isSuccess) {
          completedCount = items.size
          outputUris.add(saveResult.getOrThrow().toString())
        } else {
          failedCount = items.size
          errorDetails.add("Failed to save contact sheet.")
        }
      } else {
        failedCount = items.size
        errorDetails.add("Failed to render contact sheet.")
      }

      currentJob = currentJob.copy(
        completedItems = completedCount,
        failedItems = failedCount,
        status = if (failedCount == 0) BatchJobStatus.COMPLETED else BatchJobStatus.FAILED,
        outputUris = outputUris,
        errorDetails = errorDetails
      )
      onProgress(currentJob)
      return@withContext currentJob
    }

    for (index in items.indices) {
      val item = items[index]
      val uriStr = item.uri

      currentJob = currentJob.copy(
        completedItems = completedCount,
        failedItems = failedCount,
        currentFileName = item.title
      )
      onProgress(currentJob)

      if (uriStr == null) {
        failedCount++
        errorDetails.add("Item ${item.title}: URI is missing.")
        continue
      }

      try {
        val sourceUri = Uri.parse(uriStr)
        var success = false

        when (operation) {
          BatchOperation.RENAME -> {
            val cfg = renameConfig ?: BatchRenameConfig()
            val numStr = (cfg.startNumber + index).toString().padStart(cfg.digitsCount, '0')
            val ext = if (cfg.preserveExtension) (item.uri?.substringAfterLast('.', "jpg") ?: "jpg") else "jpg"
            val newName = "${cfg.prefix}$numStr${cfg.suffix}.$ext"
            val destFile = File(batchOutputDir, newName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
              FileOutputStream(destFile).use { output ->
                input.copyTo(output)
              }
            }

            if (destFile.exists()) {
              MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
              outputUris.add(Uri.fromFile(destFile).toString())
              success = true
            }
          }

          BatchOperation.RESIZE -> {
            val cfg = resizeConfig ?: BatchResizeParams()
            val bmp = ImageResizeEngine.loadSafeBitmap(context, sourceUri, maxOf(cfg.targetWidth, cfg.targetHeight))
            if (bmp != null) {
              val resized = ImageResizeEngine.resizeBitmap(
                source = bmp,
                targetWidth = cfg.targetWidth,
                targetHeight = cfg.targetHeight,
                mode = cfg.resizeMode
              )
              val destFile = File(batchOutputDir, "Resized_${item.title}.jpg")
              FileOutputStream(destFile).use { fos ->
                resized.compress(Bitmap.CompressFormat.JPEG, cfg.quality, fos)
              }
              if (resized != bmp) resized.recycle()
              bmp.recycle()
              if (destFile.exists()) {
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("image/jpeg"), null)
                outputUris.add(Uri.fromFile(destFile).toString())
                success = true
              }
            }
          }

          BatchOperation.COMPRESS, BatchOperation.CONVERT -> {
            val cfg = compressionConfig ?: CompressionConfig(
              mode = CompressionMode.TARGET_SIZE,
              targetSizeBytes = 500 * 1024L,
              outputFormat = OutputFormat.JPEG
            )
            val res = ImageCompressionEngine.compressImage(context, sourceUri, cfg, item.title)
            if (res.isSuccess && res.outputFilePath.isNotBlank()) {
              val destFile = File(res.outputFilePath)
              if (destFile.exists()) {
                outputUris.add(Uri.fromFile(destFile).toString())
                success = true
              }
            }
          }

          BatchOperation.WATERMARK -> {
            val cfg = watermarkConfig ?: WatermarkConfig()
            val bmp = decodeBitmap(context, sourceUri)
            if (bmp != null) {
              val watermarked = WatermarkEngine.applyWatermark(bmp, cfg)
              val destFile = File(batchOutputDir, "WM_${item.title}.jpg")
              FileOutputStream(destFile).use { fos ->
                watermarked.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("image/jpeg"), null)
              outputUris.add(Uri.fromFile(destFile).toString())
              success = true
            }
          }

          BatchOperation.SIGNATURE -> {
            val cfg = signatureConfig ?: SignatureConfig()
            val bmp = decodeBitmap(context, sourceUri)
            if (bmp != null) {
              val signed = WatermarkEngine.applySignature(bmp, cfg)
              val destFile = File(batchOutputDir, "Sign_${item.title}.jpg")
              FileOutputStream(destFile).use { fos ->
                signed.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("image/jpeg"), null)
              outputUris.add(Uri.fromFile(destFile).toString())
              success = true
            }
          }

          BatchOperation.ADJUSTMENTS -> {
            val cfg = adjustmentConfig ?: BatchAdjustmentConfig()
            val bmp = decodeBitmap(context, sourceUri)
            if (bmp != null) {
              val adjusted = applyAdjustmentsToBitmap(bmp, cfg)
              val destFile = File(batchOutputDir, "Adj_${item.title}.jpg")
              FileOutputStream(destFile).use { fos ->
                adjusted.compress(Bitmap.CompressFormat.JPEG, 92, fos)
              }
              MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("image/jpeg"), null)
              outputUris.add(Uri.fromFile(destFile).toString())
              success = true
            }
          }

          else -> {
            success = false
          }
        }

        if (success) {
          completedCount++
        } else {
          failedCount++
          errorDetails.add("${item.title}: processing returned no output.")
        }
      } catch (e: Exception) {
        e.printStackTrace()
        failedCount++
        errorDetails.add("${item.title}: ${e.message ?: "Unknown error"}")
      }
    }

    currentJob = currentJob.copy(
      completedItems = completedCount,
      failedItems = failedCount,
      currentFileName = "",
      status = if (failedCount == items.size) BatchJobStatus.FAILED else BatchJobStatus.COMPLETED,
      outputUris = outputUris,
      errorDetails = errorDetails
    )
    onProgress(currentJob)

    currentJob
  }

  private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
      val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun applyAdjustmentsToBitmap(source: Bitmap, config: BatchAdjustmentConfig): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val cm = ColorMatrix()

    // Contrast
    val scale = config.contrast
    val translate = (-0.5f * scale + 0.5f) * 255f
    val contrastMatrix = ColorMatrix(floatArrayOf(
      scale, 0f, 0f, 0f, translate + config.brightness,
      0f, scale, 0f, 0f, translate + config.brightness,
      0f, 0f, scale, 0f, translate + config.brightness,
      0f, 0f, 0f, 1f, 0f
    ))
    cm.postConcat(contrastMatrix)

    // Saturation
    val satMatrix = ColorMatrix().apply { setSaturation(config.saturation) }
    cm.postConcat(satMatrix)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter = ColorMatrixColorFilter(cm)
    }

    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
  }
}
