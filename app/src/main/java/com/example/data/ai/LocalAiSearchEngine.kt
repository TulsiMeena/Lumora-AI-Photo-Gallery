package com.example.data.ai

import com.example.model.AlbumItem
import com.example.model.DateRangeFilter
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MediaTypeFilter
import com.example.model.OrientationFilter
import com.example.model.SortOption
import java.util.Calendar
import java.util.Locale

data class AiSearchResult(
  val parsedQuery: ParsedAiQuery,
  val matchingItems: List<MediaItem>,
  val photosCount: Int,
  val videosCount: Int,
  val explanation: String,
  val badges: List<RecognizedFilterBadge>,
  val conversationalAnswer: String? = null
)

object LocalAiSearchEngine {

  fun executeSearch(
    query: String,
    allMedia: List<MediaItem>,
    albums: List<AlbumItem>,
    attachedMedia: MediaItem? = null
  ): AiSearchResult {
    val parsed = LocalAiQueryParser.parse(query, hasAttachedImage = attachedMedia != null)

    // Handle Image Question
    if (parsed.isImageQuestion && attachedMedia != null) {
      val analysis = LocalImageAnalyzer.analyzeImage(attachedMedia, query)
      return AiSearchResult(
        parsedQuery = parsed,
        matchingItems = listOf(attachedMedia),
        photosCount = if (attachedMedia.mediaType == MediaType.PHOTO) 1 else 0,
        videosCount = if (attachedMedia.mediaType == MediaType.VIDEO) 1 else 0,
        explanation = analysis.overview,
        badges = listOf(
          RecognizedFilterBadge("Local Vision Analysis", "eye", "ai"),
          RecognizedFilterBadge(attachedMedia.categoryTag, "category", "tag")
        ),
        conversationalAnswer = analysis.overview
      )
    }

    // Handle Count Questions (e.g. "इस album में कितनी photos हैं?", "Camera में कितनी photos हैं?")
    if (parsed.isCountQuery) {
      val albumMatch = albums.firstOrNull { album ->
        parsed.targetAlbum?.let { album.name.contains(it, ignoreCase = true) } == true ||
          query.contains(album.name, ignoreCase = true)
      }
      val countAnswer = if (albumMatch != null) {
        "Album '${albumMatch.name}' contains ${albumMatch.count} photos and videos. (Description: ${albumMatch.description})"
      } else {
        val totalPhotos = allMedia.count { it.mediaType == MediaType.PHOTO }
        val totalVideos = allMedia.count { it.mediaType == MediaType.VIDEO }
        "Your gallery has $totalPhotos photos and $totalVideos videos across ${albums.size} albums, totaling ${allMedia.size} items."
      }

      val badges = listOf(
        RecognizedFilterBadge("Gallery Analytics", "analytics", "stats"),
        RecognizedFilterBadge("Offline Query", "offline", "privacy")
      )

      return AiSearchResult(
        parsedQuery = parsed,
        matchingItems = emptyList(),
        photosCount = 0,
        videosCount = 0,
        explanation = countAnswer,
        badges = badges,
        conversationalAnswer = countAnswer
      )
    }

    // Prepare Date reference points
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)
    val thisWeek = now.get(Calendar.WEEK_OF_YEAR)
    val thisMonth = now.get(Calendar.MONTH)

    val yesterdayCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yestYear = yesterdayCal.get(Calendar.YEAR)
    val yestDay = yesterdayCal.get(Calendar.DAY_OF_YEAR)

    val badges = mutableListOf<RecognizedFilterBadge>()

    // Record filter badges
    if (parsed.specificMonthName != null) {
      badges.add(RecognizedFilterBadge("Month: ${parsed.specificMonthName}", "calendar", "date"))
    } else if (parsed.dateRange != DateRangeFilter.ALL) {
      badges.add(RecognizedFilterBadge(parsed.dateRange.label, "calendar", "date"))
    }
    if (parsed.specificYear != null) {
      badges.add(RecognizedFilterBadge("Year: ${parsed.specificYear}", "calendar", "date"))
    }
    if (parsed.mediaType != MediaTypeFilter.ALL) {
      badges.add(RecognizedFilterBadge(parsed.mediaType.label, "media", "type"))
    }
    if (parsed.onlyFavorites) {
      badges.add(RecognizedFilterBadge("Favorites ⭐", "star", "attribute"))
    }
    if (parsed.onlyEdited) {
      badges.add(RecognizedFilterBadge("Edited 🎨", "edit", "attribute"))
    }
    if (parsed.onlyScreenshots) {
      badges.add(RecognizedFilterBadge("Screenshots 📱", "phone", "attribute"))
    }
    if (parsed.onlyDocuments) {
      badges.add(RecognizedFilterBadge("Documents / Aadhaar 📄", "doc", "attribute"))
    }
    if (parsed.onlyLargeFiles) {
      badges.add(RecognizedFilterBadge("Large Files (>5MB) 💾", "storage", "size"))
    }
    if (parsed.onlySmallFiles) {
      badges.add(RecognizedFilterBadge("Small Files (<1MB) 💾", "storage", "size"))
    }
    if (parsed.targetAlbum != null) {
      badges.add(RecognizedFilterBadge("Album: ${parsed.targetAlbum}", "folder", "album"))
    }
    parsed.semanticKeywords.forEach { kw ->
      badges.add(RecognizedFilterBadge("Concept: $kw", "sparkles", "semantic"))
    }

    // Filter media items
    val filtered = allMedia.filter { item ->
      val itemCal = Calendar.getInstance().apply {
        timeInMillis = if (item.dateTakenMillis > 0L) item.dateTakenMillis else System.currentTimeMillis()
      }
      val itemYear = itemCal.get(Calendar.YEAR)
      val itemDay = itemCal.get(Calendar.DAY_OF_YEAR)
      val itemWeek = itemCal.get(Calendar.WEEK_OF_YEAR)
      val itemMonth = itemCal.get(Calendar.MONTH)

      // 1. Date Range
      when (parsed.dateRange) {
        DateRangeFilter.TODAY -> {
          if (itemYear != todayYear || itemDay != todayDay) return@filter false
        }
        DateRangeFilter.YESTERDAY -> {
          if (itemYear != yestYear || itemDay != yestDay) return@filter false
        }
        DateRangeFilter.THIS_WEEK -> {
          if (itemYear != todayYear || itemWeek != thisWeek) return@filter false
        }
        DateRangeFilter.THIS_MONTH -> {
          if (itemYear != todayYear || itemMonth != thisMonth) return@filter false
        }
        DateRangeFilter.THIS_YEAR -> {
          if (itemYear != todayYear) return@filter false
        }
        DateRangeFilter.ALL -> {}
      }

      // Specific Month (e.g. September)
      if (parsed.specificMonth != null) {
        val matchesMonthIndex = itemMonth == parsed.specificMonth
        val matchesMonthText = item.dateTaken.contains("Sep", ignoreCase = true) ||
          item.dateTaken.contains(parsed.specificMonthName ?: "", ignoreCase = true)
        if (!matchesMonthIndex && !matchesMonthText) return@filter false
      }

      // Specific Year
      if (parsed.specificYear != null) {
        val matchesYear = itemYear == parsed.specificYear || item.dateTaken.contains(parsed.specificYear.toString())
        if (!matchesYear) return@filter false
      }

      // 2. Media Type
      when (parsed.mediaType) {
        MediaTypeFilter.PHOTOS -> if (item.mediaType != MediaType.PHOTO) return@filter false
        MediaTypeFilter.VIDEOS -> if (item.mediaType != MediaType.VIDEO) return@filter false
        MediaTypeFilter.ALL -> {}
      }

      // 3. Favorites
      if (parsed.onlyFavorites && !item.isFavorite) return@filter false

      // 4. Edited
      if (parsed.onlyEdited) {
        val isEdited = item.isEdited ||
          item.title.contains("edit", ignoreCase = true) ||
          item.albumName.contains("edit", ignoreCase = true)
        if (!isEdited) return@filter false
      }

      // 5. Screenshots
      if (parsed.onlyScreenshots) {
        val isScr = item.albumName.contains("screenshot", ignoreCase = true) ||
          item.title.contains("screenshot", ignoreCase = true) ||
          item.categoryTag.equals("Screenshot", ignoreCase = true)
        if (!isScr) return@filter false
      }

      // 6. Documents
      if (parsed.onlyDocuments) {
        val isDoc = item.categoryTag.equals("Document", ignoreCase = true) ||
          item.albumName.contains("doc", ignoreCase = true) ||
          item.title.contains("aadhaar", ignoreCase = true) ||
          item.title.contains("doc", ignoreCase = true) ||
          item.title.contains("invoice", ignoreCase = true) ||
          item.title.contains("receipt", ignoreCase = true) ||
          item.title.contains("card", ignoreCase = true) ||
          item.semanticTags.any { it.contains("aadhaar", ignoreCase = true) || it.contains("document", ignoreCase = true) }
        if (!isDoc) return@filter false
      }

      // 7. Large / Small file size
      if (parsed.onlyLargeFiles && item.sizeBytes < 5 * 1024 * 1024L) return@filter false
      if (parsed.onlySmallFiles && item.sizeBytes >= 1 * 1024 * 1024L) return@filter false

      // 8. Orientation
      when (parsed.orientation) {
        OrientationFilter.PORTRAIT -> if (item.height <= item.width) return@filter false
        OrientationFilter.LANDSCAPE -> if (item.width <= item.height) return@filter false
        OrientationFilter.ALL -> {}
      }

      // 9. Target Album
      if (parsed.targetAlbum != null) {
        val matchesAlbum = item.albumName.contains(parsed.targetAlbum, ignoreCase = true)
        if (!matchesAlbum) return@filter false
      }

      // 10. Semantic Keywords (Objects, Colors, Scenes, Tags)
      if (parsed.semanticKeywords.isNotEmpty()) {
        val searchableText = buildString {
          append(item.title).append(" ")
          append(item.categoryTag).append(" ")
          append(item.albumName).append(" ")
          append(item.locationName).append(" ")
          append(item.dominantColor).append(" ")
          append(item.detectedText).append(" ")
          item.semanticTags.forEach { append(it).append(" ") }
          item.detectedObjects.forEach { append(it).append(" ") }
        }.lowercase(Locale.getDefault())

        val matchesAnyKeyword = parsed.semanticKeywords.any { kw ->
          searchableText.contains(kw)
        }
        if (!matchesAnyKeyword) return@filter false
      }

      true
    }

    // Sort results
    val sortOption = parsed.sortOption ?: SortOption.NEWEST_FIRST
    val sorted = when (sortOption) {
      SortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.dateTakenMillis }
      SortOption.OLDEST_FIRST -> filtered.sortedBy { it.dateTakenMillis }
      SortOption.LARGEST_FILE -> filtered.sortedByDescending { it.sizeBytes }
      SortOption.SMALLEST_FILE -> filtered.sortedBy { it.sizeBytes }
      SortOption.NAME_AZ -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
      SortOption.NAME_ZA -> filtered.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
      SortOption.RECENTLY_MODIFIED -> filtered.sortedByDescending { it.dateTakenMillis }
    }

    val photosCount = sorted.count { it.mediaType == MediaType.PHOTO }
    val videosCount = sorted.count { it.mediaType == MediaType.VIDEO }

    val explanation = if (sorted.isEmpty()) {
      "No matching photos found for '${parsed.originalQuery}'. You can try searching another date, searching albums, or using a broader query."
    } else {
      val breakdown = if (photosCount > 0 && videosCount > 0) {
        "$photosCount photos and $videosCount videos"
      } else if (videosCount > 0) {
        "$videosCount videos"
      } else {
        "$photosCount photos"
      }
      "Found $breakdown matching your query '${parsed.originalQuery}'."
    }

    return AiSearchResult(
      parsedQuery = parsed,
      matchingItems = sorted,
      photosCount = photosCount,
      videosCount = videosCount,
      explanation = explanation,
      badges = badges,
      conversationalAnswer = if (sorted.isNotEmpty()) explanation else null
    )
  }
}
