package com.example.model.vault

import com.example.model.MediaType

/**
 * Represents an encrypted media item securely stored inside the Private Vault.
 */
data class VaultMediaItem(
  val id: String,
  val originalMediaId: String?,
  val title: String,
  val originalFileName: String,
  val encryptedFileName: String,
  val fileSizeBytes: Long,
  val mediaType: MediaType = MediaType.PHOTO,
  val mimeType: String = "image/jpeg",
  val width: Int = 0,
  val height: Int = 0,
  val durationMs: Long = 0L,
  val dateAddedMillis: Long = System.currentTimeMillis(),
  val albumId: String = "personal",
  val isFavorite: Boolean = false,
  val ivBase64: String, // 12-byte AES-GCM IV encoded in Base64
  val originalUriBeforeMove: String? = null
) {
  val formattedSize: String
    get() {
      val kb = fileSizeBytes / 1024.0
      val mb = kb / 1024.0
      return when {
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$fileSizeBytes B"
      }
    }
}

/**
 * Encrypted album inside the Private Vault.
 */
data class VaultAlbumItem(
  val id: String,
  val name: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val itemCount: Int = 0,
  val coverMediaId: String? = null
)

/**
 * Vault security settings and persistent state.
 */
data class VaultSecurityConfig(
  val isSetupCompleted: Boolean = false,
  val pinHash: String = "",
  val pinSalt: String = "",
  val pinLength: Int = 4,
  val isBiometricEnabled: Boolean = false,
  val autoLockTimeoutSeconds: Int = 60, // 1 minute default
  val isSecureScreenEnabled: Boolean = true, // FLAG_SECURE
  val askBeforeAiUpload: Boolean = true, // Mandatory AI protection default
  val exportWarningEnabled: Boolean = true,
  val failedAttempts: Int = 0,
  val lockoutUntilMillis: Long = 0L
)

/**
 * Active tab within the unlocked Private Vault.
 */
enum class VaultTab(val label: String) {
  PHOTOS("Photos"),
  VIDEOS("Videos"),
  ALBUMS("Albums"),
  FAVORITES("Favorites")
}

/**
 * Safe delete mode for gallery media.
 */
enum class SafeDeleteMode(val title: String, val description: String) {
  REMOVE_FROM_APP(
    title = "Remove from My Gallery",
    description = "Hides this item from My Gallery AI app. The original photo/video remains intact on your phone storage."
  ),
  DELETE_FROM_DEVICE(
    title = "Delete from Device",
    description = "Requests Android system to permanently remove this file from your device storage."
  )
}
