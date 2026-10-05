package com.example.model

enum class MediaType {
  PHOTO, VIDEO
}

data class MediaItem(
  val id: String,
  val title: String,
  val mediaType: MediaType = MediaType.PHOTO,
  val uri: String? = null,
  val durationSeconds: Int = 0,
  val dateTaken: String = "",
  val dateTakenMillis: Long = 0L,
  val sizeBytes: Long = 0L,
  val width: Int = 1920,
  val height: Int = 1080,
  val mimeType: String = "image/jpeg",
  val isFavorite: Boolean = false,
  val isExcluded: Boolean = false,
  val albumName: String = "Camera",
  val bucketId: String = "",
  val resolution: String = "4000 x 3000 (12 MP)",
  val cameraModel: String = "Camera",
  val iso: String = "ISO 100",
  val shutterSpeed: String = "1/120s",
  val locationName: String = "",
  val categoryTag: String = "General",
  val colorSeed: Int = 0,
  val isEdited: Boolean = false,
  val semanticTags: List<String> = emptyList(),
  val detectedObjects: List<String> = emptyList(),
  val detectedText: String = "",
  val dominantColor: String = ""
) {
  val formattedDuration: String
    get() {
      if (mediaType != MediaType.VIDEO) return ""
      val mins = durationSeconds / 60
      val secs = durationSeconds % 60
      return "%d:%02d".format(mins, secs)
    }

  val formattedSize: String
    get() {
      val mb = sizeBytes / (1024.0 * 1024.0)
      return if (mb < 1.0) {
        val kb = sizeBytes / 1024
        "$kb KB"
      } else {
        "%.1f MB".format(mb)
      }
    }
}

data class TimelineSection(
  val title: String,
  val count: Int,
  val items: List<MediaItem>
)

enum class AlbumType {
  ALL, VIDEOS, FAVORITES, SCREENSHOTS, DOWNLOADS, WHATSAPP, CAMERA, DOCUMENTS, RECENT_EDITED, RECENT_ADDED, CUSTOM
}

data class AlbumItem(
  val id: String,
  val name: String,
  val count: Int,
  val albumType: AlbumType,
  val colorSeed: Int = 0,
  val description: String = "",
  val coverUri: String? = null,
  val bucketId: String? = null,
  val isCustom: Boolean = false,
  val customAlbumId: String? = null
)

enum class DocumentFormat {
  PDF, SCAN, IMAGE
}

object DocumentCategories {
  const val ALL = "All Documents"
  const val SCANNED = "Scanned Documents"
  const val ID_DOCUMENTS = "ID Documents"
  const val NOTES = "Notes"
  const val RECEIPTS = "Receipts"
  const val FORMS = "Forms"
  const val CERTIFICATES = "Certificates"
  const val OTHER = "Other Documents"
  const val RECENT = "Recent Documents"

  val ALL_CATEGORIES = listOf(
    ALL,
    SCANNED,
    ID_DOCUMENTS,
    NOTES,
    RECEIPTS,
    FORMS,
    CERTIFICATES,
    OTHER,
    RECENT
  )

  val EDITABLE_CATEGORIES = listOf(
    ID_DOCUMENTS,
    RECEIPTS,
    NOTES,
    FORMS,
    CERTIFICATES,
    SCANNED,
    OTHER
  )
}

data class DocumentPageItem(
  val pageId: String,
  val pageNumber: Int,
  val imageUri: String,
  val rotationDegrees: Int = 0,
  val ocrText: String = "",
  val filterApplied: String = "Color Scan"
)

data class OcrAnalysisResult(
  val fullText: String,
  val lineCount: Int,
  val wordCount: Int,
  val detectedDates: List<String> = emptyList(),
  val detectedAmounts: List<String> = emptyList(),
  val detectedIds: List<String> = emptyList(),
  val summary: String = ""
)

data class DocumentItem(
  val id: String,
  val title: String,
  val pageCount: Int,
  val sizeBytes: Long,
  val format: DocumentFormat,
  val dateModified: String,
  val category: String,
  val colorSeed: Int = 0,
  val ocrText: String = "",
  val pages: List<DocumentPageItem> = emptyList(),
  val thumbnailUri: String? = null,
  val originalImageUri: String? = null,
  val detectedDates: List<String> = emptyList(),
  val detectedAmounts: List<String> = emptyList(),
  val detectedIds: List<String> = emptyList(),
  val resolution: String = "2480 × 3508 • 300 DPI",
  val width: Int = 2480,
  val height: Int = 3508,
  val dateModifiedMillis: Long = 0L
) {
  val formattedSize: String
    get() {
      val mb = sizeBytes / (1024.0 * 1024.0)
      return if (mb < 1.0) {
        val kb = sizeBytes / 1024
        "$kb KB"
      } else {
        "%.1f MB".format(mb)
      }
    }

  val hasOcr: Boolean
    get() = ocrText.isNotBlank()
}

data class AiActionItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val badge: String,
  val timestamp: String
)

data class VaultFolderItem(
  val id: String,
  val title: String,
  val count: Int,
  val lastUpdated: String,
  val iconName: String
)

enum class SortOption(val displayName: String) {
  NEWEST_FIRST("Newest first"),
  OLDEST_FIRST("Oldest first"),
  RECENTLY_MODIFIED("Recently modified"),
  LARGEST_FILE("Largest file"),
  SMALLEST_FILE("Smallest file"),
  NAME_AZ("Name A–Z"),
  NAME_ZA("Name Z–A")
}

enum class MediaTypeFilter(val label: String) {
  ALL("All Media"),
  PHOTOS("Photos only"),
  VIDEOS("Videos only")
}

enum class OrientationFilter(val label: String) {
  ALL("Any Orientation"),
  PORTRAIT("Portrait"),
  LANDSCAPE("Landscape")
}

enum class DateRangeFilter(val label: String) {
  ALL("All Time"),
  TODAY("Today"),
  YESTERDAY("Yesterday"),
  THIS_WEEK("This Week"),
  THIS_MONTH("This Month"),
  THIS_YEAR("This Year")
}

data class FilterState(
  val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
  val onlyFavorites: Boolean = false,
  val onlyEdited: Boolean = false,
  val onlyScreenshots: Boolean = false,
  val onlyDocuments: Boolean = false,
  val orientation: OrientationFilter = OrientationFilter.ALL,
  val fileType: String = "All",
  val dateRange: DateRangeFilter = DateRangeFilter.ALL,
  val selectedFolder: String = "All",
  val customAlbumId: String? = null
) {
  val isActive: Boolean
    get() = mediaType != MediaTypeFilter.ALL ||
      onlyFavorites ||
      onlyEdited ||
      onlyScreenshots ||
      onlyDocuments ||
      orientation != OrientationFilter.ALL ||
      fileType != "All" ||
      dateRange != DateRangeFilter.ALL ||
      selectedFolder != "All" ||
      customAlbumId != null

  val activeFilterCount: Int
    get() {
      var count = 0
      if (mediaType != MediaTypeFilter.ALL) count++
      if (onlyFavorites) count++
      if (onlyEdited) count++
      if (onlyScreenshots) count++
      if (onlyDocuments) count++
      if (orientation != OrientationFilter.ALL) count++
      if (fileType != "All") count++
      if (dateRange != DateRangeFilter.ALL) count++
      if (selectedFolder != "All") count++
      if (customAlbumId != null) count++
      return count
    }
}

data class EditRecord(
  val id: String,
  val originalMediaId: String,
  val originalUri: String,
  val editedUri: String,
  val editType: String,
  val editDate: String,
  val editDateMillis: Long,
  val previewColorSeed: Int = 0
)

data class SelectionSummary(
  val count: Int,
  val totalSizeBytes: Long,
  val photoCount: Int,
  val videoCount: Int,
  val dateSpan: String
) {
  val formattedSize: String
    get() {
      val mb = totalSizeBytes / (1024.0 * 1024.0)
      return if (mb >= 1000) {
        "%.2f GB".format(mb / 1024.0)
      } else {
        "%.1f MB".format(mb)
      }
    }
}

data class SmartGroupCategory(
  val id: String,
  val title: String,
  val subtitle: String,
  val count: Int,
  val iconName: String,
  val representativeUri: String? = null
)

data class GallerySettingsState(
  val themeMode: com.example.ui.theme.ThemeMode = com.example.ui.theme.ThemeMode.SYSTEM,
  val gridColumns: Int = 3,
  val sortOption: SortOption = SortOption.NEWEST_FIRST,
  val smoothAnimations: Boolean = true,
  val localOnlyAi: Boolean = true,
  val offlineAiEnabled: Boolean = true,
  val onlineAiEnabled: Boolean = false,
  val bioAuthEnabled: Boolean = true,
  val geminiApiKey: String = "",
  val askBeforeSendingToAi: Boolean = true,
  val highPerformanceMode: Boolean = true,
  val incrementalIndexingEnabled: Boolean = true,
  val thumbnailQuality: String = "Balanced",
  val autoCleanTempFiles: Boolean = true,
  val showUploadConfirmation: Boolean = true,
  val soundEffectsEnabled: Boolean = true,
  val hapticFeedbackEnabled: Boolean = true
)
