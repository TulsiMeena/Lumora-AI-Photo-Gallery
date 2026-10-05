package com.example.utils.advanced

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.MediaItem
import com.example.model.advanced.ExifPrivacyReport
import com.example.utils.storage.SafeTempFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * EXIF Privacy Shield: Inspects and scrubs sensitive GPS locations, device serials, and timestamps
 * before sharing media to messaging apps or social networks.
 */
object ExifPrivacyScrubber {

  suspend fun inspectExif(context: Context, item: MediaItem): ExifPrivacyReport = withContext(Dispatchers.IO) {
    val uriStr = item.uri ?: return@withContext ExifPrivacyReport(item.id, hasGps = false)
    try {
      val uri = Uri.parse(uriStr)
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        val latLong = FloatArray(2)
        val hasGps = exif.getLatLong(latLong)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

        ExifPrivacyReport(
          mediaId = item.id,
          hasGps = hasGps,
          latitude = if (hasGps) latLong[0].toDouble() else null,
          longitude = if (hasGps) latLong[1].toDouble() else null,
          deviceModel = model,
          dateTaken = date,
          software = software
        )
      } ?: ExifPrivacyReport(item.id, hasGps = false)
    } catch (e: Exception) {
      ExifPrivacyReport(item.id, hasGps = false)
    }
  }

  suspend fun createPrivacyCleanCopy(context: Context, item: MediaItem): Uri? = withContext(Dispatchers.IO) {
    val uriStr = item.uri ?: return@withContext null
    try {
      val uri = Uri.parse(uriStr)
      val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      } ?: return@withContext null

      // Save to clean temp file without any EXIF tags
      val cleanFile = SafeTempFileManager.createTempFile(context, prefix = "clean_share", extension = ".jpg")
      FileOutputStream(cleanFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
      }
      bitmap.recycle()

      FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        cleanFile
      )
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
