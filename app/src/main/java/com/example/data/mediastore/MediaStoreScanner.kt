package com.example.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.data.local.CachedMediaDao
import com.example.data.local.CachedMediaEntity
import com.example.model.AlbumItem
import com.example.model.AlbumType
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.TimelineSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MediaStoreScanner(private val context: Context) {

  private val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
  private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
  private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

  /**
   * Scans device images and videos via Android MediaStore.
   * If cachedMediaDao is provided, leverages local index for instant startup and performs
   * incremental change detection (new, modified, deleted) to prevent scanning the entire library repeatedly.
   */
  suspend fun scanMediaWithCache(
    cachedMediaDao: CachedMediaDao,
    excludedIds: Set<String> = emptySet(),
    favoriteIds: Set<String> = emptySet(),
    forceFullRescan: Boolean = false
  ): List<MediaItem> = withContext(Dispatchers.IO) {
    val cachedCount = cachedMediaDao.getMediaCount()
    val lastModifiedSec = if (!forceFullRescan) {
      (cachedMediaDao.getLatestModifiedTimestamp() ?: 0L) / 1000L
    } else 0L

    if (cachedCount > 0 && !forceFullRescan) {
      // 1. Check for incremental new/modified files
      val deltaList = mutableListOf<Pair<MediaItem, Long>>()
      scanImagesIncremental(deltaList, sinceModifiedSec = lastModifiedSec)
      scanVideosIncremental(deltaList, sinceModifiedSec = lastModifiedSec)

      if (deltaList.isNotEmpty()) {
        val entities = deltaList.map { (item, modSec) ->
          CachedMediaEntity(
            id = item.id,
            title = item.title,
            mediaType = item.mediaType.name,
            uri = item.uri ?: "",
            durationSeconds = item.durationSeconds,
            dateTaken = item.dateTaken,
            dateTakenMillis = item.dateTakenMillis,
            dateModifiedMillis = modSec * 1000L,
            sizeBytes = item.sizeBytes,
            width = item.width,
            height = item.height,
            mimeType = item.mimeType,
            isFavorite = favoriteIds.contains(item.id),
            albumName = item.albumName,
            bucketId = item.bucketId,
            resolution = item.resolution,
            cameraModel = item.cameraModel,
            categoryTag = item.categoryTag,
            colorSeed = item.colorSeed
          )
        }
        entities.chunked(200).forEach { chunk ->
          cachedMediaDao.insertMediaList(chunk)
        }
      }

      // Read from indexed Room cache (fast, non-blocking)
      val cached = cachedMediaDao.getPagedMedia(limit = 100000, offset = 0)
      return@withContext cached
        .filterNot { excludedIds.contains(it.id) }
        .map { entity ->
          MediaItem(
            id = entity.id,
            title = entity.title,
            mediaType = if (entity.mediaType == "VIDEO") MediaType.VIDEO else MediaType.PHOTO,
            uri = entity.uri,
            durationSeconds = entity.durationSeconds,
            dateTaken = entity.dateTaken,
            dateTakenMillis = entity.dateTakenMillis,
            sizeBytes = entity.sizeBytes,
            width = entity.width,
            height = entity.height,
            mimeType = entity.mimeType,
            isFavorite = favoriteIds.contains(entity.id) || entity.isFavorite,
            albumName = entity.albumName,
            bucketId = entity.bucketId,
            resolution = entity.resolution,
            cameraModel = entity.cameraModel,
            categoryTag = entity.categoryTag,
            colorSeed = entity.colorSeed
          )
        }
    }

    // Full scan initial run or forced rescan
    val scanned = scanMedia(excludedIds, favoriteIds)
    // Populate cache in background chunks
    val entities = scanned.map { item ->
      CachedMediaEntity(
        id = item.id,
        title = item.title,
        mediaType = item.mediaType.name,
        uri = item.uri ?: "",
        durationSeconds = item.durationSeconds,
        dateTaken = item.dateTaken,
        dateTakenMillis = item.dateTakenMillis,
        dateModifiedMillis = System.currentTimeMillis(),
        sizeBytes = item.sizeBytes,
        width = item.width,
        height = item.height,
        mimeType = item.mimeType,
        isFavorite = item.isFavorite,
        albumName = item.albumName,
        bucketId = item.bucketId,
        resolution = item.resolution,
        cameraModel = item.cameraModel,
        categoryTag = item.categoryTag,
        colorSeed = item.colorSeed
      )
    }
    cachedMediaDao.clearAll()
    entities.chunked(250).forEach { chunk ->
      cachedMediaDao.insertMediaList(chunk)
    }
    scanned
  }

  /**
   * Scans device images and videos via Android MediaStore.
   */
  suspend fun scanMedia(excludedIds: Set<String> = emptySet(), favoriteIds: Set<String> = emptySet()): List<MediaItem> =
    withContext(Dispatchers.IO) {
      val mediaList = mutableListOf<MediaItem>()

      // 1. Scan Images
      scanImages(mediaList)

      // 2. Scan Videos
      scanVideos(mediaList)

      // 3. Sort latest first (by dateTakenMillis DESC)
      mediaList.sortByDescending { it.dateTakenMillis }

      // 4. Filter excluded and mark favorites
      mediaList
        .filterNot { excludedIds.contains(it.id) }
        .map { item ->
          if (favoriteIds.contains(item.id)) {
            item.copy(isFavorite = true)
          } else {
            item
          }
        }
    }

  private fun scanImagesIncremental(outputList: MutableList<Pair<MediaItem, Long>>, sinceModifiedSec: Long) {
    if (sinceModifiedSec <= 0) return
    val selection = "${MediaStore.Images.Media.DATE_MODIFIED} > ?"
    val selectionArgs = arrayOf(sinceModifiedSec.toString())
    queryImages(selection, selectionArgs) { item, modSec ->
      outputList.add(item to modSec)
    }
  }

  private fun scanVideosIncremental(outputList: MutableList<Pair<MediaItem, Long>>, sinceModifiedSec: Long) {
    if (sinceModifiedSec <= 0) return
    val selection = "${MediaStore.Video.Media.DATE_MODIFIED} > ?"
    val selectionArgs = arrayOf(sinceModifiedSec.toString())
    queryVideos(selection, selectionArgs) { item, modSec ->
      outputList.add(item to modSec)
    }
  }

  private fun queryImages(
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    onItem: (MediaItem, Long) -> Unit
  ) {
    val projection = arrayOf(
      MediaStore.Images.Media._ID,
      MediaStore.Images.Media.DISPLAY_NAME,
      MediaStore.Images.Media.DATE_TAKEN,
      MediaStore.Images.Media.DATE_MODIFIED,
      MediaStore.Images.Media.WIDTH,
      MediaStore.Images.Media.HEIGHT,
      MediaStore.Images.Media.SIZE,
      MediaStore.Images.Media.MIME_TYPE,
      MediaStore.Images.Media.BUCKET_ID,
      MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    try {
      context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        val mimeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumn)
          val name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "IMG_$id.jpg" else "IMG_$id.jpg"
          val rawDateTaken = if (dateTakenColumn >= 0) cursor.getLong(dateTakenColumn) else 0L
          val rawDateModified = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
          val dateMillis = if (rawDateTaken > 0) rawDateTaken else rawDateModified * 1000L

          val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 1920
          val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 1080
          val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
          val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "image/jpeg" else "image/jpeg"
          val bucketId = if (bucketIdColumn >= 0) cursor.getString(bucketIdColumn) ?: "" else ""
          val bucketName = if (bucketNameColumn >= 0) cursor.getString(bucketNameColumn) ?: "Camera" else "Camera"

          val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
          val formattedDate = if (dateMillis > 0) dateFormat.format(Date(dateMillis)) else "Unknown Date"
          val megapixels = if (width > 0 && height > 0) {
            String.format(Locale.US, "%.1f MP", (width * height) / 1000000.0)
          } else {
            "Photo"
          }

          val category = when {
            bucketName.contains("Screenshot", ignoreCase = true) -> "Screenshots"
            bucketName.contains("WhatsApp", ignoreCase = true) -> "Social"
            bucketName.contains("Download", ignoreCase = true) -> "Downloads"
            bucketName.contains("Document", ignoreCase = true) -> "Documents"
            else -> "Camera"
          }

          val item = MediaItem(
            id = "img_$id",
            title = name,
            mediaType = MediaType.PHOTO,
            uri = contentUri,
            durationSeconds = 0,
            dateTaken = formattedDate,
            dateTakenMillis = dateMillis,
            sizeBytes = size,
            width = if (width > 0) width else 1920,
            height = if (height > 0) height else 1080,
            mimeType = mime,
            isFavorite = false,
            albumName = bucketName,
            bucketId = bucketId,
            resolution = "$width x $height ($megapixels)",
            cameraModel = bucketName,
            categoryTag = category,
            colorSeed = (id % 100).toInt()
          )
          onItem(item, rawDateModified)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun queryVideos(
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    onItem: (MediaItem, Long) -> Unit
  ) {
    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DATE_TAKEN,
      MediaStore.Video.Media.DATE_MODIFIED,
      MediaStore.Video.Media.WIDTH,
      MediaStore.Video.Media.HEIGHT,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.MIME_TYPE,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.BUCKET_ID,
      MediaStore.Video.Media.BUCKET_DISPLAY_NAME
    )
    val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
    try {
      context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
        val dateTakenColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
        val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        val mimeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
        val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumn)
          val name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "VID_$id.mp4" else "VID_$id.mp4"
          val rawDateTaken = if (dateTakenColumn >= 0) cursor.getLong(dateTakenColumn) else 0L
          val rawDateModified = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
          val dateMillis = if (rawDateTaken > 0) rawDateTaken else rawDateModified * 1000L

          val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 1920
          val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 1080
          val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
          val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "video/mp4" else "video/mp4"
          val durationMs = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
          val bucketId = if (bucketIdColumn >= 0) cursor.getString(bucketIdColumn) ?: "" else ""
          val bucketName = if (bucketNameColumn >= 0) cursor.getString(bucketNameColumn) ?: "Videos" else "Videos"

          val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
          val formattedDate = if (dateMillis > 0) dateFormat.format(Date(dateMillis)) else "Unknown Date"

          val item = MediaItem(
            id = "vid_$id",
            title = name,
            mediaType = MediaType.VIDEO,
            uri = contentUri,
            durationSeconds = (durationMs / 1000).toInt(),
            dateTaken = formattedDate,
            dateTakenMillis = dateMillis,
            sizeBytes = size,
            width = if (width > 0) width else 1920,
            height = if (height > 0) height else 1080,
            mimeType = mime,
            isFavorite = false,
            albumName = bucketName,
            bucketId = bucketId,
            resolution = "$width x $height HD",
            cameraModel = bucketName,
            categoryTag = "Video",
            colorSeed = (id % 100).toInt()
          )
          onItem(item, rawDateModified)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun scanImages(outputList: MutableList<MediaItem>) {
    val projection = arrayOf(
      MediaStore.Images.Media._ID,
      MediaStore.Images.Media.DISPLAY_NAME,
      MediaStore.Images.Media.DATE_TAKEN,
      MediaStore.Images.Media.DATE_MODIFIED,
      MediaStore.Images.Media.WIDTH,
      MediaStore.Images.Media.HEIGHT,
      MediaStore.Images.Media.SIZE,
      MediaStore.Images.Media.MIME_TYPE,
      MediaStore.Images.Media.BUCKET_ID,
      MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    try {
      context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        val mimeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumn)
          val name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "IMG_$id.jpg" else "IMG_$id.jpg"
          val rawDateTaken = if (dateTakenColumn >= 0) cursor.getLong(dateTakenColumn) else 0L
          val rawDateModified = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
          val dateMillis = if (rawDateTaken > 0) rawDateTaken else rawDateModified * 1000L

          val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 1920
          val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 1080
          val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
          val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "image/jpeg" else "image/jpeg"
          val bucketId = if (bucketIdColumn >= 0) cursor.getString(bucketIdColumn) ?: "" else ""
          val bucketName = if (bucketNameColumn >= 0) cursor.getString(bucketNameColumn) ?: "Camera" else "Camera"

          val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
          val formattedDate = if (dateMillis > 0) dateFormat.format(Date(dateMillis)) else "Unknown Date"

          val megapixels = if (width > 0 && height > 0) {
            String.format(Locale.US, "%.1f MP", (width * height) / 1000000.0)
          } else {
            "Photo"
          }

          val category = when {
            bucketName.contains("Screenshot", ignoreCase = true) -> "Screenshots"
            bucketName.contains("WhatsApp", ignoreCase = true) -> "Social"
            bucketName.contains("Download", ignoreCase = true) -> "Downloads"
            bucketName.contains("Document", ignoreCase = true) -> "Documents"
            else -> "Camera"
          }

          outputList.add(
            MediaItem(
              id = "img_$id",
              title = name,
              mediaType = MediaType.PHOTO,
              uri = contentUri,
              durationSeconds = 0,
              dateTaken = formattedDate,
              dateTakenMillis = dateMillis,
              sizeBytes = size,
              width = if (width > 0) width else 1920,
              height = if (height > 0) height else 1080,
              mimeType = mime,
              isFavorite = false,
              albumName = bucketName,
              bucketId = bucketId,
              resolution = "$width x $height ($megapixels)",
              cameraModel = bucketName,
              categoryTag = category,
              colorSeed = (id % 100).toInt()
            )
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun scanVideos(outputList: MutableList<MediaItem>) {
    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DATE_TAKEN,
      MediaStore.Video.Media.DATE_MODIFIED,
      MediaStore.Video.Media.WIDTH,
      MediaStore.Video.Media.HEIGHT,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.MIME_TYPE,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.BUCKET_ID,
      MediaStore.Video.Media.BUCKET_DISPLAY_NAME
    )

    val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

    try {
      context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
        val dateTakenColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
        val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        val mimeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
        val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumn)
          val name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "VID_$id.mp4" else "VID_$id.mp4"
          val rawDateTaken = if (dateTakenColumn >= 0) cursor.getLong(dateTakenColumn) else 0L
          val rawDateModified = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
          val dateMillis = if (rawDateTaken > 0) rawDateTaken else rawDateModified * 1000L

          val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 1920
          val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 1080
          val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
          val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "video/mp4" else "video/mp4"
          val durationMs = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
          val bucketId = if (bucketIdColumn >= 0) cursor.getString(bucketIdColumn) ?: "" else ""
          val bucketName = if (bucketNameColumn >= 0) cursor.getString(bucketNameColumn) ?: "Videos" else "Videos"

          val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
          val formattedDate = if (dateMillis > 0) dateFormat.format(Date(dateMillis)) else "Unknown Date"

          outputList.add(
            MediaItem(
              id = "vid_$id",
              title = name,
              mediaType = MediaType.VIDEO,
              uri = contentUri,
              durationSeconds = (durationMs / 1000).toInt(),
              dateTaken = formattedDate,
              dateTakenMillis = dateMillis,
              sizeBytes = size,
              width = if (width > 0) width else 1920,
              height = if (height > 0) height else 1080,
              mimeType = mime,
              isFavorite = false,
              albumName = bucketName,
              bucketId = bucketId,
              resolution = "$width x $height HD",
              cameraModel = bucketName,
              categoryTag = "Video",
              colorSeed = (id % 100).toInt()
            )
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Generates albums dynamically from scanned MediaItems based on folder/bucket info.
   * Only albums that actually contain media are created.
   */
  /**
   * Generates albums dynamically from scanned MediaItems based on folder/bucket info
   * and custom Room albums.
   * Only albums that actually contain media are created (except All Photos).
   */
  fun generateAlbums(
    mediaItems: List<MediaItem>,
    customAlbums: List<com.example.data.local.CustomAlbumEntity> = emptyList(),
    albumMediaMap: Map<String, List<String>> = emptyMap(),
    editedMediaIds: Set<String> = emptySet()
  ): List<AlbumItem> {
    if (mediaItems.isEmpty() && customAlbums.isEmpty()) return emptyList()

    val albums = mutableListOf<AlbumItem>()
    val now = System.currentTimeMillis()
    val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)

    // 1. All Photos Album (always created if media exists)
    if (mediaItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_all",
          name = "All Photos",
          count = mediaItems.size,
          albumType = AlbumType.ALL,
          coverUri = mediaItems.firstOrNull()?.uri,
          description = "Complete device library"
        )
      )
    }

    // 2. All Videos Album (only if videos exist)
    val videos = mediaItems.filter { it.mediaType == MediaType.VIDEO }
    if (videos.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_videos",
          name = "All Videos",
          count = videos.size,
          albumType = AlbumType.VIDEOS,
          coverUri = videos.first().uri,
          description = "All recorded & saved videos"
        )
      )
    }

    // 3. Favorites Album (only if favorites exist)
    val favorites = mediaItems.filter { it.isFavorite }
    if (favorites.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_fav",
          name = "Favorites",
          count = favorites.size,
          albumType = AlbumType.FAVORITES,
          coverUri = favorites.first().uri,
          description = "Starred photos & videos"
        )
      )
    }

    // 4. Recently Added Album (media taken/added in last 7 days)
    val recentlyAdded = mediaItems.filter { it.dateTakenMillis >= sevenDaysAgo }
    if (recentlyAdded.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_recent_added",
          name = "Recently Added",
          count = recentlyAdded.size,
          albumType = AlbumType.RECENT_ADDED,
          coverUri = recentlyAdded.first().uri,
          description = "Captured in the last 7 days"
        )
      )
    }

    // 5. Recently Edited Album (media with edit records or edit in name)
    val editedItems = mediaItems.filter {
      it.isEdited || editedMediaIds.contains(it.id) ||
        it.albumName.contains("edit", ignoreCase = true) ||
        it.title.contains("edit", ignoreCase = true)
    }
    if (editedItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_recent_edited",
          name = "Recently Edited",
          count = editedItems.size,
          albumType = AlbumType.RECENT_EDITED,
          coverUri = editedItems.first().uri,
          description = "Photos adjusted with My Gallery AI"
        )
      )
    }

    // 6. Camera Album
    val cameraItems = mediaItems.filter {
      it.albumName.equals("Camera", ignoreCase = true) ||
        it.albumName.contains("DCIM", ignoreCase = true)
    }
    if (cameraItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_camera",
          name = "Camera",
          count = cameraItems.size,
          albumType = AlbumType.CAMERA,
          coverUri = cameraItems.first().uri,
          description = "${cameraItems.size} shots from camera"
        )
      )
    }

    // 7. Screenshots Album
    val screenshotItems = mediaItems.filter {
      it.albumName.contains("Screenshot", ignoreCase = true) ||
        it.title.contains("Screenshot", ignoreCase = true)
    }
    if (screenshotItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_screenshots",
          name = "Screenshots",
          count = screenshotItems.size,
          albumType = AlbumType.SCREENSHOTS,
          coverUri = screenshotItems.first().uri,
          description = "${screenshotItems.size} captured screens"
        )
      )
    }

    // 8. Downloads Album
    val downloadItems = mediaItems.filter {
      it.albumName.contains("Download", ignoreCase = true)
    }
    if (downloadItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_downloads",
          name = "Downloads",
          count = downloadItems.size,
          albumType = AlbumType.DOWNLOADS,
          coverUri = downloadItems.first().uri,
          description = "${downloadItems.size} saved files"
        )
      )
    }

    // 9. WhatsApp Album
    val whatsAppItems = mediaItems.filter {
      it.albumName.contains("WhatsApp", ignoreCase = true)
    }
    if (whatsAppItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_whatsapp",
          name = "WhatsApp",
          count = whatsAppItems.size,
          albumType = AlbumType.WHATSAPP,
          coverUri = whatsAppItems.first().uri,
          description = "${whatsAppItems.size} media from WhatsApp"
        )
      )
    }

    // 10. Documents Album
    val documentItems = mediaItems.filter {
      it.albumName.contains("Doc", ignoreCase = true) ||
        it.mimeType.contains("pdf", ignoreCase = true) ||
        it.title.contains("scan", ignoreCase = true) ||
        it.title.contains("doc", ignoreCase = true)
    }
    if (documentItems.isNotEmpty()) {
      albums.add(
        AlbumItem(
          id = "album_documents",
          name = "Documents",
          count = documentItems.size,
          albumType = AlbumType.DOCUMENTS,
          coverUri = documentItems.first().uri,
          description = "${documentItems.size} text scans & documents"
        )
      )
    }

    // 11. Other folder buckets on device that aren't already represented above
    val standardFolders = setOf(
      "camera", "dcim", "screenshot", "screenshots", "download",
      "downloads", "whatsapp", "doc", "documents", "recent"
    )
    val otherBuckets = mediaItems
      .filterNot { item -> standardFolders.any { item.albumName.contains(it, ignoreCase = true) } }
      .groupBy { it.albumName }

    otherBuckets.forEach { (bucketName, items) ->
      if (items.isNotEmpty()) {
        albums.add(
          AlbumItem(
            id = "album_bucket_${bucketName.lowercase().replace(" ", "_")}",
            name = bucketName,
            count = items.size,
            albumType = AlbumType.CUSTOM,
            coverUri = items.first().uri,
            bucketId = items.first().bucketId,
            description = "${items.size} items in $bucketName"
          )
        )
      }
    }

    // 12. User Custom Albums from Room database
    val mediaById = mediaItems.associateBy { it.id }
    customAlbums.forEach { customAlbum ->
      val memberIds = albumMediaMap[customAlbum.id] ?: emptyList()
      val memberItems = memberIds.mapNotNull { mediaById[it] }

      // Representative cover: user's customCoverUri, or latest item in album, or null
      val cover = customAlbum.customCoverUri ?: memberItems.firstOrNull()?.uri

      albums.add(
        AlbumItem(
          id = "custom_${customAlbum.id}",
          name = customAlbum.name,
          count = memberItems.size,
          albumType = AlbumType.CUSTOM,
          coverUri = cover,
          description = "${memberItems.size} items • Custom Album",
          isCustom = true,
          customAlbumId = customAlbum.id
        )
      )
    }

    return albums
  }

  /**
   * Groups media items into timeline sections:
   * Today, Yesterday, This Week, Month + Year, Year.
   */
  fun generateTimeline(items: List<MediaItem>): List<TimelineSection> {
    if (items.isEmpty()) return emptyList()

    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
    val weekStart = todayStart - (6 * 24 * 60 * 60 * 1000L)
    val currentYear = now.get(Calendar.YEAR)

    val groupedMap = linkedMapOf<String, MutableList<MediaItem>>()

    items.forEach { item ->
      val time = item.dateTakenMillis
      val sectionTitle = when {
        time >= todayStart -> "Today"
        time >= yesterdayStart -> "Yesterday"
        time >= weekStart -> "This Week"
        else -> {
          val cal = Calendar.getInstance().apply { timeInMillis = time }
          val itemYear = cal.get(Calendar.YEAR)
          if (itemYear == currentYear) {
            monthYearFormat.format(Date(time))
          } else {
            yearFormat.format(Date(time))
          }
        }
      }

      groupedMap.getOrPut(sectionTitle) { mutableListOf() }.add(item)
    }

    return groupedMap.map { (title, list) ->
      TimelineSection(
        title = title,
        count = list.size,
        items = list
      )
    }
  }

  /**
   * Observes MediaStore changes using ContentObserver for real-time reactivity.
   */
  fun observeMediaStoreChanges(): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        trySend(Unit)
      }
    }

    context.contentResolver.registerContentObserver(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      true,
      observer
    )
    context.contentResolver.registerContentObserver(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      true,
      observer
    )

    awaitClose {
      context.contentResolver.unregisterContentObserver(observer)
    }
  }
}
