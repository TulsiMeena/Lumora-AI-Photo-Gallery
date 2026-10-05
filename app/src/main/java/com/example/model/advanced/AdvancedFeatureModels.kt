package com.example.model.advanced

import android.graphics.Bitmap
import android.graphics.PointF
import com.example.model.MediaItem

// --- 1. Storage Doctor & Duplicate Cleaner Models ---
enum class CleanerCategory(val label: String, val iconDescription: String) {
  DUPLICATES("Exact Duplicates", "Identical copies consuming double space"),
  SIMILAR("Similar / Burst Shots", "Repeated takes; keep the best one"),
  BLURRY("Blurry Photos", "Out-of-focus or motion-blurred shots"),
  DARK("Dark / Underexposed", "Accidental dark captures & pocket photos"),
  LARGE_VIDEOS("Heavy Videos", "Long videos taking gigabytes of space"),
  MEMES_CLUTTER("Screenshots & Memes", "Forwarded media & temporary screenshots")
}

data class DuplicateGroup(
  val id: String,
  val category: CleanerCategory,
  val items: List<MediaItem>,
  val recommendedKeepId: String? = null,
  val potentialSavingsBytes: Long = 0L
)

data class StorageDoctorScanResult(
  val duplicateGroups: List<DuplicateGroup> = emptyList(),
  val totalSavingsBytes: Long = 0L,
  val scannedCount: Int = 0,
  val isScanning: Boolean = false
) {
  fun formattedSavings(): String {
    val mb = totalSavingsBytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
      String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
    } else {
      String.format(java.util.Locale.US, "%.1f MB", mb)
    }
  }
}

// --- 2. Interactive Travel Map Models ---
data class GeoLocationPoint(
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double = 0.0,
  val locationName: String = "",
  val country: String = ""
)

data class GeoMediaItem(
  val media: MediaItem,
  val location: GeoLocationPoint
)

data class LocationCluster(
  val id: String,
  val title: String,
  val centerLat: Double,
  val centerLng: Double,
  val mediaItems: List<MediaItem>,
  val coverUri: String? = null
)

// --- 3. AI Magic Eraser Models ---
data class InpaintStroke(
  val points: List<PointF>,
  val strokeWidth: Float
)

data class MagicEraserState(
  val targetMedia: MediaItem? = null,
  val originalBitmap: Bitmap? = null,
  val processedBitmap: Bitmap? = null,
  val strokes: List<InpaintStroke> = emptyList(),
  val currentStrokeWidth: Float = 38f,
  val isProcessing: Boolean = false,
  val mode: EraserMode = EraserMode.OBJECT_REMOVE
)

enum class EraserMode {
  OBJECT_REMOVE,
  TEXT_REMOVE,
  BLEMISH_FIX,
  PHOTOBOMB_CLEAR
}

// --- 4. People & Pets Face Clustering Models ---
data class FaceCrop(
  val faceBitmap: Bitmap?,
  val confidence: Float,
  val boundsLeft: Float,
  val boundsTop: Float,
  val boundsRight: Float,
  val boundsBottom: Float
)

data class PersonProfile(
  val id: String,
  val name: String,
  val avatarUri: String? = null,
  val isPet: Boolean = false,
  val photos: List<MediaItem> = emptyList(),
  val relationshipTag: String = "Friend"
)

// --- 5. AI Cinematic Memories & Story Reels ---
enum class MemoryTheme(val title: String, val gradientColors: List<Long>) {
  ON_THIS_DAY("On This Day", listOf(0xFF8B5CF6, 0xFF3B82F6)),
  SUMMER_VIBES("Golden Hours", listOf(0xFFF59E0B, 0xFFEF4444)),
  WEEKEND_ESCAPE("Weekend Escape", listOf(0xFF10B981, 0xFF06B6D4)),
  BEST_OF_MONTH("Best of Month", listOf(0xFFEC4899, 0xFF8B5CF6)),
  YEAR_RECAP("Annual Highlights", listOf(0xFF6366F1, 0xFF14B8A6))
}

data class MemorySlide(
  val media: MediaItem,
  val caption: String,
  val dateFormatted: String,
  val locationLabel: String = ""
)

data class MemoryStory(
  val id: String,
  val theme: MemoryTheme,
  val title: String,
  val subtitle: String,
  val coverUri: String?,
  val slides: List<MemorySlide>
)

// --- 6. EXIF Privacy Shield Models ---
data class ExifPrivacyReport(
  val mediaId: String,
  val hasGps: Boolean,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val deviceModel: String? = null,
  val dateTaken: String? = null,
  val software: String? = null,
  val serialNumber: String? = null
)

// --- 7. Old Photo Restore & Colorizer Models ---
data class RestoreFilterConfig(
  val colorizeIntensity: Float = 0.85f,
  val warmLightTone: Float = 0.20f,
  val denoiseStrength: Float = 0.50f,
  val scratchReduction: Boolean = true,
  val faceEnhanceClarity: Float = 0.40f,
  val autoContrast: Boolean = true
)

// --- 8. Mini Video Studio Models ---
data class VideoTrimConfig(
  val startMs: Long = 0L,
  val endMs: Long = 10000L,
  val totalDurationMs: Long = 10000L,
  val removeAudio: Boolean = false,
  val playbackSpeed: Float = 1.0f
)

data class VideoGifExportConfig(
  val startMs: Long = 0L,
  val durationMs: Long = 4000L,
  val frameRate: Int = 12,
  val widthPx: Int = 480,
  val loopCount: Int = 0 // 0 = infinite
)
