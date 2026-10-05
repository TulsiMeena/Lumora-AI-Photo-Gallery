package com.example.data.ai

import com.example.model.DateRangeFilter
import com.example.model.MediaItem
import com.example.model.MediaTypeFilter
import com.example.model.OrientationFilter
import com.example.model.SortOption
import java.util.UUID

data class RecognizedFilterBadge(
  val label: String,
  val iconName: String,
  val filterType: String
)

data class ParsedAiQuery(
  val originalQuery: String,
  val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
  val dateRange: DateRangeFilter = DateRangeFilter.ALL,
  val specificMonth: Int? = null, // Calendar.SEPTEMBER, etc.
  val specificMonthName: String? = null,
  val specificYear: Int? = null,
  val onlyFavorites: Boolean = false,
  val onlyEdited: Boolean = false,
  val onlyScreenshots: Boolean = false,
  val onlyDocuments: Boolean = false,
  val onlyLargeFiles: Boolean = false,
  val onlySmallFiles: Boolean = false,
  val orientation: OrientationFilter = OrientationFilter.ALL,
  val targetAlbum: String? = null,
  val semanticKeywords: List<String> = emptyList(),
  val sortOption: SortOption? = null,
  val isCountQuery: Boolean = false,
  val isImageQuestion: Boolean = false
) {
  val hasActiveFilters: Boolean
    get() = mediaType != MediaTypeFilter.ALL ||
      dateRange != DateRangeFilter.ALL ||
      specificMonth != null ||
      specificYear != null ||
      onlyFavorites ||
      onlyEdited ||
      onlyScreenshots ||
      onlyDocuments ||
      onlyLargeFiles ||
      onlySmallFiles ||
      orientation != OrientationFilter.ALL ||
      targetAlbum != null ||
      semanticKeywords.isNotEmpty()
}

data class ImageAnalysisResult(
  val overview: String,
  val detectedObjects: List<String>,
  val extractedText: String,
  val dominantColor: String,
  val technicalDetails: Map<String, String>,
  val privacyNotice: String = "Processed 100% locally on-device. No data sent to external cloud."
)

data class AiChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val text: String,
  val isUser: Boolean,
  val timestamp: Long = System.currentTimeMillis(),
  val attachedMedia: MediaItem? = null,
  val recognizedBadges: List<RecognizedFilterBadge> = emptyList(),
  val matchingItems: List<MediaItem> = emptyList(),
  val isSearchQuery: Boolean = false,
  val isThinking: Boolean = false,
  val analysisResult: ImageAnalysisResult? = null,
  val isLocalOfflineResult: Boolean = true
)
