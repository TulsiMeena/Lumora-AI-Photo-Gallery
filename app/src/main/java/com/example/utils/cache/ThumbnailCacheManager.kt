package com.example.utils.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

/**
 * Thumbnail size categories tailored for gallery UI layouts.
 */
enum class ThumbnailSize(val maxDimension: Int, val useRgb565: Boolean) {
  TINY(160, true),       // Timeline dense strip, micro previews
  GRID(400, true),       // 3-column / 4-column photo grid, albums
  PREVIEW(1280, false),  // Fullscreen viewer initial fast preview
  DETAILED(2048, false)  // Zoom preview before tiling
}

/**
 * High-performance, low-memory thumbnail caching system with dual-layer LRU (Memory + Disk).
 * Prevents OOMs by decoding directly to target sizes with RGB_565 config and safe downsampling.
 */
object ThumbnailCacheManager {

  private const val DISK_CACHE_DIR = "thumb_cache_v2"
  private const val MAX_DISK_CACHE_BYTES = 120L * 1024L * 1024L // 120 MB max disk cache

  // Memory Cache: 1/8th of available runtime heap
  private val maxMemoryBytes = Runtime.getRuntime().maxMemory()
  private val memoryCacheSize = (maxMemoryBytes / 8).toInt().coerceIn(16 * 1024 * 1024, 64 * 1024 * 1024)

  private val memoryCache = object : LruCache<String, Bitmap>(memoryCacheSize) {
    override fun sizeOf(key: String, bitmap: Bitmap): Int {
      return bitmap.byteCount
    }

    override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
      // Allow GC to reclaim bitmap memory
    }
  }

  /**
   * Generates a unique cache key based on uri, size, and optional lastModified timestamp.
   */
  fun getCacheKey(uriString: String, size: ThumbnailSize, lastModified: Long = 0L): String {
    val raw = "${uriString}_${size.name}_$lastModified"
    return try {
      val md = MessageDigest.getInstance("MD5")
      val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
      bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
      raw.hashCode().toString()
    }
  }

  /**
   * Retrieves a cached thumbnail from memory or disk, or safely decodes it from source.
   */
  suspend fun getThumbnail(
    context: Context,
    uri: Uri,
    size: ThumbnailSize = ThumbnailSize.GRID,
    lastModified: Long = 0L
  ): Bitmap? = withContext(Dispatchers.IO) {
    val key = getCacheKey(uri.toString(), size, lastModified)

    // 1. Check Memory Cache
    memoryCache.get(key)?.let { cachedBitmap ->
      if (!cachedBitmap.isRecycled) {
        return@withContext cachedBitmap
      } else {
        memoryCache.remove(key)
      }
    }

    // 2. Check Disk Cache
    val diskFile = getDiskCacheFile(context, key)
    if (diskFile.exists() && diskFile.length() > 0) {
      try {
        val options = BitmapFactory.Options().apply {
          inPreferredConfig = if (size.useRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        }
        val diskBitmap = BitmapFactory.decodeFile(diskFile.absolutePath, options)
        if (diskBitmap != null) {
          memoryCache.put(key, diskBitmap)
          return@withContext diskBitmap
        }
      } catch (e: Exception) {
        diskFile.delete()
      }
    }

    // 3. Decode from content URI with sample size downsampling
    val decodedBitmap = decodeSampledBitmapFromUri(context, uri, size.maxDimension, size.useRgb565)
      ?: return@withContext null

    // 4. Save to Memory Cache
    memoryCache.put(key, decodedBitmap)

    // 5. Persist to Disk Cache asynchronously (JPEG/WEBP)
    try {
      diskFile.parentFile?.mkdirs()
      FileOutputStream(diskFile).use { out ->
        val format = if (decodedBitmap.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        decodedBitmap.compress(format, 82, out)
      }
      trimDiskCacheIfNeeded(context)
    } catch (e: Exception) {
      // Ignore disk write failure
    }

    decodedBitmap
  }

  /**
   * Low-memory image decoder using inJustDecodeBounds, inSampleSize, and orientation correction.
   */
  private fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    reqDimension: Int,
    useRgb565: Boolean
  ): Bitmap? {
    return try {
      // Pass 1: Decode dimensions only
      var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return null
      val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      BitmapFactory.decodeStream(inputStream, null, boundsOptions)
      inputStream?.close()

      val rawWidth = boundsOptions.outWidth
      val rawHeight = boundsOptions.outHeight
      if (rawWidth <= 0 || rawHeight <= 0) return null

      // Calculate inSampleSize (power of 2)
      var sampleSize = 1
      while ((rawWidth / (sampleSize * 2)) >= reqDimension && (rawHeight / (sampleSize * 2)) >= reqDimension) {
        sampleSize *= 2
      }

      // Pass 2: Decode bitmap with downsampling
      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = if (useRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        inDither = true
      }

      inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
      inputStream.close()

      if (sampledBitmap == null) return null

      // Check Exif Orientation
      val exifOrientation = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
      } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
      }

      if (exifOrientation == ExifInterface.ORIENTATION_NORMAL) {
        sampledBitmap
      } else {
        val matrix = Matrix()
        when (exifOrientation) {
          ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
          ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
          ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
          ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
          ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        val rotated = Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
        if (rotated != sampledBitmap) {
          sampledBitmap.recycle()
        }
        rotated
      }
    } catch (e: Throwable) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Decodes a specific rectangular region of an image without loading the entire bitmap into memory.
   */
  suspend fun decodeRegion(
    context: Context,
    uri: Uri,
    rect: Rect,
    sampleSize: Int = 1
  ): Bitmap? = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val decoder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
          BitmapRegionDecoder.newInstance(stream)
        } else {
          @Suppress("DEPRECATION")
          BitmapRegionDecoder.newInstance(stream, false)
        } ?: return@use null

        val options = BitmapFactory.Options().apply {
          inSampleSize = sampleSize.coerceAtLeast(1)
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        decoder.decodeRegion(rect, options)
      }
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Invalidates cached items for a given URI.
   */
  fun invalidate(context: Context, uriString: String) {
    ThumbnailSize.values().forEach { size ->
      val key = getCacheKey(uriString, size, 0L)
      memoryCache.remove(key)
      val file = getDiskCacheFile(context, key)
      if (file.exists()) file.delete()
    }
  }

  /**
   * Clears memory and disk thumbnail caches completely.
   */
  fun clearAll(context: Context) {
    memoryCache.evictAll()
    try {
      val cacheDir = File(context.cacheDir, DISK_CACHE_DIR)
      if (cacheDir.exists()) {
        cacheDir.listFiles()?.forEach { it.delete() }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Returns current disk cache size in bytes.
   */
  fun getDiskCacheSizeBytes(context: Context): Long {
    val cacheDir = File(context.cacheDir, DISK_CACHE_DIR)
    if (!cacheDir.exists()) return 0L
    return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
  }

  private fun getDiskCacheFile(context: Context, key: String): File {
    val dir = File(context.cacheDir, DISK_CACHE_DIR)
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "$key.thumb")
  }

  private fun trimDiskCacheIfNeeded(context: Context) {
    val cacheDir = File(context.cacheDir, DISK_CACHE_DIR)
    if (!cacheDir.exists()) return
    val files = cacheDir.listFiles() ?: return
    var totalSize = files.sumOf { it.length() }
    if (totalSize > MAX_DISK_CACHE_BYTES) {
      val sortedFiles = files.sortedBy { it.lastModified() }
      for (file in sortedFiles) {
        val len = file.length()
        if (file.delete()) {
          totalSize -= len
          if (totalSize <= MAX_DISK_CACHE_BYTES * 0.75) break
        }
      }
    }
  }
}
