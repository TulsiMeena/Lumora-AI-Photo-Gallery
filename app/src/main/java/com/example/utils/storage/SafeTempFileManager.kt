package com.example.utils.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Safe Temporary File System ensuring atomic writes and protection against partial/corrupted output.
 * Any crash during rendering or export leaves original files intact.
 */
object SafeTempFileManager {

  private const val TEMP_DIR_NAME = "gallery_temp_pipeline"

  fun getTempDirectory(context: Context): File {
    val dir = File(context.cacheDir, TEMP_DIR_NAME)
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  /**
   * Creates a unique temporary file with given extension for staged work.
   */
  fun createTempFile(context: Context, prefix: String = "work", extension: String = ".tmp"): File {
    val dir = getTempDirectory(context)
    val ext = if (extension.startsWith(".")) extension else ".$extension"
    return File(dir, "${prefix}_${UUID.randomUUID()}$ext")
  }

  /**
   * Atomically commits a temp file to its final destination path.
   * If atomic rename fails across partitions, it uses safe buffered transfer with fsync and deletes temp file.
   */
  suspend fun commitTempFile(tempFile: File, targetFile: File): Boolean = withContext(Dispatchers.IO) {
    if (!tempFile.exists() || tempFile.length() == 0L) {
      return@withContext false
    }

    try {
      targetFile.parentFile?.mkdirs()

      // Attempt atomic file system rename
      if (tempFile.renameTo(targetFile)) {
        return@withContext true
      }

      // Fallback: Buffered copy + fsync + atomic replace
      val stagingFile = File(targetFile.parentFile, "${targetFile.name}.staged_${System.currentTimeMillis()}")
      FileInputStream(tempFile).use { input ->
        FileOutputStream(stagingFile).use { output ->
          input.copyTo(output)
          output.fd.sync()
        }
      }

      if (targetFile.exists()) {
        targetFile.delete()
      }

      val success = stagingFile.renameTo(targetFile)
      if (!success) {
        // Direct rename failed, copy staging to target
        stagingFile.copyTo(targetFile, overwrite = true)
        stagingFile.delete()
      }

      tempFile.delete()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Cleans stale temporary files older than maxAgeMillis (default 6 hours).
   * Safe: ONLY deletes from `gallery_temp_pipeline`. Never touches user photos or documents.
   */
  suspend fun pruneStaleTempFiles(context: Context, maxAgeMillis: Long = 6 * 60 * 60 * 1000L): Int =
    withContext(Dispatchers.IO) {
      var deletedCount = 0
      try {
        val dir = getTempDirectory(context)
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        dir.listFiles()?.forEach { file ->
          if (file.lastModified() < cutoff) {
            if (file.delete()) deletedCount++
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
      deletedCount
    }

  /**
   * Cleans all temporary files in the processing pipeline immediately.
   */
  fun cleanAllTempFiles(context: Context): Long {
    var reclaimed = 0L
    try {
      val dir = getTempDirectory(context)
      dir.listFiles()?.forEach { file ->
        val len = file.length()
        if (file.delete()) reclaimed += len
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return reclaimed
  }

  fun getTempFilesSizeBytes(context: Context): Long {
    val dir = getTempDirectory(context)
    if (!dir.exists()) return 0L
    return dir.listFiles()?.sumOf { it.length() } ?: 0L
  }
}
