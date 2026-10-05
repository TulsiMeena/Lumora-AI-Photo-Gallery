package com.example.data

import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.SmartGroupCategory
import java.util.Calendar
import kotlin.math.abs

object SmartGroupingManager {

  /**
   * Evaluates media items and builds local smart grouping clusters:
   * - People (portrait aspect ratio & portrait shots)
   * - Objects / Scenes (scenic & travel shots)
   * - Events (time-clustered photos taken within close proximity)
   * - Documents (text, scans, recipes, receipts)
   * - Screenshots (screenshot tags, prefixes, and folders)
   * - Similar Images (close dimensions & timestamps)
   * - Duplicate Detection (identical dimensions and exact file size or timestamp)
   */
  fun computeSmartClusters(items: List<MediaItem>): List<SmartGroupCategory> {
    if (items.isEmpty()) return emptyList()

    val peopleItems = items.filter {
      it.categoryTag.equals("Portrait", true) ||
        it.title.contains("portrait", true) ||
        it.title.contains("person", true) ||
        it.title.contains("friend", true) ||
        (it.height > it.width && !it.categoryTag.equals("Document", true))
    }

    val objectItems = items.filter {
      it.categoryTag in listOf("Nature", "Urban", "Architecture", "Travel", "Landscape") ||
        it.title.contains("mountain", true) ||
        it.title.contains("sunset", true) ||
        it.title.contains("beach", true)
    }

    val documentItems = items.filter {
      it.categoryTag.equals("Document", true) ||
        it.mimeType.contains("pdf", true) ||
        it.title.contains("scan", true) ||
        it.title.contains("doc", true) ||
        it.title.contains("receipt", true) ||
        it.title.contains("invoice", true)
    }

    val screenshotItems = items.filter {
      it.albumName.contains("screenshot", true) ||
        it.title.contains("screenshot", true) ||
        it.categoryTag.equals("Screenshot", true)
    }

    // Event detection: groups of 2+ photos taken on the same day with specific clustering
    val eventsCount = items.groupBy { item ->
      val cal = Calendar.getInstance().apply {
        timeInMillis = if (item.dateTakenMillis > 0L) item.dateTakenMillis else 0L
      }
      "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }.count { it.value.size >= 2 }

    // Duplicate detection: same size & dimensions
    val duplicatesCount = items.groupBy { "${it.width}x${it.height}_${it.sizeBytes}" }
      .filter { it.key != "0x0_0" && it.value.size > 1 }
      .values.sumOf { it.size }

    // Similar images: taken within 60 seconds of each other
    var similarCount = 0
    val sorted = items.filter { it.dateTakenMillis > 0L }.sortedBy { it.dateTakenMillis }
    for (i in 0 until sorted.size - 1) {
      if (abs(sorted[i + 1].dateTakenMillis - sorted[i].dateTakenMillis) <= 60_000L) {
        similarCount++
      }
    }

    val list = mutableListOf<SmartGroupCategory>()

    if (peopleItems.isNotEmpty()) {
      list.add(
        SmartGroupCategory(
          id = "group_people",
          title = "People & Faces",
          subtitle = "${peopleItems.size} portrait & person memories",
          count = peopleItems.size,
          iconName = "Face",
          representativeUri = peopleItems.first().uri
        )
      )
    }

    if (objectItems.isNotEmpty()) {
      list.add(
        SmartGroupCategory(
          id = "group_objects",
          title = "Places & Scenes",
          subtitle = "${objectItems.size} landscapes, nature & urban captures",
          count = objectItems.size,
          iconName = "Landscape",
          representativeUri = objectItems.first().uri
        )
      )
    }

    if (eventsCount > 0) {
      list.add(
        SmartGroupCategory(
          id = "group_events",
          title = "Event Clusters",
          subtitle = "$eventsCount day & weekend activity outings",
          count = eventsCount,
          iconName = "Event",
          representativeUri = items.firstOrNull()?.uri
        )
      )
    }

    if (documentItems.isNotEmpty()) {
      list.add(
        SmartGroupCategory(
          id = "group_documents",
          title = "Scans & Documents",
          subtitle = "${documentItems.size} text papers, receipts & forms",
          count = documentItems.size,
          iconName = "Description",
          representativeUri = documentItems.first().uri
        )
      )
    }

    if (screenshotItems.isNotEmpty()) {
      list.add(
        SmartGroupCategory(
          id = "group_screenshots",
          title = "Screenshots",
          subtitle = "${screenshotItems.size} captured app screens",
          count = screenshotItems.size,
          iconName = "Smartphone",
          representativeUri = screenshotItems.first().uri
        )
      )
    }

    if (duplicatesCount > 0) {
      list.add(
        SmartGroupCategory(
          id = "group_duplicates",
          title = "Duplicate Cleaner",
          subtitle = "$duplicatesCount identical or duplicate files detected",
          count = duplicatesCount,
          iconName = "ContentCopy",
          representativeUri = items.firstOrNull()?.uri
        )
      )
    } else if (similarCount > 0) {
      list.add(
        SmartGroupCategory(
          id = "group_similar",
          title = "Similar Shots",
          subtitle = "$similarCount burst or consecutive photos",
          count = similarCount,
          iconName = "BurstMode",
          representativeUri = items.firstOrNull()?.uri
        )
      )
    }

    return list
  }
}
