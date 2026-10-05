package com.example.model.compression

import android.graphics.Color
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode
import kotlin.math.roundToInt

/**
 * Core operation mode for Compression Studio.
 */
enum class CompressionMode(val label: String, val description: String) {
  TARGET_SIZE("Target File Size", "Produce an output at or below your chosen target size (e.g. 500 KB, 1 MB) with best practical quality."),
  MAXIMUM_SIZE("Strict Maximum Size", "Guarantees the output will not exceed target size whenever technically possible."),
  QUALITY("Quality Mode", "Control compression ratio directly using precise quality slider (1-100%)."),
  SMART_COMPRESS("Smart Compression", "Automatically balances dimensions, format, and compression for optimal visual clarity."),
  CONVERT_FORMAT("Format Converter", "Convert between JPG, PNG, and WebP with transparency & codec optimization.")
}

/**
 * Target file size presets for rapid selection.
 */
enum class TargetSizePreset(val bytes: Long, val label: String, val subtitle: String) {
  KB_50(50 * 1024L, "50 KB", "Tiny avatar, signature & legacy portals"),
  KB_100(100 * 1024L, "100 KB", "Government exams & job application portals"),
  KB_200(200 * 1024L, "200 KB", "Standard visa & ID upload requirements"),
  KB_300(300 * 1024L, "300 KB", "Online documents & e-forms"),
  KB_500(500 * 1024L, "500 KB", "Email attachments & high-res web photos"),
  MB_1(1024 * 1024L, "1 MB", "Social media, WhatsApp & messaging apps"),
  MB_2(2 * 1024 * 1024L, "2 MB", "High-res presentation & printing"),
  MB_5(5 * 1024 * 1024L, "5 MB", "High-megapixel camera photo optimization"),
  MB_10(10 * 1024 * 1024L, "10 MB", "Large photo archive upload limit"),
  CUSTOM(0L, "Custom", "Set custom target size in KB or MB")
}

/**
 * Smart compression presets balancing quality, dimension scale and target size.
 */
enum class SmartCompressPreset(
  val label: String,
  val description: String,
  val defaultQuality: Int,
  val scaleFactor: Float,
  val recommendedTargetBytes: Long
) {
  BEST_QUALITY(
    label = "Best Quality",
    description = "Preserves 90%+ visual details, subtle compression with full sharpness",
    defaultQuality = 88,
    scaleFactor = 1.0f,
    recommendedTargetBytes = 2 * 1024 * 1024L
  ),
  BALANCED(
    label = "Balanced (Recommended)",
    description = "Optimal balance between visual quality and ~60-70% file size reduction",
    defaultQuality = 75,
    scaleFactor = 0.85f,
    recommendedTargetBytes = 750 * 1024L
  ),
  SMALL_FILE(
    label = "Small File",
    description = "Aggressive compression for low storage space and fast transmission",
    defaultQuality = 55,
    scaleFactor = 0.65f,
    recommendedTargetBytes = 300 * 1024L
  ),
  DOCUMENT(
    label = "Document / Scan",
    description = "Optimized for scanned pages, receipts & high-contrast text readability",
    defaultQuality = 70,
    scaleFactor = 0.80f,
    recommendedTargetBytes = 250 * 1024L
  ),
  WEBSITE(
    label = "Website / Blog",
    description = "Fast-loading web assets with balanced compression & clean edges",
    defaultQuality = 75,
    scaleFactor = 0.75f,
    recommendedTargetBytes = 400 * 1024L
  ),
  WHATSAPP(
    label = "WhatsApp / Chat",
    description = "Instant send size preventing WhatsApp auto-blur compression",
    defaultQuality = 70,
    scaleFactor = 0.70f,
    recommendedTargetBytes = 500 * 1024L
  ),
  EMAIL(
    label = "Email Attachment",
    description = "Universally compatible JPEG below typical corporate attachment limits",
    defaultQuality = 65,
    scaleFactor = 0.60f,
    recommendedTargetBytes = 350 * 1024L
  ),
  FORM_UPLOAD(
    label = "Government / Job Form",
    description = "Strictly fits under 100-200 KB government exam and passport portals",
    defaultQuality = 50,
    scaleFactor = 0.50f,
    recommendedTargetBytes = 100 * 1024L
  )
}

/**
 * Quality levels for manual quality mode.
 */
enum class QualityLevel(val label: String, val qualityValue: Int, val description: String) {
  MAXIMUM("Maximum", 95, "Near-lossless visual quality, minor file savings"),
  HIGH("High", 82, "Crisp clarity with substantial size reduction"),
  MEDIUM("Medium", 65, "Good quality for general sharing and web use"),
  LOW("Low", 38, "Aggressive size reduction, slight compression artifacts"),
  CUSTOM("Custom", 75, "User-selected quality between 1% and 100%")
}

/**
 * Status of target size achievement.
 */
enum class TargetStatus(val label: String, val isAchieved: Boolean) {
  ACHIEVED("Target achieved", true),
  NOT_REACHED("Target could not be reached without excessive quality loss", false),
  NOT_APPLICABLE("Quality-based output", true)
}

/**
 * Configuration payload for an image compression job.
 */
data class CompressionConfig(
  val mode: CompressionMode = CompressionMode.TARGET_SIZE,
  val targetSizeBytes: Long = 500 * 1024L,
  val isMaximumSizeMode: Boolean = true,
  val qualityPercent: Int = 80,
  val smartPreset: SmartCompressPreset = SmartCompressPreset.BALANCED,
  val outputFormat: OutputFormat = OutputFormat.JPEG,
  val webpLossless: Boolean = false,
  val transparentBgColor: Int = Color.WHITE,
  val preserveMetadata: Boolean = false,
  val removeMetadata: Boolean = true,
  val enableDimensionResize: Boolean = false,
  val customWidth: Int = 1080,
  val customHeight: Int = 1080,
  val lockAspectRatio: Boolean = true,
  val resizeMode: ResizeMode = ResizeMode.ORIGINAL_RATIO,
  val customPrefix: String = "",
  val customSuffix: String = "compressed"
)

/**
 * Status of a single item in a batch compression queue.
 */
enum class BatchItemStatus {
  PENDING,
  PROCESSING,
  SUCCESS,
  FAILED,
  CANCELLED
}

/**
 * Trackable item for batch processing.
 */
data class BatchCompressionItem(
  val id: String,
  val uri: String,
  val title: String,
  val originalSize: Long,
  val originalWidth: Int,
  val originalHeight: Int,
  val originalFormat: String,
  val status: BatchItemStatus = BatchItemStatus.PENDING,
  val outputUri: String? = null,
  val outputPath: String? = null,
  val outputSize: Long = 0L,
  val outputWidth: Int = 0,
  val outputHeight: Int = 0,
  val outputFormat: String = "JPEG",
  val qualityUsed: Int = 80,
  val targetAchieved: Boolean = true,
  val errorMessage: String? = null
) {
  val reductionPercent: Int
    get() {
      if (originalSize <= 0 || outputSize >= originalSize) return 0
      val saved = originalSize - outputSize
      return ((saved.toDouble() / originalSize.toDouble()) * 100).roundToInt().coerceIn(0, 99)
    }
}

/**
 * Result returned by the compression engine.
 */
data class CompressionExecutionResult(
  val isSuccess: Boolean,
  val originalUri: String,
  val originalTitle: String,
  val outputUri: String,
  val outputFilePath: String,
  val originalSizeBytes: Long,
  val outputSizeBytes: Long,
  val originalWidth: Int,
  val originalHeight: Int,
  val outputWidth: Int,
  val outputHeight: Int,
  val outputFormat: OutputFormat,
  val qualityUsed: Int,
  val targetAchieved: Boolean,
  val statusMessage: String,
  val errorMessage: String? = null
) {
  val reductionPercent: Int
    get() {
      if (originalSizeBytes <= 0 || outputSizeBytes >= originalSizeBytes) return 0
      val saved = originalSizeBytes - outputSizeBytes
      return ((saved.toDouble() / originalSizeBytes.toDouble()) * 100).roundToInt().coerceIn(0, 99)
    }
}

/**
 * Universal file size formatting helper.
 */
fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> "%.2f GB".format(gb)
    mb >= 1.0 -> "%.2f MB".format(mb)
    kb >= 1.0 -> "%.1f KB".format(kb)
    else -> "$bytes B"
  }
}
