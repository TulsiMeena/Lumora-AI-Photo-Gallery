package com.example.data

import com.example.model.DateRangeFilter
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MediaTypeFilter
import com.example.model.OrientationFilter
import com.example.model.SortOption
import com.example.model.TimelineSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object GalleryFilterEngine {

  /**
   * Applies search query, multi-faceted filters, and sorting.
   * Filters combine correctly (e.g. September + Videos + Favorites).
   */
  fun filterAndSort(
    items: List<MediaItem>,
    searchQuery: String,
    filterState: FilterState,
    sortOption: SortOption,
    customAlbumMediaIds: Set<String>? = null,
    editedMediaIds: Set<String> = emptySet()
  ): List<MediaItem> {
    val trimmedQuery = searchQuery.trim().lowercase(Locale.getDefault())

    val filtered = items.filter { item ->
      // 1. Search Query
      if (trimmedQuery.isNotEmpty()) {
        val matchesSearch = item.title.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
          item.albumName.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
          item.dateTaken.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
          item.mimeType.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
          item.categoryTag.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
          item.locationName.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
          item.resolution.lowercase(Locale.getDefault()).contains(trimmedQuery)
        if (!matchesSearch) return@filter false
      }

      // 2. Media Type (Photos / Videos)
      when (filterState.mediaType) {
        MediaTypeFilter.PHOTOS -> if (item.mediaType != MediaType.PHOTO) return@filter false
        MediaTypeFilter.VIDEOS -> if (item.mediaType != MediaType.VIDEO) return@filter false
        MediaTypeFilter.ALL -> {}
      }

      // 3. Favorites
      if (filterState.onlyFavorites && !item.isFavorite) {
        return@filter false
      }

      // 4. Recently Edited
      if (filterState.onlyEdited) {
        val isItemEdited = item.isEdited ||
          editedMediaIds.contains(item.id) ||
          item.title.contains("edit", ignoreCase = true) ||
          item.albumName.contains("edit", ignoreCase = true)
        if (!isItemEdited) return@filter false
      }

      // 5. Screenshots
      if (filterState.onlyScreenshots) {
        val isScreenshot = item.albumName.contains("screenshot", ignoreCase = true) ||
          item.title.contains("screenshot", ignoreCase = true) ||
          item.categoryTag.equals("Screenshot", ignoreCase = true)
        if (!isScreenshot) return@filter false
      }

      // 6. Documents / Scans
      if (filterState.onlyDocuments) {
        val isDoc = item.albumName.contains("doc", ignoreCase = true) ||
          item.title.contains("doc", ignoreCase = true) ||
          item.title.contains("scan", ignoreCase = true) ||
          item.mimeType.contains("pdf", ignoreCase = true) ||
          item.categoryTag.equals("Document", ignoreCase = true)
        if (!isDoc) return@filter false
      }

      // 7. Orientation (Portrait / Landscape)
      when (filterState.orientation) {
        OrientationFilter.PORTRAIT -> if (item.height <= item.width) return@filter false
        OrientationFilter.LANDSCAPE -> if (item.width <= item.height) return@filter false
        OrientationFilter.ALL -> {}
      }

      // 8. File Type (e.g., JPEG, PNG, WEBP, MP4)
      if (filterState.fileType != "All") {
        val target = filterState.fileType.lowercase(Locale.getDefault())
        val matchesMime = item.mimeType.lowercase(Locale.getDefault()).contains(target)
        val matchesExt = item.title.lowercase(Locale.getDefault()).endsWith(target)
        if (!matchesMime && !matchesExt) return@filter false
      }

      // 9. Date Range
      if (filterState.dateRange != DateRangeFilter.ALL) {
        if (!isWithinDateRange(item.dateTakenMillis, filterState.dateRange)) {
          return@filter false
        }
      }

      // 10. Folder / Bucket Album
      if (filterState.selectedFolder != "All") {
        if (!item.albumName.equals(filterState.selectedFolder, ignoreCase = true)) {
          return@filter false
        }
      }

      // 11. Custom Album membership
      if (filterState.customAlbumId != null && customAlbumMediaIds != null) {
        if (!customAlbumMediaIds.contains(item.id)) {
          return@filter false
        }
      }

      true
    }

    // Sort the filtered results
    return when (sortOption) {
      SortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.dateTakenMillis }
      SortOption.OLDEST_FIRST -> filtered.sortedBy { it.dateTakenMillis }
      SortOption.RECENTLY_MODIFIED -> filtered.sortedByDescending { it.dateTakenMillis }
      SortOption.LARGEST_FILE -> filtered.sortedByDescending { it.sizeBytes }
      SortOption.SMALLEST_FILE -> filtered.sortedBy { it.sizeBytes }
      SortOption.NAME_AZ -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
      SortOption.NAME_ZA -> filtered.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
    }
  }

  private fun isWithinDateRange(timestamp: Long, range: DateRangeFilter): Boolean {
    if (timestamp <= 0L) return true
    val now = Calendar.getInstance()

    val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when (range) {
      DateRangeFilter.ALL -> true
      DateRangeFilter.TODAY -> {
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
          now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
      }
      DateRangeFilter.YESTERDAY -> {
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        yesterday.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
          yesterday.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
      }
      DateRangeFilter.THIS_WEEK -> {
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
          now.get(Calendar.WEEK_OF_YEAR) == itemCal.get(Calendar.WEEK_OF_YEAR)
      }
      DateRangeFilter.THIS_MONTH -> {
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
          now.get(Calendar.MONTH) == itemCal.get(Calendar.MONTH)
      }
      DateRangeFilter.THIS_YEAR -> {
        now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
      }
    }
  }

  /**
   * Generates chronological timeline sections with sticky-ready grouping
   * (Today, Yesterday, Week, Month, Year).
   */
  fun generateGroupedTimeline(items: List<MediaItem>): List<TimelineSection> {
    if (items.isEmpty()) return emptyList()

    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)

    val yesterdayCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yestYear = yesterdayCal.get(Calendar.YEAR)
    val yestDay = yesterdayCal.get(Calendar.DAY_OF_YEAR)

    val thisWeek = now.get(Calendar.WEEK_OF_YEAR)

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    val grouped = linkedMapOf<String, MutableList<MediaItem>>()

    items.forEach { item ->
      val cal = Calendar.getInstance().apply {
        timeInMillis = if (item.dateTakenMillis > 0L) item.dateTakenMillis else System.currentTimeMillis()
      }
      val itemYear = cal.get(Calendar.YEAR)
      val itemDay = cal.get(Calendar.DAY_OF_YEAR)
      val itemWeek = cal.get(Calendar.WEEK_OF_YEAR)

      val header = when {
        itemYear == todayYear && itemDay == todayDay -> "Today"
        itemYear == yestYear && itemDay == yestDay -> "Yesterday"
        itemYear == todayYear && itemWeek == thisWeek -> "This Week"
        itemYear == todayYear -> monthFormat.format(Date(cal.timeInMillis))
        else -> yearFormat.format(Date(cal.timeInMillis))
      }

      grouped.getOrPut(header) { mutableListOf() }.add(item)
    }

    return grouped.map { (title, list) ->
      TimelineSection(title = title, count = list.size, items = list)
    }
  }
}
