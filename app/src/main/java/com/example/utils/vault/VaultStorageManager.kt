package com.example.utils.vault

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Base64
import com.example.data.local.GalleryDatabase
import com.example.data.local.VaultMediaEntity
import com.example.model.MediaType
import com.example.model.vault.VaultMediaItem
import com.example.model.vault.VaultSecurityConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object VaultStorageManager {

  /**
   * Initializes private vault storage directories and creates .nomedia guard.
   */
  fun getVaultMediaDir(context: Context): File {
    val dir = File(context.filesDir, "vault/media").apply { mkdirs() }
    val noMedia = File(context.filesDir, "vault/.nomedia")
    if (!noMedia.exists()) {
      try { noMedia.createNewFile() } catch (_: Exception) {}
    }
    return dir
  }

  /**
   * Imports a photo or video into the encrypted Private Vault.
   * If isMoveAction is true, only deletes the original source after 100% verified decryption test.
   */
  suspend fun importMedia(
    context: Context,
    sourceUri: Uri,
    originalTitle: String,
    mediaType: MediaType,
    albumId: String = "personal",
    isMoveAction: Boolean = false
  ): Result<VaultMediaItem> = withContext(Dispatchers.IO) {
    try {
      val mediaDir = getVaultMediaDir(context)
      val fileId = UUID.randomUUID().toString()
      val encryptedFileName = "vault_${fileId.take(12)}.enc"
      val destEncryptedFile = File(mediaDir, encryptedFileName)
      val iv = VaultCryptoManager.generateIv()
      val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

      // 1. Copy source stream to temp working file for measurement & encryption
      val tempSource = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.tmp")
      var sourceSizeBytes = 0L
      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(tempSource).use { output ->
          val buffer = ByteArray(8192)
          var read: Int
          while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            sourceSizeBytes += read
          }
          output.flush()
        }
      } ?: return@withContext Result.failure(Exception("Cannot open input stream from source URI."))

      // 2. Measure dimensions & metadata
      var width = 0
      var height = 0
      var durationMs = 0L
      var mimeType = if (mediaType == MediaType.VIDEO) "video/mp4" else "image/jpeg"

      try {
        if (mediaType == MediaType.PHOTO) {
          val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
          BitmapFactory.decodeFile(tempSource.absolutePath, opts)
          width = opts.outWidth
          height = opts.outHeight
          opts.outMimeType?.let { mimeType = it }
        } else {
          val retriever = MediaMetadataRetriever()
          retriever.setDataSource(tempSource.absolutePath)
          width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
          height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
          durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
          retriever.release()
        }
      } catch (_: Exception) {}

      // 3. Encrypt file using AES-256 GCM
      val encryptSuccess = VaultCryptoManager.encryptFile(
        sourceFile = tempSource,
        destFile = destEncryptedFile,
        iv = iv
      )

      // Clean temp source file
      tempSource.delete()

      if (!encryptSuccess || !destEncryptedFile.exists()) {
        return@withContext Result.failure(Exception("Encryption failed. Source file preserved."))
      }

      // 4. CRITICAL VERIFICATION: Test decrypting the encrypted file before saving
      val isVerified = VaultCryptoManager.verifyDecryptionIntegrity(destEncryptedFile, iv)
      if (!isVerified) {
        destEncryptedFile.delete()
        return@withContext Result.failure(Exception("Integrity verification failed. Original file untouched."))
      }

      // 5. Save entity to database
      val entity = VaultMediaEntity(
        id = fileId,
        originalMediaId = sourceUri.lastPathSegment,
        title = originalTitle.ifBlank { "Private_${if (mediaType == MediaType.VIDEO) "Video" else "Photo"}" },
        originalFileName = originalTitle,
        encryptedFileName = encryptedFileName,
        fileSizeBytes = destEncryptedFile.length(),
        mediaType = mediaType.name,
        mimeType = mimeType,
        width = width,
        height = height,
        durationMs = durationMs,
        dateAddedMillis = System.currentTimeMillis(),
        albumId = albumId,
        isFavorite = false,
        ivBase64 = ivBase64,
        originalUriBeforeMove = if (isMoveAction) sourceUri.toString() else null
      )

      val db = GalleryDatabase.getDatabase(context)
      db.vaultDao().insertVaultMedia(entity)

      // 6. If move action was requested, delete source only after DB record & verified encryption
      if (isMoveAction) {
        try {
          context.contentResolver.delete(sourceUri, null, null)
        } catch (_: Exception) {
          // If MediaStore delete requires user confirmation on Android 11+,
          // we record originalUriBeforeMove so UI handles safe removal
        }
      }

      val vaultItem = VaultMediaItem(
        id = entity.id,
        originalMediaId = entity.originalMediaId,
        title = entity.title,
        originalFileName = entity.originalFileName,
        encryptedFileName = entity.encryptedFileName,
        fileSizeBytes = entity.fileSizeBytes,
        mediaType = mediaType,
        mimeType = entity.mimeType,
        width = entity.width,
        height = entity.height,
        durationMs = entity.durationMs,
        dateAddedMillis = entity.dateAddedMillis,
        albumId = entity.albumId,
        isFavorite = entity.isFavorite,
        ivBase64 = entity.ivBase64,
        originalUriBeforeMove = entity.originalUriBeforeMove
      )

      Result.success(vaultItem)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  /**
   * Exports an encrypted vault item back to the public gallery storage (Move Out of Vault).
   */
  suspend fun exportMedia(
    context: Context,
    item: VaultMediaItem,
    deleteFromVaultAfterExport: Boolean = true
  ): Result<Uri> = withContext(Dispatchers.IO) {
    try {
      val mediaDir = getVaultMediaDir(context)
      val encryptedFile = File(mediaDir, item.encryptedFileName)
      if (!encryptedFile.exists()) {
        return@withContext Result.failure(Exception("Encrypted media file not found."))
      }

      val iv = Base64.decode(item.ivBase64, Base64.NO_WRAP)
      val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val exportDir = File(picturesDir, "My Gallery AI/Exported").apply { mkdirs() }

      val ext = when {
        item.mimeType.contains("png") -> ".png"
        item.mimeType.contains("webp") -> ".webp"
        item.mimeType.contains("mp4") -> ".mp4"
        else -> ".jpg"
      }

      val cleanName = item.originalFileName.substringBeforeLast(".").ifBlank { "exported_media" }
      var targetFile = File(exportDir, "$cleanName$ext")
      var counter = 1
      while (targetFile.exists()) {
        targetFile = File(exportDir, "${cleanName}_$counter$ext")
        counter++
      }

      val decryptSuccess = VaultCryptoManager.decryptFile(
        encryptedFile = encryptedFile,
        destFile = targetFile,
        iv = iv
      )

      if (!decryptSuccess || !targetFile.exists() || targetFile.length() == 0L) {
        if (targetFile.exists()) targetFile.delete()
        return@withContext Result.failure(Exception("Decryption failed during export."))
      }

      // Notify Android MediaScanner so file appears in gallery
      MediaScannerConnection.scanFile(
        context,
        arrayOf(targetFile.absolutePath),
        arrayOf(item.mimeType),
        null
      )

      // Delete from vault if requested
      if (deleteFromVaultAfterExport) {
        deleteMediaFromVault(context, item)
      }

      Result.success(Uri.fromFile(targetFile))
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  /**
   * Permanently deletes media from Vault and wipes encrypted file.
   */
  suspend fun deleteMediaFromVault(context: Context, item: VaultMediaItem) = withContext(Dispatchers.IO) {
    val mediaDir = getVaultMediaDir(context)
    val encryptedFile = File(mediaDir, item.encryptedFileName)
    if (encryptedFile.exists()) {
      encryptedFile.delete()
    }
    val db = GalleryDatabase.getDatabase(context)
    db.vaultDao().deleteVaultMediaById(item.id)
  }

  /**
   * Prepares a temporary decrypted video file for secure playback.
   */
  suspend fun prepareDecryptedTempVideo(context: Context, item: VaultMediaItem): File? = withContext(Dispatchers.IO) {
    try {
      val mediaDir = getVaultMediaDir(context)
      val encryptedFile = File(mediaDir, item.encryptedFileName)
      if (!encryptedFile.exists()) return@withContext null

      val iv = Base64.decode(item.ivBase64, Base64.NO_WRAP)
      val tempVideoFile = File(context.cacheDir, "vault_playback_${item.id}.mp4")
      if (tempVideoFile.exists()) tempVideoFile.delete()

      val success = VaultCryptoManager.decryptFile(
        encryptedFile = encryptedFile,
        destFile = tempVideoFile,
        iv = iv
      )
      if (success && tempVideoFile.exists()) tempVideoFile else null
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Cleans up temporary video playback file.
   */
  fun cleanupTempVideo(file: File?) {
    try {
      if (file != null && file.exists()) {
        file.delete()
      }
    } catch (_: Exception) {}
  }

  // --- Security Config & Preferences ---

  fun getSecurityConfig(context: Context): VaultSecurityConfig {
    val prefs = context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)
    val isSetup = prefs.getBoolean("is_setup_completed", false)
    val hash = prefs.getString("pin_hash", "") ?: ""
    val salt = prefs.getString("pin_salt_base64", "") ?: ""
    val length = prefs.getInt("pin_length", 4)
    val bio = prefs.getBoolean("biometric_enabled", false)
    val autoLock = prefs.getInt("auto_lock_seconds", 60)
    val secureScreen = prefs.getBoolean("secure_screen_enabled", true)
    val askAi = prefs.getBoolean("ask_before_ai_upload", true)
    val exportWarn = prefs.getBoolean("export_warning_enabled", true)
    val failed = prefs.getInt("failed_attempts", 0)
    val lockout = prefs.getLong("lockout_until_millis", 0L)
    return VaultSecurityConfig(
      isSetupCompleted = isSetup,
      pinHash = hash,
      pinSalt = salt,
      pinLength = length,
      isBiometricEnabled = bio,
      autoLockTimeoutSeconds = autoLock,
      isSecureScreenEnabled = secureScreen,
      askBeforeAiUpload = askAi,
      exportWarningEnabled = exportWarn,
      failedAttempts = failed,
      lockoutUntilMillis = lockout
    )
  }

  fun saveSecurityConfig(context: Context, config: VaultSecurityConfig) {
    context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE).edit()
      .putBoolean("is_setup_completed", config.isSetupCompleted)
      .putString("pin_hash", config.pinHash)
      .putString("pin_salt_base64", config.pinSalt)
      .putInt("pin_length", config.pinLength)
      .putBoolean("biometric_enabled", config.isBiometricEnabled)
      .putInt("auto_lock_seconds", config.autoLockTimeoutSeconds)
      .putBoolean("secure_screen_enabled", config.isSecureScreenEnabled)
      .putBoolean("ask_before_ai_upload", config.askBeforeAiUpload)
      .putBoolean("export_warning_enabled", config.exportWarningEnabled)
      .putInt("failed_attempts", config.failedAttempts)
      .putLong("lockout_until_millis", config.lockoutUntilMillis)
      .apply()
  }

  fun setupVault(context: Context, pin: String, enableBiometrics: Boolean): VaultSecurityConfig {
    val saltBytes = VaultCryptoManager.generateSalt()
    val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    val pinHash = VaultCryptoManager.hashPin(pin, saltBytes)
    val config = VaultSecurityConfig(
      isSetupCompleted = true,
      pinHash = pinHash,
      pinSalt = saltBase64,
      pinLength = pin.length,
      isBiometricEnabled = enableBiometrics,
      autoLockTimeoutSeconds = 60,
      isSecureScreenEnabled = true,
      askBeforeAiUpload = true,
      exportWarningEnabled = true,
      failedAttempts = 0,
      lockoutUntilMillis = 0L
    )
    saveSecurityConfig(context, config)
    return config
  }

  fun verifyPin(context: Context, enteredPin: String): Boolean {
    val config = getSecurityConfig(context)
    if (!config.isSetupCompleted || config.pinHash.isBlank() || config.pinSalt.isBlank()) {
      return enteredPin == "1234"
    }
    val saltBytes = try {
      Base64.decode(config.pinSalt, Base64.NO_WRAP)
    } catch (_: Exception) {
      return false
    }
    return VaultCryptoManager.verifyPin(enteredPin, config.pinHash, saltBytes)
  }

  fun recordFailedAttempt(context: Context): VaultSecurityConfig {
    val config = getSecurityConfig(context)
    val newFailed = config.failedAttempts + 1
    val lockout = if (newFailed >= 5) {
      System.currentTimeMillis() + (30 * 1000L) // 30 seconds lockout
    } else {
      config.lockoutUntilMillis
    }
    val updated = config.copy(failedAttempts = newFailed, lockoutUntilMillis = lockout)
    saveSecurityConfig(context, updated)
    return updated
  }

  fun resetFailedAttempts(context: Context): VaultSecurityConfig {
    val config = getSecurityConfig(context)
    val updated = config.copy(failedAttempts = 0, lockoutUntilMillis = 0L)
    saveSecurityConfig(context, updated)
    return updated
  }
}
