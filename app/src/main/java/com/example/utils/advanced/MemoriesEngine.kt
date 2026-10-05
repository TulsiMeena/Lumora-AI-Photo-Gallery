package com.example.utils.advanced

import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.advanced.MemorySlide
import com.example.model.advanced.MemoryStory
import com.example.model.advanced.MemoryTheme
import java.util.Calendar
import java.util.UUID

/**
 * Intelligent Cinematic Memories & Recap Story Engine.
 * Curates "On This Day", "Golden Hours", and "Weekend Escape" stories.
 */
object MemoriesEngine {

  fun generateMemories(mediaItems: List<MediaItem>): List<MemoryStory> {
    val photos = mediaItems.filter { it.mediaType == MediaType.PHOTO }
    if (photos.isEmpty()) return emptyList()

    val stories = mutableListOf<MemoryStory>()
    val now = Calendar.getInstance()
    val currentDayOfYear = now.get(Calendar.DAY_OF_YEAR)
    val currentYear = now.get(Calendar.YEAR)

    // 1. "On This Day" Story (Photos taken near today's date in previous years)
    val onThisDayPhotos = photos.filter { item ->
      val cal = Calendar.getInstance().apply { timeInMillis = item.dateTakenMillis }
      val itemDay = cal.get(Calendar.DAY_OF_YEAR)
      val itemYear = cal.get(Calendar.YEAR)
      itemYear < currentYear && kotlin.math.abs(itemDay - currentDayOfYear) <= 14
    }

    if (onThisDayPhotos.isNotEmpty()) {
      val slides = onThisDayPhotos.take(8).map {
        MemorySlide(
          media = it,
          caption = "Relive this cherished moment",
          dateFormatted = it.dateTaken,
          locationLabel = it.albumName
        )
      }
      stories.add(
        MemoryStory(
          id = "story_on_this_day",
          theme = MemoryTheme.ON_THIS_DAY,
          title = "On This Day",
          subtitle = "Memories from years past",
          coverUri = slides.first().media.uri,
          slides = slides
        )
      )
    }

    // 2. "Golden Hours" Story (Sunset & Outdoors / Camera bucket)
    val goldenPhotos = photos.filter {
      it.albumName.contains("Camera", ignoreCase = true) || it.isFavorite
    }.take(8)

    if (goldenPhotos.isNotEmpty()) {
      val slides = goldenPhotos.map {
        MemorySlide(
          media = it,
          caption = "Golden memories in full glow",
          dateFormatted = it.dateTaken,
          locationLabel = it.albumName
        )
      }
      stories.add(
        MemoryStory(
          id = "story_golden",
          theme = MemoryTheme.SUMMER_VIBES,
          title = "Golden Hours",
          subtitle = "Best captures & highlights",
          coverUri = slides.first().media.uri,
          slides = slides
        )
      )
    }

    // 3. "Weekend Escape" Story
    val weekendPhotos = photos.takeLast(7).reversed()
    if (weekendPhotos.isNotEmpty()) {
      val slides = weekendPhotos.map {
        MemorySlide(
          media = it,
          caption = "Exploring the sights and city",
          dateFormatted = it.dateTaken,
          locationLabel = it.albumName
        )
      }
      stories.add(
        MemoryStory(
          id = "story_weekend",
          theme = MemoryTheme.WEEKEND_ESCAPE,
          title = "Weekend Escape",
          subtitle = "Recent adventures",
          coverUri = slides.first().media.uri,
          slides = slides
        )
      )
    }

    return stories
  }
}
