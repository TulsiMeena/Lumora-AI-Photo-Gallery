package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.resize.OutputFormat
import com.example.model.resize.PassportStandard
import com.example.model.resize.PhotoSheetConfig
import com.example.model.resize.PhotoSheetPaperSize
import com.example.model.resize.ResizeMode
import com.example.model.resize.ResizePresets
import com.example.model.resize.ResizeUnit
import com.example.utils.resize.ImageResizeEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageResizeStudioTest {

  @Test
  fun testPhysicalToPixelConversions() {
    // 3.5 cm @ 300 DPI -> (3.5 / 2.54) * 300 = 413 px
    val cmWidthPx = ImageResizeEngine.toPixels(3.5, ResizeUnit.CENTIMETERS, 300)
    assertEquals(413, cmWidthPx)

    // 4.5 cm @ 300 DPI -> (4.5 / 2.54) * 300 = 531 px
    val cmHeightPx = ImageResizeEngine.toPixels(4.5, ResizeUnit.CENTIMETERS, 300)
    assertEquals(531, cmHeightPx)

    // 2 inch @ 300 DPI -> 2 * 300 = 600 px
    val inPx = ImageResizeEngine.toPixels(2.0, ResizeUnit.INCHES, 300)
    assertEquals(600, inPx)

    // 35 mm @ 300 DPI -> (35 / 25.4) * 300 = 413 px
    val mmPx = ImageResizeEngine.toPixels(35.0, ResizeUnit.MILLIMETERS, 300)
    assertEquals(413, mmPx)

    // 1080 px @ 72 DPI -> 1080 px
    val directPx = ImageResizeEngine.toPixels(1080.0, ResizeUnit.PIXELS, 72)
    assertEquals(1080, directPx)
  }

  @Test
  fun testPixelToPhysicalConversions() {
    // 600 px @ 300 DPI -> 2.0 inches
    val inches = ImageResizeEngine.fromPixels(600, ResizeUnit.INCHES, 300)
    assertEquals(2.0, inches, 0.01)

    // 413 px @ 300 DPI -> 3.5 cm
    val cm = ImageResizeEngine.fromPixels(413, ResizeUnit.CENTIMETERS, 300)
    assertEquals(3.5, cm, 0.05)

    // 413 px @ 300 DPI -> 35.0 mm
    val mm = ImageResizeEngine.fromPixels(413, ResizeUnit.MILLIMETERS, 300)
    assertEquals(35.0, mm, 0.5)
  }

  @Test
  fun testPassportStandardsDefinitions() {
    val indiaPassport = ResizePresets.PASSPORT_STANDARDS.first { it.id == "passport_35x45" }
    assertEquals(35.0, indiaPassport.widthMm, 0.01)
    assertEquals(45.0, indiaPassport.heightMm, 0.01)
    val (pxW, pxH) = indiaPassport.getPixels(300)
    assertEquals(413, pxW)
    assertEquals(531, pxH)

    val usPassport = ResizePresets.PASSPORT_STANDARDS.first { it.id == "passport_2x2_in" }
    val (usW, usH) = usPassport.getPixels(300)
    assertEquals(600, usW)
    assertEquals(600, usH)
  }

  @Test
  fun testBitmapResizeModes() = runBlocking {
    val source = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)

    // Stretch
    val stretched = ImageResizeEngine.resizeBitmap(source, 300, 300, ResizeMode.STRETCH)
    assertEquals(300, stretched.width)
    assertEquals(300, stretched.height)

    // Fit (Inside 300x300 canvas)
    val fitted = ImageResizeEngine.resizeBitmap(source, 300, 300, ResizeMode.FIT, Color.WHITE)
    assertEquals(300, fitted.width)
    assertEquals(300, fitted.height)

    // Original Ratio
    val origRatio = ImageResizeEngine.resizeBitmap(source, 300, 300, ResizeMode.ORIGINAL_RATIO)
    assertEquals(150, origRatio.width)
    assertEquals(300, origRatio.height)

    // Fill
    val filled = ImageResizeEngine.resizeBitmap(source, 300, 300, ResizeMode.FILL)
    assertEquals(300, filled.width)
    assertEquals(300, filled.height)
  }

  @Test
  fun testFileSizeEstimation() {
    val jpegSize = ImageResizeEngine.estimateFileSize(1080, 1080, OutputFormat.JPEG, 90)
    assertTrue("JPEG size estimate should be reasonable", jpegSize in 50_000..800_000)

    val pngSize = ImageResizeEngine.estimateFileSize(1080, 1080, OutputFormat.PNG, 100)
    assertTrue("PNG size estimate should be larger for lossless", pngSize > jpegSize)
  }

  @Test
  fun testPassportPhotoSheetLayout() = runBlocking {
    val photo = Bitmap.createBitmap(413, 531, Bitmap.Config.ARGB_8888)
    val config = PhotoSheetConfig(
      paperSize = PhotoSheetPaperSize.FOUR_BY_SIX,
      photoWidthMm = 35.0,
      photoHeightMm = 45.0,
      copiesCount = 6,
      spacingMm = 3.0,
      marginMm = 8.0,
      targetDpi = 150
    )

    val sheet = ImageResizeEngine.generatePassportPhotoSheetBitmap(photo, config)
    assertNotNull(sheet)
    assertTrue(sheet.width > 0)
    assertTrue(sheet.height > 0)
  }
}
