package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.MediaType
import com.example.model.vault.VaultMediaItem
import com.example.model.vault.VaultSecurityConfig
import com.example.model.vault.VaultTab
import com.example.utils.vault.VaultCryptoManager
import com.example.utils.vault.VaultStorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultSecurityEngineTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun testIvGeneration() {
    val iv1 = VaultCryptoManager.generateIv()
    val iv2 = VaultCryptoManager.generateIv()
    assertEquals(12, iv1.size)
    assertEquals(12, iv2.size)
    assertFalse(iv1.contentEquals(iv2))
  }

  @Test
  fun testAesGcmEncryptionDecryption() {
    val secretMessage = "TopSecretConfidentialDocument123!@#".toByteArray(Charsets.UTF_8)
    val iv = VaultCryptoManager.generateIv()

    val encrypted = VaultCryptoManager.encryptBytes(secretMessage, iv)
    assertNotNull(encrypted)
    assertTrue(encrypted.isNotEmpty())
    assertFalse(secretMessage.contentEquals(encrypted))

    val decrypted = VaultCryptoManager.decryptBytes(encrypted, iv)
    assertNotNull(decrypted)
    assertEquals(String(secretMessage, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))
  }

  @Test
  fun testPbkdf2PinHashingAndVerification() {
    val pin = "8492"
    val salt = VaultCryptoManager.generateSalt()
    assertEquals(16, salt.size)

    val hash = VaultCryptoManager.hashPin(pin, salt)
    assertNotNull(hash)
    assertTrue(hash.isNotBlank())
    assertFalse(hash == pin)

    // Constant-time check verification
    val isCorrect = VaultCryptoManager.verifyPin("8492", hash, salt)
    assertTrue(isCorrect)

    val isWrong = VaultCryptoManager.verifyPin("1234", hash, salt)
    assertFalse(isWrong)
  }

  @Test
  fun testStreamingFileEncryptionAndVerification() {
    runBlocking {
      val testDir = File(context.cacheDir, "vault_test_stream").apply { mkdirs() }
      val sourceFile = File(testDir, "test_plaintext.txt").apply {
        writeText("Hello Private Vault! Hardware-backed offline AES-256 GCM encryption.")
      }
      val encryptedFile = File(testDir, "test_ciphertext.enc")
      val decryptedFile = File(testDir, "test_decrypted.txt")
      val iv = VaultCryptoManager.generateIv()

      val encryptSuccess = VaultCryptoManager.encryptFile(sourceFile, encryptedFile, iv)
      assertTrue(encryptSuccess)
      assertTrue(encryptedFile.exists())
      assertTrue(encryptedFile.length() > 0)

      val integrityVerified = VaultCryptoManager.verifyDecryptionIntegrity(encryptedFile, iv)
      assertTrue(integrityVerified)

      val decryptSuccess = VaultCryptoManager.decryptFile(encryptedFile, decryptedFile, iv)
      assertTrue(decryptSuccess)
      assertTrue(decryptedFile.exists())
      assertEquals(sourceFile.readText(), decryptedFile.readText())

      // Clean up
      sourceFile.delete()
      encryptedFile.delete()
      decryptedFile.delete()
    }
  }

  @Test
  fun testVaultStorageManagerSetupAndPreferences() {
    val initialConfig = VaultStorageManager.getSecurityConfig(context)
    assertNotNull(initialConfig)

    // Setup vault with 6-digit PIN and biometrics enabled
    val newConfig = VaultStorageManager.setupVault(context, "927451", enableBiometrics = true)
    assertTrue(newConfig.isSetupCompleted)
    assertTrue(newConfig.isBiometricEnabled)
    assertEquals(6, newConfig.pinLength)

    // Verify PIN through Storage Manager
    assertTrue(VaultStorageManager.verifyPin(context, "927451"))
    assertFalse(VaultStorageManager.verifyPin(context, "000000"))

    // Test failed attempts & lockout counter
    val failed1 = VaultStorageManager.recordFailedAttempt(context)
    assertEquals(1, failed1.failedAttempts)
    VaultStorageManager.recordFailedAttempt(context)
    VaultStorageManager.recordFailedAttempt(context)
    VaultStorageManager.recordFailedAttempt(context)
    val failed5 = VaultStorageManager.recordFailedAttempt(context)
    assertEquals(5, failed5.failedAttempts)
    assertTrue(failed5.lockoutUntilMillis > System.currentTimeMillis())

    // Reset attempts on successful authentication
    val reset = VaultStorageManager.resetFailedAttempts(context)
    assertEquals(0, reset.failedAttempts)
    assertEquals(0L, reset.lockoutUntilMillis)
  }

  @Test
  fun testVaultDirectoryCreationWithNoMedia() {
    val mediaDir = VaultStorageManager.getVaultMediaDir(context)
    assertTrue(mediaDir.exists())
    assertTrue(mediaDir.isDirectory)

    val noMediaFile = File(context.filesDir, "vault/.nomedia")
    assertTrue(noMediaFile.exists())
  }

  @Test
  fun testMemoryCacheWipe() {
    // Calling wipe should safely clear without exceptions
    VaultCryptoManager.wipeDecryptedMemoryCache()
  }

  @Test
  fun testVaultModels() {
    val item = VaultMediaItem(
      id = "item_123",
      originalMediaId = "orig_456",
      title = "Confidential Passport",
      originalFileName = "passport.jpg",
      encryptedFileName = "vault_abc123.enc",
      fileSizeBytes = 2 * 1024 * 1024L + 512 * 1024L, // 2.5 MB
      mediaType = MediaType.PHOTO,
      mimeType = "image/jpeg",
      ivBase64 = "123456789012"
    )

    assertEquals("2.5 MB", item.formattedSize)
    assertEquals(MediaType.PHOTO, item.mediaType)
    assertEquals("personal", item.albumId)
    assertFalse(item.isFavorite)

    assertEquals(4, VaultTab.entries.size)
    assertEquals("Photos", VaultTab.PHOTOS.label)
    assertEquals("Videos", VaultTab.VIDEOS.label)
    assertEquals("Albums", VaultTab.ALBUMS.label)
    assertEquals("Favorites", VaultTab.FAVORITES.label)
  }
}
