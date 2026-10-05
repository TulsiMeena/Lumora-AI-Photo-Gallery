package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.mediastore.MediaStoreScanner
import com.example.model.MediaItem
import com.example.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaStoreAndTimelineTest {

  private lateinit var context: Context
  private lateinit var scanner: MediaStoreScanner

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    scanner = MediaStoreScanner(context)
  }

  @Test
  fun testTimelineGeneration() {
    val now = System.currentTimeMillis()
    val day = 24 * 60 * 60 * 1000L

    val items = listOf(
      MediaItem(id = "1", title = "Today photo", dateTakenMillis = now - 1000),
      MediaItem(id = "2", title = "Yesterday photo", dateTakenMillis = now - day),
      MediaItem(id = "3", title = "Week photo", dateTakenMillis = now - (3 * day))
    )

    val timeline = scanner.generateTimeline(items)
    assertTrue("Timeline sections should not be empty", timeline.isNotEmpty())
    assertEquals(3, timeline.sumOf { it.count })
  }

  @Test
  fun testAlbumGenerationFromBuckets() {
    val items = listOf(
      MediaItem(id = "1", title = "Cam 1", albumName = "Camera", mediaType = MediaType.PHOTO),
      MediaItem(id = "2", title = "Vid 1", albumName = "Camera", mediaType = MediaType.VIDEO),
      MediaItem(id = "3", title = "Fav 1", albumName = "Screenshots", isFavorite = true)
    )

    val albums = scanner.generateAlbums(items)
    // Should have All Photos, Favorites, Videos, and folder buckets
    assertTrue(albums.any { it.name == "All Photos" })
    assertTrue(albums.any { it.name == "Favorites" })
    assertTrue(albums.any { it.name == "All Videos" || it.name == "Videos" })
    assertTrue(albums.any { it.name == "Camera" })
    assertTrue(albums.any { it.name == "Screenshots" })
  }

  @Test
  fun testSafeRemoveExclusion() {
    val excludedIds = setOf("img_1", "img_3")
    val allItems = listOf(
      MediaItem(id = "img_1", title = "Hide 1"),
      MediaItem(id = "img_2", title = "Visible"),
      MediaItem(id = "img_3", title = "Hide 2")
    )

    val visibleItems = allItems.filterNot { excludedIds.contains(it.id) }
    assertEquals(1, visibleItems.size)
    assertEquals("Visible", visibleItems.first().title)
  }
}
