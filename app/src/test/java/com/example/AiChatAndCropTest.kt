package com.example

import com.example.data.SampleDataProvider
import com.example.data.ai.LocalAiQueryParser
import com.example.data.ai.LocalAiSearchEngine
import com.example.model.editor.CropAspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatAndCropTest {

  @Test
  fun testRedColorTshirtQueryReturnsRedShirtPhotoTopMatch() {
    val query = "red colour tshirt bala photo do"
    val parsed = LocalAiQueryParser.parse(query)

    assertEquals("shirt", parsed.primarySubject)
    assertEquals("red", parsed.primaryColor)
    assertEquals("red shirt", parsed.exactCompoundPhrase)

    val searchResult = LocalAiSearchEngine.executeSearch(
      query = query,
      allMedia = SampleDataProvider.samplePhotos,
      albums = emptyList()
    )

    assertTrue("Search result should not be empty", searchResult.matchingItems.isNotEmpty())
    val topMatch = searchResult.matchingItems.first()
    assertEquals("m4", topMatch.id)
    assertEquals("Portrait in Red Shirt", topMatch.title)
    assertTrue("Explanation should highlight red shirt", searchResult.explanation.contains("Portrait in Red Shirt") || searchResult.explanation.contains("red shirt"))
  }

  @Test
  fun testHindiRedShirtQuery() {
    val query = "लाल shirt वाली photos दिखाओ"
    val searchResult = LocalAiSearchEngine.executeSearch(
      query = query,
      allMedia = SampleDataProvider.samplePhotos,
      albums = emptyList()
    )

    assertTrue(searchResult.matchingItems.isNotEmpty())
    assertEquals("Portrait in Red Shirt", searchResult.matchingItems.first().title)
  }

  @Test
  fun testCropAspectRatiosCalculation() {
    val squareBounds = CropAspectRatio.SQUARE_1_1.calculateCropBounds(1920f, 1080f)
    assertEquals(0f, squareBounds.cropTop, 0.01f)
    assertEquals(1f, squareBounds.cropBottom, 0.01f)
    assertTrue(squareBounds.cropLeft > 0f)
    assertTrue(squareBounds.cropRight < 1f)

    val portraitBounds = CropAspectRatio.RATIO_9_16.calculateCropBounds(1000f, 1000f)
    assertTrue(portraitBounds.cropLeft > 0f)
    assertTrue(portraitBounds.cropRight < 1f)
  }
}
