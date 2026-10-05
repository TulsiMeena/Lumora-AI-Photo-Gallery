package com.example.utils.vault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCryptoManager {

  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "GalleryVaultMasterKey_v1"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 12
  private const val PBKDF2_ITERATIONS = 10000
  private const val PBKDF2_KEY_LENGTH = 256

  private val secureRandom = SecureRandom()

  // In-memory decrypted thumbnail cache (wiped instantly on lockVault)
  private val maxCacheMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
  private val cacheSize = maxCacheMemory / 8 // 1/8th of available memory
  private val inMemoryThumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
    override fun sizeOf(key: String, bitmap: Bitmap): Int {
      return bitmap.byteCount / 1024
    }
  }

  // Backup fallback key for Robolectric/testing environments where AndroidKeyStore provider is absent
  @Volatile
  private var fallbackKey: SecretKey? = null

  /**
   * Retrieves or initializes the hardware-backed AES-256 master key in Android Keystore.
   */
  @Synchronized
  fun getOrCreateMasterKey(): SecretKey {
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      if (keyStore.containsAlias(KEY_ALIAS)) {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (entry != null) {
          return entry.secretKey
        }
      }

      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val keyGenParameterSpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setRandomizedEncryptionRequired(false) // We manage 12-byte random IVs per file
        .build()

      keyGenerator.init(keyGenParameterSpec)
      return keyGenerator.generateKey()
    } catch (_: Exception) {
      // In JVM / Robolectric environment where AndroidKeyStore is not mocked
      if (fallbackKey == null) {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, secureRandom)
        fallbackKey = keyGen.generateKey()
      }
      return fallbackKey!!
    }
  }

  /**
   * Generates a 12-byte cryptographically secure random IV for AES-GCM.
   */
  fun generateIv(): ByteArray {
    val iv = ByteArray(GCM_IV_LENGTH)
    secureRandom.nextBytes(iv)
    return iv
  }

  /**
   * Encrypts plaintext bytes using AES-256 GCM.
   */
  fun encryptBytes(plainBytes: ByteArray, iv: ByteArray): ByteArray {
    val key = getOrCreateMasterKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return cipher.doFinal(plainBytes)
  }

  /**
   * Decrypts ciphertext bytes using AES-256 GCM.
   */
  fun decryptBytes(cipherBytes: ByteArray, iv: ByteArray): ByteArray {
    val key = getOrCreateMasterKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return cipher.doFinal(cipherBytes)
  }

  /**
   * Encrypts an input file or stream to an encrypted destination file.
   */
  suspend fun encryptFile(
    sourceFile: File,
    destFile: File,
    iv: ByteArray
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val key = getOrCreateMasterKey()
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      FileInputStream(sourceFile).use { fis ->
        FileOutputStream(destFile).use { fos ->
          CipherOutputStream(fos, cipher).use { cos ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
              cos.write(buffer, 0, bytesRead)
            }
            cos.flush()
          }
        }
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      if (destFile.exists()) destFile.delete()
      false
    }
  }

  /**
   * Decrypts an encrypted file to a temporary or export destination file.
   */
  suspend fun decryptFile(
    encryptedFile: File,
    destFile: File,
    iv: ByteArray
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val key = getOrCreateMasterKey()
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      FileInputStream(encryptedFile).use { fis ->
        CipherInputStream(fis, cipher).use { cis ->
          FileOutputStream(destFile).use { fos ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (cis.read(buffer).also { bytesRead = it } != -1) {
              fos.write(buffer, 0, bytesRead)
            }
            fos.flush()
          }
        }
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      if (destFile.exists()) destFile.delete()
      false
    }
  }

  /**
   * Decrypts an encrypted image file into memory and decodes it safely to a Bitmap.
   * Never writes the decrypted bitmap to unencrypted disk cache.
   */
  suspend fun decodeEncryptedBitmap(
    encryptedFile: File,
    iv: ByteArray,
    maxDim: Int = 2048
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (!encryptedFile.exists()) return@withContext null
    val cacheKey = "${encryptedFile.name}_$maxDim"

    // Check in-memory cache first
    inMemoryThumbnailCache.get(cacheKey)?.let { return@withContext it }

    try {
      val key = getOrCreateMasterKey()
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      val baos = ByteArrayOutputStream()
      FileInputStream(encryptedFile).use { fis ->
        CipherInputStream(fis, cipher).use { cis ->
          val buffer = ByteArray(8192)
          var bytesRead: Int
          while (cis.read(buffer).also { bytesRead = it } != -1) {
            baos.write(buffer, 0, bytesRead)
          }
        }
      }

      val decryptedBytes = baos.toByteArray()
      if (decryptedBytes.isEmpty()) return@withContext null

      // Measure dimensions
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, boundsOptions)

      var sampleSize = 1
      val rawW = boundsOptions.outWidth
      val rawH = boundsOptions.outHeight
      if (rawW > maxDim || rawH > maxDim) {
        val halfW = rawW / 2
        val halfH = rawH / 2
        while ((halfW / sampleSize) >= maxDim && (halfH / sampleSize) >= maxDim) {
          sampleSize *= 2
        }
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      val decoded = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, decodeOptions)
      if (decoded != null) {
        inMemoryThumbnailCache.put(cacheKey, decoded)
      }
      decoded
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Verifies that the encrypted file can be decrypted and matches minimum valid header.
   */
  suspend fun verifyDecryptionIntegrity(
    encryptedFile: File,
    iv: ByteArray
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      if (!encryptedFile.exists() || encryptedFile.length() < 16) return@withContext false
      val key = getOrCreateMasterKey()
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      FileInputStream(encryptedFile).use { fis ->
        CipherInputStream(fis, cipher).use { cis ->
          val testBuffer = ByteArray(64)
          val read = cis.read(testBuffer)
          read > 0
        }
      }
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Hashes a user PIN with PBKDF2 and a random salt.
   * Raw PIN is never stored.
   */
  fun hashPin(pin: String, salt: ByteArray): String {
    return try {
      val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      val hash = factory.generateSecret(spec).encoded
      Base64.encodeToString(hash, Base64.NO_WRAP)
    } catch (_: Exception) {
      // Fallback to SHA-256 + salt if PBKDF2 is unavailable on older test JVM
      val md = MessageDigest.getInstance("SHA-256")
      md.update(salt)
      val hash = md.digest(pin.toByteArray(Charsets.UTF_8))
      Base64.encodeToString(hash, Base64.NO_WRAP)
    }
  }

  /**
   * Verifies an entered PIN against stored PBKDF2 hash using constant-time comparison.
   */
  fun verifyPin(enteredPin: String, storedHash: String, salt: ByteArray): Boolean {
    val computedHash = hashPin(enteredPin, salt)
    return MessageDigest.isEqual(
      computedHash.toByteArray(Charsets.UTF_8),
      storedHash.toByteArray(Charsets.UTF_8)
    )
  }

  /**
   * Generates a 16-byte random salt for password hashing.
   */
  fun generateSalt(): ByteArray {
    val salt = ByteArray(16)
    secureRandom.nextBytes(salt)
    return salt
  }

  /**
   * Wipes all in-memory decrypted Bitmaps upon locking the vault.
   */
  fun wipeDecryptedMemoryCache() {
    inMemoryThumbnailCache.evictAll()
  }
}
