package com.example.utils.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.example.model.compression.BatchCompressionItem
import com.example.model.compression.BatchItemStatus
import com.example.model.compression.CompressionConfig
import com.example.model.compression.CompressionExecutionResult
import com.example.model.compression.CompressionMode
import com.example.model.compression.SmartCompressPreset
import com.example.model.compression.formatFileSize
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode
import com.example.utils.resize.ImageResizeEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageCompressionEngine {

  const val MIN_QUALITY_FLOOR = 15
  const val MAX_QUALITY_CEILING = 98
  const val MIN_SAFE_DIMENSION = 240

  /**
   * Estimates the output file size for a given bitmap and settings in bytes.
   */
  suspend fun estimateCompressedSize(
    bitmap: Bitmap,
    outputFormat: OutputFormat,
    quality: Int,
    webpLossless: Boolean = false,
    transparentBgColor: Int = Color.WHITE
  ): Long = withContext(Dispatchers.Default) {
    val preparedBitmap = prepareBitmapForFormat(bitmap, outputFormat, transparentBgColor)
    val baos = ByteArrayOutputStream()
    val compressFormat = resolveCompressFormat(outputFormat, webpLossless)
    val effectiveQuality = if (outputFormat == OutputFormat.PNG) 100 else quality.coerceIn(1, 100)

    preparedBitmap.compress(compressFormat, effectiveQuality, baos)
    val size = baos.size().toLong()
    if (preparedBitmap != bitmap) {
      preparedBitmap.recycle()
    }
    size
  }

  /**
   * Generates a preview bitmap showing the visual effects of compression.
   */
  suspend fun generateCompressedPreview(
    bitmap: Bitmap,
    outputFormat: OutputFormat,
    quality: Int,
    webpLossless: Boolean = false,
    transparentBgColor: Int = Color.WHITE
  ): Bitmap? = withContext(Dispatchers.Default) {
    try {
      val prepared = prepareBitmapForFormat(bitmap, outputFormat, transparentBgColor)
      val baos = ByteArrayOutputStream()
      val compressFormat = resolveCompressFormat(outputFormat, webpLossless)
      val effectiveQuality = if (outputFormat == OutputFormat.PNG) 100 else quality.coerceIn(1, 100)

      prepared.compress(compressFormat, effectiveQuality, baos)
      val bytes = baos.toByteArray()
      if (prepared != bitmap) prepared.recycle()

      android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Main execution function for compressing and/or converting a single image.
   * Never overwrites original file.
   */
  suspend fun compressImage(
    context: Context,
    sourceUri: Uri,
    config: CompressionConfig,
    originalTitle: String = "photo"
  ): CompressionExecutionResult = withContext(Dispatchers.IO) {
    // 1. Load source bitmap safely
    val originalMetadata = ImageResizeEngine.readImageMetadata(context, sourceUri)
    val sourceBitmap = ImageResizeEngine.loadSafeBitmap(context, sourceUri, maxTargetDim = 4096)
      ?: return@withContext CompressionExecutionResult(
        isSuccess = false,
        originalUri = sourceUri.toString(),
        originalTitle = originalTitle,
        outputUri = "",
        outputFilePath = "",
        originalSizeBytes = originalMetadata.sizeBytes,
        outputSizeBytes = 0L,
        originalWidth = originalMetadata.widthPx,
        originalHeight = originalMetadata.heightPx,
        outputWidth = 0,
        outputHeight = 0,
        outputFormat = config.outputFormat,
        qualityUsed = 0,
        targetAchieved = false,
        statusMessage = "Could not decode image safely.",
        errorMessage = "Memory or format error decoding bitmap."
      )

    val origW = sourceBitmap.width
    val origH = sourceBitmap.height
    val origSize = if (originalMetadata.sizeBytes > 0) originalMetadata.sizeBytes else (origW * origH * 4L)

    try {
      // 2. Perform dimension resize if requested
      var workingBitmap: Bitmap = sourceBitmap
      if (config.enableDimensionResize && config.customWidth > 0 && config.customHeight > 0) {
        val resized = ImageResizeEngine.resizeBitmap(
          source = workingBitmap,
          targetWidth = config.customWidth,
          targetHeight = config.customHeight,
          mode = config.resizeMode,
          backgroundColor = config.transparentBgColor
        )
        if (resized != workingBitmap && workingBitmap != sourceBitmap) {
          workingBitmap.recycle()
        }
        workingBitmap = resized
      } else if (config.mode == CompressionMode.SMART_COMPRESS && config.smartPreset.scaleFactor < 1.0f) {
        // Smart scale dimensions
        val targetW = max(MIN_SAFE_DIMENSION, (origW * config.smartPreset.scaleFactor).roundToInt())
        val targetH = max(MIN_SAFE_DIMENSION, (origH * config.smartPreset.scaleFactor).roundToInt())
        val scaled = Bitmap.createScaledBitmap(workingBitmap, targetW, targetH, true)
        if (scaled != workingBitmap && workingBitmap != sourceBitmap) {
          workingBitmap.recycle()
        }
        workingBitmap = scaled
      }

      // 3. Execute Target-Size Search or Quality Compression
      val compressionOutput = executeTargetOptimization(
        workingBitmap = workingBitmap,
        config = config,
        originalSizeBytes = origSize
      )

      val finalBitmap = compressionOutput.finalBitmap
      val compressedBytes = compressionOutput.compressedBytes
      val qualityUsed = compressionOutput.qualityUsed
      val targetAchieved = compressionOutput.targetAchieved

      // 4. Determine save directory & file naming
      val outputFormat = config.outputFormat
      val extension = outputFormat.extension
      val baseCleanName = originalTitle.substringBeforeLast(".").ifBlank { "compressed" }
      val suffix = if (config.customSuffix.isNotBlank()) "_${config.customSuffix}" else "_compressed"
      val prefix = if (config.customPrefix.isNotBlank()) "${config.customPrefix}_" else ""

      val outputDir = getOutputCompressedDirectory(context)
      if (!outputDir.exists()) outputDir.mkdirs()

      var candidateFile = File(outputDir, "$prefix$baseCleanName$suffix$extension")
      var counter = 1
      while (candidateFile.exists()) {
        candidateFile = File(outputDir, "$prefix$baseCleanName$suffix" + "_$counter$extension")
        counter++
      }

      // 5. Write bytes to new file safely
      FileOutputStream(candidateFile).use { fos ->
        fos.write(compressedBytes)
        fos.flush()
      }

      // 6. Metadata preservation / stripping
      if (config.preserveMetadata && !config.removeMetadata) {
        tryCopyExif(context, sourceUri, candidateFile.absolutePath)
      }

      // 7. Notify media scanner so file appears in gallery
      MediaScannerConnection.scanFile(
        context,
        arrayOf(candidateFile.absolutePath),
        arrayOf(outputFormat.mimeType),
        null
      )

      val outputUri = Uri.fromFile(candidateFile).toString()
      val outputSize = candidateFile.length()

      // Cleanup working bitmap if created
      if (finalBitmap != sourceBitmap) {
        finalBitmap.recycle()
      }
      if (workingBitmap != sourceBitmap && workingBitmap != finalBitmap) {
        workingBitmap.recycle()
      }
      sourceBitmap.recycle()

      val statusMsg = when {
        config.mode == CompressionMode.TARGET_SIZE || config.mode == CompressionMode.MAXIMUM_SIZE -> {
          if (targetAchieved) "Target achieved: ${formatFileSize(outputSize)}"
          else "Target could not be reached without excessive quality loss"
        }
        config.mode == CompressionMode.CONVERT_FORMAT -> "Successfully converted to ${outputFormat.displayName}"
        config.mode == CompressionMode.SMART_COMPRESS -> "Smart compressed with ${config.smartPreset.label}"
        else -> "Compressed at $qualityUsed% quality"
      }

      CompressionExecutionResult(
        isSuccess = true,
        originalUri = sourceUri.toString(),
        originalTitle = originalTitle,
        outputUri = outputUri,
        outputFilePath = candidateFile.absolutePath,
        originalSizeBytes = origSize,
        outputSizeBytes = outputSize,
        originalWidth = origW,
        originalHeight = origH,
        outputWidth = finalBitmap.width,
        outputHeight = finalBitmap.height,
        outputFormat = outputFormat,
        qualityUsed = qualityUsed,
        targetAchieved = targetAchieved,
        statusMessage = statusMsg
      )
    } catch (e: Exception) {
      e.printStackTrace()
      CompressionExecutionResult(
        isSuccess = false,
        originalUri = sourceUri.toString(),
        originalTitle = originalTitle,
        outputUri = "",
        outputFilePath = "",
        originalSizeBytes = origSize,
        outputSizeBytes = 0L,
        originalWidth = origW,
        originalHeight = origH,
        outputWidth = 0,
        outputHeight = 0,
        outputFormat = config.outputFormat,
        qualityUsed = 0,
        targetAchieved = false,
        statusMessage = "Compression error: ${e.localizedMessage ?: "Unknown"}",
        errorMessage = e.message
      )
    }
  }

  /**
   * Helper class holding intermediate optimization results.
   */
  data class TargetOptimizationOutput(
    val finalBitmap: Bitmap,
    val compressedBytes: ByteArray,
    val qualityUsed: Int,
    val targetAchieved: Boolean
  )

  /**
   * Iterative compression algorithm to achieve target file size.
   */
  private suspend fun executeTargetOptimization(
    workingBitmap: Bitmap,
    config: CompressionConfig,
    originalSizeBytes: Long
  ): TargetOptimizationOutput = withContext(Dispatchers.Default) {
    val outputFormat = config.outputFormat
    val targetBytes = if (config.targetSizeBytes > 0) config.targetSizeBytes else config.smartPreset.recommendedTargetBytes
    val compressFormat = resolveCompressFormat(outputFormat, config.webpLossless)

    // For PNG: PNG is lossless on Android, quality parameter is ignored
    if (outputFormat == OutputFormat.PNG) {
      val prepared = prepareBitmapForFormat(workingBitmap, outputFormat, config.transparentBgColor)
      val baos = ByteArrayOutputStream()
      prepared.compress(compressFormat, 100, baos)
      val bytes = baos.toByteArray()
      val achieved = bytes.size <= targetBytes || config.mode == CompressionMode.QUALITY || config.mode == CompressionMode.CONVERT_FORMAT
      return@withContext TargetOptimizationOutput(
        finalBitmap = prepared,
        compressedBytes = bytes,
        qualityUsed = 100,
        targetAchieved = achieved
      )
    }

    // If user specified Quality Mode or Format Converter without target sizing:
    if (config.mode == CompressionMode.QUALITY ||
      (config.mode == CompressionMode.CONVERT_FORMAT && !config.isMaximumSizeMode)
    ) {
      val quality = config.qualityPercent.coerceIn(1, 100)
      val prepared = prepareBitmapForFormat(workingBitmap, outputFormat, config.transparentBgColor)
      val baos = ByteArrayOutputStream()
      prepared.compress(compressFormat, quality, baos)
      return@withContext TargetOptimizationOutput(
        finalBitmap = prepared,
        compressedBytes = baos.toByteArray(),
        qualityUsed = quality,
        targetAchieved = true
      )
    }

    // Target Size or Maximum Size or Smart Compress iterative search:
    var currentBitmap = prepareBitmapForFormat(workingBitmap, outputFormat, config.transparentBgColor)
    var lowQ = MIN_QUALITY_FLOOR
    var highQ = MAX_QUALITY_CEILING
    var bestQuality = config.qualityPercent.coerceIn(lowQ, highQ)
    var bestBytes: ByteArray? = null
    var bestAchieved = false

    // Iterative quality search (up to 7 iterations)
    var iterations = 0
    while (lowQ <= highQ && iterations < 7) {
      iterations++
      val midQ = (lowQ + highQ) / 2
      val baos = ByteArrayOutputStream()
      currentBitmap.compress(compressFormat, midQ, baos)
      val bytes = baos.toByteArray()
      val size = bytes.size.toLong()

      if (size <= targetBytes) {
        // Fits under target! Try to search for higher quality while still staying <= targetBytes
        bestQuality = midQ
        bestBytes = bytes
        bestAchieved = true
        lowQ = midQ + 1
      } else {
        // Exceeds target, reduce quality
        highQ = midQ - 1
      }
    }

    // If still exceeds target and strict maximum size or smart compress is enabled:
    // Try downsampling dimensions up to 3 times (0.80x, 0.65x, 0.50x)
    if (!bestAchieved && (config.isMaximumSizeMode || config.mode == CompressionMode.SMART_COMPRESS || !config.enableDimensionResize)) {
      val scaleRatios = floatArrayOf(0.80f, 0.65f, 0.50f, 0.40f)
      for (scale in scaleRatios) {
        val newW = max(MIN_SAFE_DIMENSION, (currentBitmap.width * scale).roundToInt())
        val newH = max(MIN_SAFE_DIMENSION, (currentBitmap.height * scale).roundToInt())
        if (newW < MIN_SAFE_DIMENSION || newH < MIN_SAFE_DIMENSION) break

        val scaled = Bitmap.createScaledBitmap(currentBitmap, newW, newH, true)
        // Test with moderate quality first
        for (testQ in intArrayOf(75, 55, 35, MIN_QUALITY_FLOOR)) {
          val baos = ByteArrayOutputStream()
          scaled.compress(compressFormat, testQ, baos)
          val bytes = baos.toByteArray()
          if (bytes.size <= targetBytes) {
            if (currentBitmap != workingBitmap) currentBitmap.recycle()
            currentBitmap = scaled
            bestQuality = testQ
            bestBytes = bytes
            bestAchieved = true
            break
          }
        }
        if (bestAchieved) break
        if (scaled != currentBitmap && scaled != workingBitmap) scaled.recycle()
      }
    }

    // Fallback if target still could not be reached without excessive quality degradation:
    if (bestBytes == null) {
      val baos = ByteArrayOutputStream()
      bestQuality = MIN_QUALITY_FLOOR
      currentBitmap.compress(compressFormat, bestQuality, baos)
      bestBytes = baos.toByteArray()
      bestAchieved = false
    }

    TargetOptimizationOutput(
      finalBitmap = currentBitmap,
      compressedBytes = bestBytes,
      qualityUsed = bestQuality,
      targetAchieved = bestAchieved
    )
  }

  /**
   * Prepares a bitmap for output: handles background fill for transparent formats converting to JPEG.
   */
  fun prepareBitmapForFormat(
    source: Bitmap,
    targetFormat: OutputFormat,
    transparentBgColor: Int = Color.WHITE
  ): Bitmap {
    if (targetFormat == OutputFormat.JPEG && source.hasAlpha()) {
      val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(result)
      canvas.drawColor(transparentBgColor)
      canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
      return result
    }
    return source
  }

  /**
   * Resolves appropriate Bitmap.CompressFormat for Android SDK levels.
   */
  private fun resolveCompressFormat(format: OutputFormat, webpLossless: Boolean): Bitmap.CompressFormat {
    return when (format) {
      OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
      OutputFormat.PNG -> Bitmap.CompressFormat.PNG
      OutputFormat.WEBP -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          if (webpLossless) Bitmap.CompressFormat.WEBP_LOSSLESS
          else Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          @Suppress("DEPRECATION")
          Bitmap.CompressFormat.WEBP
        }
      }
    }
  }

  /**
   * Safely copies non-location EXIF metadata from source to output file.
   */
  private fun tryCopyExif(context: Context, sourceUri: Uri, destinationPath: String) {
    try {
      context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
        val srcExif = ExifInterface(inStream)
        val dstExif = ExifInterface(destinationPath)

        val tagsToCopy = arrayOf(
          ExifInterface.TAG_DATETIME,
          ExifInterface.TAG_MAKE,
          ExifInterface.TAG_MODEL,
          ExifInterface.TAG_FLASH,
          ExifInterface.TAG_FOCAL_LENGTH,
          ExifInterface.TAG_WHITE_BALANCE,
          ExifInterface.TAG_EXPOSURE_TIME,
          ExifInterface.TAG_F_NUMBER,
          ExifInterface.TAG_ISO_SPEED_RATINGS
        )

        for (tag in tagsToCopy) {
          srcExif.getAttribute(tag)?.let { value ->
            dstExif.setAttribute(tag, value)
          }
        }
        // Force output orientation to NORMAL because pixels were already transformed
        dstExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        dstExif.saveAttributes()
      }
    } catch (_: Exception) {}
  }

  /**
   * Batch compression queue processor with cancellation and item status reporting.
   */
  suspend fun processBatchCompression(
    context: Context,
    items: List<BatchCompressionItem>,
    config: CompressionConfig,
    onProgress: (completedCount: Int, totalCount: Int, currentItem: BatchCompressionItem) -> Unit
  ): List<BatchCompressionItem> = withContext(Dispatchers.IO) {
    val results = mutableListOf<BatchCompressionItem>()

    // Check available storage
    val usableSpace = Environment.getDataDirectory().usableSpace
    if (usableSpace < 20 * 1024 * 1024L) { // Less than 20MB
      return@withContext items.map {
        it.copy(
          status = BatchItemStatus.FAILED,
          errorMessage = "Not enough storage space to safely process images."
        )
      }
    }

    var completed = 0
    val total = items.size

    for (item in items) {
      coroutineContext.ensureActive()

      val itemUri = Uri.parse(item.uri)
      onProgress(completed, total, item.copy(status = BatchItemStatus.PROCESSING))

      try {
        val result = compressImage(
          context = context,
          sourceUri = itemUri,
          config = config,
          originalTitle = item.title
        )

        if (result.isSuccess) {
          val updated = item.copy(
            status = BatchItemStatus.SUCCESS,
            outputUri = result.outputUri,
            outputPath = result.outputFilePath,
            outputSize = result.outputSizeBytes,
            outputWidth = result.outputWidth,
            outputHeight = result.outputHeight,
            outputFormat = result.outputFormat.displayName,
            qualityUsed = result.qualityUsed,
            targetAchieved = result.targetAchieved
          )
          results.add(updated)
          completed++
          onProgress(completed, total, updated)
        } else {
          val failedItem = item.copy(
            status = BatchItemStatus.FAILED,
            errorMessage = result.statusMessage
          )
          results.add(failedItem)
          completed++
          onProgress(completed, total, failedItem)
        }
      } catch (ce: CancellationException) {
        results.add(item.copy(status = BatchItemStatus.CANCELLED, errorMessage = "Cancelled by user."))
        throw ce
      } catch (e: Exception) {
        val failed = item.copy(
          status = BatchItemStatus.FAILED,
          errorMessage = e.localizedMessage ?: "Processing error"
        )
        results.add(failed)
        completed++
        onProgress(completed, total, failed)
      }
    }

    results
  }

  fun getOutputCompressedDirectory(context: Context): File {
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val appDir = File(picturesDir, "My Gallery AI/Compressed")
    return if (picturesDir.canWrite() || appDir.mkdirs()) {
      appDir
    } else {
      File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Compressed").apply { mkdirs() }
    }
  }
}
