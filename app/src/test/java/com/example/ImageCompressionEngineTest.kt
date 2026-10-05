package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.compression.BatchCompressionItem
import com.example.model.compression.BatchItemStatus
import com.example.model.compression.CompressionConfig
import com.example.model.compression.CompressionMode
import com.example.model.compression.QualityLevel
import com.example.model.compression.SmartCompressPreset
import com.example.model.compression.TargetSizePreset
import com.example.model.compression.formatFileSize
import com.example.model.resize.OutputFormat
import com.example.utils.compression.ImageCompressionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageCompressionEngineTest {

  @Test
  fun testTargetSizeFormatting() {
    assertEquals("0 B", formatFileSize(0L))
    assertEquals("500 B", formatFileSize(500L))
    assertEquals("50.0 KB", formatFileSize(50 * 1024L))
    assertEquals("100.0 KB", formatFileSize(100 * 1024L))
    assertEquals("500.0 KB", formatFileSize(500 * 1024L))
    assertEquals("1.00 MB", formatFileSize(1024 * 1024L))
    assertEquals("2.50 MB", formatFileSize((2.5 * 1024 * 1024L).toLong()))
    assertEquals("10.00 MB", formatFileSize(10 * 1024 * 1024L))
  }

  @Test
  fun testTargetSizePresets() {
    assertEquals(50 * 1024L, TargetSizePreset.KB_50.bytes)
    assertEquals(100 * 1024L, TargetSizePreset.KB_100.bytes)
    assertEquals(200 * 1024L, TargetSizePreset.KB_200.bytes)
    assertEquals(300 * 1024L, TargetSizePreset.KB_300.bytes)
    assertEquals(500 * 1024L, TargetSizePreset.KB_500.bytes)
    assertEquals(1024 * 1024L, TargetSizePreset.MB_1.bytes)
    assertEquals(2 * 1024 * 1024L, TargetSizePreset.MB_2.bytes)
    assertEquals(5 * 1024 * 1024L, TargetSizePreset.MB_5.bytes)
    assertEquals(10 * 1024 * 1024L, TargetSizePreset.MB_10.bytes)
  }

  @Test
  fun testTransparentAlphaHandlingForJpeg() {
    // Create bitmap with transparency (ARGB_8888 with alpha 0)
    val transparentBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    transparentBitmap.setPixel(10, 10, Color.TRANSPARENT)
    assertTrue("Source bitmap must have alpha", transparentBitmap.hasAlpha())

    // Convert to JPEG with white background fill
    val prepared = ImageCompressionEngine.prepareBitmapForFormat(
      source = transparentBitmap,
      targetFormat = OutputFormat.JPEG,
      transparentBgColor = Color.WHITE
    )

    assertNotNull(prepared)
    assertEquals(100, prepared.width)
    assertEquals(100, prepared.height)
  }

  @Test
  fun testEstimateCompressedSize() = runBlocking {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(Color.BLUE)

    val estJpegHigh = ImageCompressionEngine.estimateCompressedSize(
      bitmap = bitmap,
      outputFormat = OutputFormat.JPEG,
      quality = 85
    )
    val estJpegLow = ImageCompressionEngine.estimateCompressedSize(
      bitmap = bitmap,
      outputFormat = OutputFormat.JPEG,
      quality = 25
    )

    assertTrue("JPEG size estimate should be greater than 0", estJpegHigh > 0)
    assertTrue("Lower quality should result in smaller or equal size", estJpegLow <= estJpegHigh)
  }

  @Test
  fun testBatchItemReductionCalculation() {
    val item = BatchCompressionItem(
      id = "1",
      uri = "content://media/1",
      title = "Photo.jpg",
      originalSize = 5 * 1024 * 1024L, // 5 MB
      originalWidth = 4000,
      originalHeight = 3000,
      originalFormat = "JPEG",
      status = BatchItemStatus.SUCCESS,
      outputSize = 500 * 1024L // 500 KB (90% reduction)
    )

    assertEquals(90, item.reductionPercent)
  }

  @Test
  fun testSmartCompressPresets() {
    assertTrue(SmartCompressPreset.BEST_QUALITY.defaultQuality > SmartCompressPreset.BALANCED.defaultQuality)
    assertTrue(SmartCompressPreset.BALANCED.defaultQuality > SmartCompressPreset.SMALL_FILE.defaultQuality)
    assertEquals(0.85f, SmartCompressPreset.BALANCED.scaleFactor, 0.01f)
    assertEquals(0.50f, SmartCompressPreset.FORM_UPLOAD.scaleFactor, 0.01f)
  }

  @Test
  fun testFormatsCodecSupport() {
    val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val jpegBaos = ByteArrayOutputStream()
    testBmp.compress(Bitmap.CompressFormat.JPEG, 80, jpegBaos)
    assertTrue("JPEG stream output should contain bytes", jpegBaos.size() > 0)

    val pngBaos = ByteArrayOutputStream()
    testBmp.compress(Bitmap.CompressFormat.PNG, 100, pngBaos)
    assertTrue("PNG stream output should contain bytes", pngBaos.size() > 0)
  }
}
