package com.example.utils.storage

import android.content.Context
import com.example.utils.cache.ThumbnailCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageBreakdown(
  val thumbnailCacheBytes: Long,
  val tempFilesBytes: Long,
  val editorTempBytes: Long,
  val pdfTempBytes: Long,
  val aiTempBytes: Long,
  val batchTempBytes: Long,
  val vaultStorageBytes: Long,
  val appDatabaseBytes: Long,
  val aiCacheRecordsCount: Int = 0
) {
  val totalSafeCleanableBytes: Long
    get() = thumbnailCacheBytes + tempFilesBytes + editorTempBytes + pdfTempBytes + aiTempBytes + batchTempBytes

  fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups.coerceIn(0, units.size - 1)])
  }
}

/**
 * Storage & Cache Manager providing non-destructive cleanup and accurate storage attribution.
 * NEVER deletes user photos, videos, vault originals, or exported user documents.
 */
object StorageCacheManager {

  /**
   * Calculates comprehensive storage breakdown.
   */
  suspend fun calculateStorage(context: Context, aiCacheCount: Int = 0): StorageBreakdown = withContext(Dispatchers.IO) {
    val thumbSize = ThumbnailCacheManager.getDiskCacheSizeBytes(context)
    val tempPipelineSize = SafeTempFileManager.getTempFilesSizeBytes(context)

    // Editor temp directory
    val editorTempDir = File(context.cacheDir, "editor_previews")
    val editorTempSize = getDirectorySize(editorTempDir)

    // PDF temp directory
    val pdfTempDir = File(context.cacheDir, "pdf_temp")
    val pdfTempSize = getDirectorySize(pdfTempDir)

    // AI temp directory
    val aiTempDir = File(context.cacheDir, "ai_temp")
    val aiTempSize = getDirectorySize(aiTempDir)

    // Batch work directory
    val batchTempDir = File(context.cacheDir, "batch_work")
    val batchTempSize = getDirectorySize(batchTempDir)

    // Vault encrypted files storage (in files/vault_media)
    val vaultDir = File(context.filesDir, "vault_media")
    val vaultSize = getDirectorySize(vaultDir)

    // App Database size
    val dbFile = context.getDatabasePath("my_gallery_ai.db")
    val dbWal = File(dbFile.parentFile, "my_gallery_ai.db-wal")
    val dbShm = File(dbFile.parentFile, "my_gallery_ai.db-shm")
    val dbSize = (if (dbFile.exists()) dbFile.length() else 0L) +
      (if (dbWal.exists()) dbWal.length() else 0L) +
      (if (dbShm.exists()) dbShm.length() else 0L)

    StorageBreakdown(
      thumbnailCacheBytes = thumbSize,
      tempFilesBytes = tempPipelineSize,
      editorTempBytes = editorTempSize,
      pdfTempBytes = pdfTempSize,
      aiTempBytes = aiTempSize,
      batchTempBytes = batchTempSize,
      vaultStorageBytes = vaultSize,
      appDatabaseBytes = dbSize,
      aiCacheRecordsCount = aiCacheCount
    )
  }

  /**
   * Safely clears thumbnail caches (Memory + Disk).
   */
  suspend fun clearThumbnailCache(context: Context) = withContext(Dispatchers.IO) {
    ThumbnailCacheManager.clearAll(context)
  }

  /**
   * Safely cleans all temporary processing files across all engines.
   * Original photos, videos, exported PDFs, and Vault data are 100% untouched.
   */
  suspend fun cleanAllTemporaryFiles(context: Context): Long = withContext(Dispatchers.IO) {
    var reclaimed = 0L

    reclaimed += SafeTempFileManager.cleanAllTempFiles(context)

    val dirsToClean = listOf(
      File(context.cacheDir, "editor_previews"),
      File(context.cacheDir, "pdf_temp"),
      File(context.cacheDir, "ai_temp"),
      File(context.cacheDir, "batch_work"),
      File(context.cacheDir, "share_compressed"),
      File(context.cacheDir, "share_resized"),
      File(context.cacheDir, "scanner_temp")
    )

    dirsToClean.forEach { dir ->
      if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
          val len = file.length()
          if (file.delete()) reclaimed += len
        }
      }
    }

    reclaimed
  }

  private fun getDirectorySize(dir: File): Long {
    if (!dir.exists()) return 0L
    var size = 0L
    dir.listFiles()?.forEach { file ->
      size += if (file.isDirectory) getDirectorySize(file) else file.length()
    }
    return size
  }
}
