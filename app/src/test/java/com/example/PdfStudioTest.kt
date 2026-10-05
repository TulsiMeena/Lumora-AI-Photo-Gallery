package com.example

import com.example.model.pdf.PdfConfig
import com.example.model.pdf.PdfCreatorPage
import com.example.model.pdf.PdfDimensionUnit
import com.example.model.pdf.PdfHistoryItem
import com.example.model.pdf.PdfImageFit
import com.example.model.pdf.PdfOrientation
import com.example.model.pdf.PdfStandardSize
import com.example.model.pdf.PdfTargetSizeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfStudioTest {

  @Test
  fun testPdfConfigDefaults() {
    val config = PdfConfig()
    assertEquals(PdfStandardSize.A4, config.standardSize)
    assertEquals(PdfOrientation.PORTRAIT, config.orientation)
    assertEquals(PdfImageFit.FIT, config.imageFit)
    assertEquals(PdfDimensionUnit.MM, config.customUnit)
    assertEquals(595f, config.effectiveWidthPt, 0.1f)
    assertEquals(842f, config.effectiveHeightPt, 0.1f)
  }

  @Test
  fun testPageDimensionPoints() {
    val a4 = PdfStandardSize.A4
    assertEquals(595f, a4.widthPt, 0.1f)
    assertEquals(842f, a4.heightPt, 0.1f)

    val letter = PdfStandardSize.LETTER
    assertEquals(612f, letter.widthPt, 0.1f)
    assertEquals(792f, letter.heightPt, 0.1f)
  }

  @Test
  fun testTargetSizeOptions() {
    val mb1 = PdfTargetSizeOption.MB_1
    assertEquals(1024L * 1024L, mb1.bytes)
    assertTrue(mb1.displayName.contains("1 MB"))

    val kb500 = PdfTargetSizeOption.KB_500
    assertEquals(500L * 1024L, kb500.bytes)
  }

  @Test
  fun testPdfHistoryItemFormatting() {
    val item = PdfHistoryItem(
      id = "pdf_test_1",
      fileName = "Document.pdf",
      filePath = "/storage/emulated/0/Documents/Document.pdf",
      fileSizeBytes = 2_450_000L,
      pageCount = 8,
      createdAtMillis = System.currentTimeMillis(),
      pageSizeName = "A4",
      orientation = "Portrait",
      thumbnailUri = null,
      title = "Test PDF Document",
      author = "My Gallery AI",
      subject = "Invoices",
      keywords = "tax, receipts"
    )

    assertEquals("Document.pdf", item.fileName)
    assertEquals(8, item.pageCount)
    assertTrue(item.formattedSize.contains("MB"))
    assertNotNull(item.formattedDate)
  }

  @Test
  fun testPdfCreatorPageOperations() {
    val page1 = PdfCreatorPage(id = "1", sourceUri = "sample://1", title = "Page 1")
    val page2 = PdfCreatorPage(id = "2", sourceUri = "sample://2", title = "Page 2")
    val page3 = PdfCreatorPage(id = "3", sourceUri = "sample://3", title = "Page 3")

    val list = mutableListOf(page1, page2, page3)
    assertEquals(3, list.size)

    // Rotation test
    val rotated = page1.copy(rotationDegrees = (page1.rotationDegrees + 90) % 360)
    assertEquals(90, rotated.rotationDegrees)

    // Reorder test: move page 0 to index 2
    val moved = list.removeAt(0)
    list.add(2, moved)
    assertEquals("2", list[0].id)
    assertEquals("3", list[1].id)
    assertEquals("1", list[2].id)
  }
}
