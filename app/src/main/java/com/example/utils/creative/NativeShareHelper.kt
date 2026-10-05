package com.example.utils.creative

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.compression.CompressionConfig
import com.example.model.compression.CompressionMode
import com.example.model.compression.TargetSizePreset
import com.example.model.creative.ShareMode
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode
import com.example.model.vault.VaultMediaItem
import com.example.utils.compression.ImageCompressionEngine
import com.example.utils.resize.ImageResizeEngine
import com.example.utils.vault.VaultCryptoManager
import com.example.utils.vault.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object NativeShareHelper {

  /**
   * Prepares and launches Android standard sharing Intent for single or multiple media items.
   */
  suspend fun shareMediaItems(
    context: Context,
    items: List<MediaItem>,
    mode: ShareMode = ShareMode.ORIGINAL
  ): Result<Intent> = withContext(Dispatchers.IO) {
    try {
      if (items.isEmpty()) return@withContext Result.failure(IllegalArgumentException("No items selected for sharing."))

      val shareUris = mutableListOf<Uri>()
      var overallMime = "*/*"

      val isSingle = items.size == 1

      for (item in items) {
        val resolvedUri = when (mode) {
          ShareMode.ORIGINAL -> {
            resolveFileProviderUri(context, item)
          }
          ShareMode.COMPRESSED -> {
            createCompressedShareCopy(context, item)
          }
          ShareMode.RESIZED -> {
            createResizedShareCopy(context, item)
          }
        }

        if (resolvedUri != null) {
          shareUris.add(resolvedUri)
          overallMime = item.mimeType
        }
      }

      if (shareUris.isEmpty()) {
        return@withContext Result.failure(Exception("Failed to prepare shareable files."))
      }

      val intent = if (isSingle) {
        Intent(Intent.ACTION_SEND).apply {
          type = overallMime
          putExtra(Intent.EXTRA_STREAM, shareUris.first())
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          type = if (items.all { it.mediaType == MediaType.PHOTO }) "image/*" else "*/*"
          putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareUris))
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }

      val chooser = Intent.createChooser(intent, "Share ${items.size} ${if (items.size == 1) "File" else "Files"}")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      context.startActivity(chooser)
      Result.success(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  /**
   * Securely decrypts and prepares a Vault item for explicit sharing.
   */
  suspend fun shareVaultMediaItem(
    context: Context,
    vaultItem: VaultMediaItem
  ): Result<Intent> = withContext(Dispatchers.IO) {
    try {
      val mediaDir = VaultStorageManager.getVaultMediaDir(context)
      val encFile = File(mediaDir, vaultItem.encryptedFileName)
      if (!encFile.exists()) return@withContext Result.failure(Exception("Encrypted file not found."))

      val shareCacheDir = File(context.cacheDir, "vault_share").apply { mkdirs() }
      val ext = if (vaultItem.mediaType == MediaType.VIDEO) ".mp4" else ".jpg"
      val tempShareFile = File(shareCacheDir, "shared_${vaultItem.id.take(8)}$ext")

      val iv = Base64.decode(vaultItem.ivBase64, Base64.NO_WRAP)
      val decryptSuccess = VaultCryptoManager.decryptFile(encFile, tempShareFile, iv)

      if (!decryptSuccess || !tempShareFile.exists()) {
        return@withContext Result.failure(Exception("Decryption failed. Vault media remains secure."))
      }

      val shareUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempShareFile
      )

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = vaultItem.mimeType
        putExtra(Intent.EXTRA_STREAM, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(intent, "Share from Private Vault")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)

      Result.success(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  private fun resolveFileProviderUri(context: Context, item: MediaItem): Uri? {
    return try {
      val uriStr = item.uri ?: return null
      val uri = Uri.parse(uriStr)

      if (uri.scheme == "content") {
        uri
      } else {
        val file = File(uri.path ?: uriStr)
        if (file.exists()) {
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
          uri
        }
      }
    } catch (_: Exception) {
      item.uri?.let { Uri.parse(it) }
    }
  }

  private suspend fun createCompressedShareCopy(context: Context, item: MediaItem): Uri? {
    return try {
      val uriStr = item.uri ?: return null
      val uri = Uri.parse(uriStr)

      val config = CompressionConfig(
        mode = CompressionMode.TARGET_SIZE,
        targetSizeBytes = 500 * 1024L,
        outputFormat = OutputFormat.JPEG
      )

      val result = ImageCompressionEngine.compressImage(context, uri, config, "share_${item.id.take(8)}")
      if (result.isSuccess && result.outputFilePath.isNotBlank()) {
        val targetFile = File(result.outputFilePath)
        if (targetFile.exists()) {
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
        } else {
          resolveFileProviderUri(context, item)
        }
      } else {
        resolveFileProviderUri(context, item)
      }
    } catch (_: Exception) {
      resolveFileProviderUri(context, item)
    }
  }

  private suspend fun createResizedShareCopy(context: Context, item: MediaItem): Uri? {
    return try {
      val uriStr = item.uri ?: return null
      val uri = Uri.parse(uriStr)

      val sourceBitmap = ImageResizeEngine.loadSafeBitmap(context, uri, 1920) ?: return resolveFileProviderUri(context, item)
      val resized = ImageResizeEngine.resizeBitmap(
        source = sourceBitmap,
        targetWidth = 1920,
        targetHeight = 1080,
        mode = ResizeMode.FIT
      )
      val savedFile = ImageResizeEngine.saveResizedImage(
        context = context,
        bitmap = resized,
        fileName = "share_hd_${item.id.take(8)}.jpg",
        format = OutputFormat.JPEG,
        quality = 88,
        dpi = 72
      )
      if (resized != sourceBitmap) resized.recycle()
      sourceBitmap.recycle()
      if (savedFile.exists()) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", savedFile)
      } else {
        resolveFileProviderUri(context, item)
      }
    } catch (_: Exception) {
      resolveFileProviderUri(context, item)
    }
  }
}
