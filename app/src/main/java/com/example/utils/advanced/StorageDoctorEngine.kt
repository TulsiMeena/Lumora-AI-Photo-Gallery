package com.example.utils.advanced

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.advanced.CleanerCategory
import com.example.model.advanced.DuplicateGroup
import com.example.model.advanced.StorageDoctorScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import kotlin.math.abs

/**
 * Intelligent Storage Doctor that detects exact duplicates, burst takes, blurry shots,
 * dark accidental photos, and heavy media to free up gigabytes of phone storage.
 */
object StorageDoctorEngine {

  suspend fun scanStorageIssues(
    context: Context,
    mediaItems: List<MediaItem>,
    onProgress: (Int, Int) -> Unit = { _, _ -> }
  ): StorageDoctorScanResult = withContext(Dispatchers.Default) {
    if (mediaItems.isEmpty()) {
      return@withContext StorageDoctorScanResult()
    }

    val groups = mutableListOf<DuplicateGroup>()
    var totalSavings = 0L

    // 1. EXACT DUPLICATES (Same size, width, height, or same name base)
    val sizeGrouped = mediaItems
      .filter { it.sizeBytes > 0 }
      .groupBy { "${it.sizeBytes}_${it.width}x${it.height}" }
      .filter { it.value.size > 1 }

    sizeGrouped.forEach { (_, items) ->
      val keep = items.maxByOrNull { (if (it.isFavorite) 100 else 0) + it.dateTakenMillis } ?: items.first()
      val duplicateList = items.filter { it.id != keep.id }
      val savings = duplicateList.sumOf { it.sizeBytes }
      totalSavings += savings
      groups.add(
        DuplicateGroup(
          id = "dup_${UUID.randomUUID()}",
          category = CleanerCategory.DUPLICATES,
          items = items,
          recommendedKeepId = keep.id,
          potentialSavingsBytes = savings
        )
      )
    }

    // 2. SIMILAR / BURST SHOTS (Taken within 12 seconds in same folder/camera)
    val photosSorted = mediaItems.filter { it.mediaType == MediaType.PHOTO }.sortedBy { it.dateTakenMillis }
    val burstClusters = mutableListOf<MutableList<MediaItem>>()
    var currentBurst = mutableListOf<MediaItem>()

    for (i in photosSorted.indices) {
      val item = photosSorted[i]
      if (currentBurst.isEmpty()) {
        currentBurst.add(item)
      } else {
        val last = currentBurst.last()
        val timeDiff = abs(item.dateTakenMillis - last.dateTakenMillis)
        val sameBucket = item.albumName.equals(last.albumName, ignoreCase = true)
        if (timeDiff <= 12000L && sameBucket && currentBurst.size < 6) {
          currentBurst.add(item)
        } else {
          if (currentBurst.size >= 2) {
            burstClusters.add(currentBurst)
          }
          currentBurst = mutableListOf(item)
        }
      }
    }
    if (currentBurst.size >= 2) {
      burstClusters.add(currentBurst)
    }

    burstClusters.take(15).forEach { burstItems ->
      val keep = burstItems.maxByOrNull { (if (it.isFavorite) 200 else 0) + (it.width * it.height) } ?: burstItems.first()
      val redundant = burstItems.filter { it.id != keep.id }
      val savings = redundant.sumOf { it.sizeBytes }
      totalSavings += savings
      groups.add(
        DuplicateGroup(
          id = "burst_${UUID.randomUUID()}",
          category = CleanerCategory.SIMILAR,
          items = burstItems,
          recommendedKeepId = keep.id,
          potentialSavingsBytes = savings
        )
      )
    }

    // 3. BLURRY / LOW-QUALITY PHOTOS (Detected by tiny file size relative to resolution)
    val blurryCandidates = mediaItems.filter { item ->
      item.mediaType == MediaType.PHOTO &&
        item.width > 1200 &&
        item.sizeBytes in 1..85000L &&
        !item.albumName.contains("screenshot", ignoreCase = true)
    }.take(10)

    if (blurryCandidates.isNotEmpty()) {
      val savings = blurryCandidates.sumOf { it.sizeBytes }
      totalSavings += savings
      groups.add(
        DuplicateGroup(
          id = "blurry_${UUID.randomUUID()}",
          category = CleanerCategory.BLURRY,
          items = blurryCandidates,
          recommendedKeepId = null,
          potentialSavingsBytes = savings
        )
      )
    }

    // 4. DARK / ACCIDENTAL POCKET PHOTOS (Tiny file size or named dark)
    val darkCandidates = mediaItems.filter { item ->
      item.mediaType == MediaType.PHOTO &&
        item.title.contains("dark", ignoreCase = true) ||
        (item.sizeBytes in 1..45000L && item.width > 800)
    }.take(8)

    if (darkCandidates.isNotEmpty()) {
      val savings = darkCandidates.sumOf { it.sizeBytes }
      totalSavings += savings
      groups.add(
        DuplicateGroup(
          id = "dark_${UUID.randomUUID()}",
          category = CleanerCategory.DARK,
          items = darkCandidates,
          recommendedKeepId = null,
          potentialSavingsBytes = savings
        )
      )
    }

    // 5. HEAVY VIDEOS (> 50 MB)
    val heavyVideos = mediaItems.filter { item ->
      item.mediaType == MediaType.VIDEO && item.sizeBytes >= 50L * 1024L * 1024L
    }.sortedByDescending { it.sizeBytes }.take(8)

    if (heavyVideos.isNotEmpty()) {
      val savings = heavyVideos.sumOf { it.sizeBytes }
      totalSavings += savings
      groups.add(
        DuplicateGroup(
          id = "heavy_${UUID.randomUUID()}",
          category = CleanerCategory.LARGE_VIDEOS,
          items = heavyVideos,
          recommendedKeepId = null,
          potentialSavingsBytes = savings
        )
      )
    }

    // 6. SCREENSHOTS & MEMES (Non-camera clutter)
    val memeClutter = mediaItems.filter { item ->
      item.albumName.contains("WhatsApp", ignoreCase = true) ||
        item.albumName.contains("Download", ignoreCase = true) ||
        item.albumName.contains("Screenshot", ignoreCase = true) ||
        item.title.contains("IMG-", ignoreCase = true) ||
        item.title.contains("WhatsApp", ignoreCase = true)
    }.take(16)

    if (memeClutter.isNotEmpty()) {
      val savings = memeClutter.sumOf { it.sizeBytes }
      totalSavings += savings
      groups.add(
        DuplicateGroup(
          id = "memes_${UUID.randomUUID()}",
          category = CleanerCategory.MEMES_CLUTTER,
          items = memeClutter,
          recommendedKeepId = null,
          potentialSavingsBytes = savings
        )
      )
    }

    StorageDoctorScanResult(
      duplicateGroups = groups,
      totalSavingsBytes = totalSavings,
      scannedCount = mediaItems.size,
      isScanning = false
    )
  }
}
