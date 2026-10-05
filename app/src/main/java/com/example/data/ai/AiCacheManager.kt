package com.example.data.ai

import com.example.data.local.AiImageMetadataCacheDao
import com.example.data.local.AiImageMetadataCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local AI Result Cache Manager for offline search acceleration and metadata reuse.
 * Strictest Privacy Constraint: Vault content is NEVER stored in this cache or exposed to public search.
 */
class AiCacheManager(
  private val aiCacheDao: AiImageMetadataCacheDao
) {

  /**
   * Retrieves cached AI metadata for a media item if it hasn't been modified since caching.
   */
  suspend fun getCachedResult(mediaId: String, currentLastModified: Long): AiImageMetadataCacheEntity? =
    withContext(Dispatchers.IO) {
      if (mediaId.startsWith("vault_")) {
        // Vault privacy rule: strictly exclude vault media from AI cache
        return@withContext null
      }

      val cached = aiCacheDao.getMetadataForMedia(mediaId) ?: return@withContext null

      // Check if original media file was modified after AI cache generation
      if (currentLastModified > 0 && cached.timestamp < currentLastModified) {
        // Outdated cache
        return@withContext null
      }

      cached
    }

  /**
   * Caches extracted AI insights (OCR, description, detected objects, doc type).
   */
  suspend fun putCachedResult(
    mediaId: String,
    mediaUri: String,
    detectedText: String = "",
    aiDescription: String = "",
    detectedObjects: String = "",
    documentType: String = "",
    dominantAttributes: String = "",
    userQueries: String = ""
  ) = withContext(Dispatchers.IO) {
    if (mediaId.startsWith("vault_") || mediaUri.contains("vault_media")) {
      // Vault privacy isolation
      return@withContext
    }

    val entity = AiImageMetadataCacheEntity(
      mediaId = mediaId,
      mediaUri = mediaUri,
      detectedText = detectedText,
      aiDescription = aiDescription,
      detectedObjects = detectedObjects,
      documentType = documentType,
      dominantVisualAttributes = dominantAttributes,
      userQueries = userQueries,
      timestamp = System.currentTimeMillis()
    )
    aiCacheDao.insertOrUpdateMetadata(entity)
  }

  /**
   * Clears the entire AI metadata cache.
   */
  suspend fun clearAll() = withContext(Dispatchers.IO) {
    aiCacheDao.clearAllAiMetadata()
  }
}
