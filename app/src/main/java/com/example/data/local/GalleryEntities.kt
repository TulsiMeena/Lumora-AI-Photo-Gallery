package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "cached_media_metadata",
  indices = [
    Index(value = ["dateTakenMillis"]),
    Index(value = ["albumName"]),
    Index(value = ["bucketId"]),
    Index(value = ["mediaType"]),
    Index(value = ["isFavorite"])
  ]
)
data class CachedMediaEntity(
  @PrimaryKey val id: String,
  val title: String,
  val mediaType: String,
  val uri: String,
  val durationSeconds: Int = 0,
  val dateTaken: String = "",
  val dateTakenMillis: Long = 0L,
  val dateModifiedMillis: Long = 0L,
  val sizeBytes: Long = 0L,
  val width: Int = 0,
  val height: Int = 0,
  val mimeType: String = "",
  val isFavorite: Boolean = false,
  val albumName: String = "",
  val bucketId: String = "",
  val resolution: String = "",
  val cameraModel: String = "",
  val categoryTag: String = "",
  val colorSeed: Int = 0
)

@Entity(
  tableName = "favorites",
  indices = [Index(value = ["dateFavorited"])]
)
data class FavoriteEntity(
  @PrimaryKey val mediaId: String,
  val mediaUri: String,
  val dateFavorited: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "excluded_media",
  indices = [Index(value = ["dateExcluded"])]
)
data class ExcludedMediaEntity(
  @PrimaryKey val mediaId: String,
  val mediaUri: String,
  val dateExcluded: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(
  @PrimaryKey val id: String,
  val name: String,
  val customCoverUri: String? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "album_media",
  primaryKeys = ["albumId", "mediaId"],
  indices = [Index(value = ["albumId"]), Index(value = ["mediaId"]), Index(value = ["addedAt"])]
)
data class AlbumMediaCrossEntity(
  val albumId: String,
  val mediaId: String,
  val mediaUri: String? = null,
  val addedAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "edit_history",
  indices = [Index(value = ["originalMediaId"]), Index(value = ["editDateMillis"])]
)
data class EditHistoryEntity(
  @PrimaryKey val id: String,
  val originalMediaId: String,
  val originalUri: String,
  val editedUri: String,
  val editType: String = "Color Filter",
  val editDateMillis: Long = System.currentTimeMillis(),
  val previewColorSeed: Int = 0
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
  @PrimaryKey val key: String,
  val value: String
)

@Entity(tableName = "smart_index")
data class SmartIndexEntity(
  @PrimaryKey val mediaId: String,
  val hasPeople: Boolean = false,
  val hasDocument: Boolean = false,
  val hasScreenshot: Boolean = false,
  val detectedObjects: String = "",
  val detectedEvent: String = "",
  val perceptualHash: String = ""
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
  @PrimaryKey val id: String,
  val query: String,
  val timestamp: Long = System.currentTimeMillis(),
  val resultCount: Int = 0
)

@Entity(
  tableName = "scanned_documents",
  indices = [Index(value = ["dateModifiedMillis"]), Index(value = ["category"])]
)
data class DocumentEntity(
  @PrimaryKey val id: String,
  val title: String,
  val category: String,
  val pageCount: Int = 1,
  val sizeBytes: Long = 0L,
  val format: String = "SCAN",
  val dateModified: String = "",
  val dateModifiedMillis: Long = System.currentTimeMillis(),
  val colorSeed: Int = 0,
  val ocrText: String = "",
  val detectedDatesJson: String = "",
  val detectedAmountsJson: String = "",
  val detectedIdsJson: String = "",
  val resolution: String = "2480 × 3508 • 300 DPI",
  val width: Int = 2480,
  val height: Int = 3508,
  val originalImageUri: String? = null
)

@Entity(
  tableName = "document_pages",
  primaryKeys = ["documentId", "pageId"],
  indices = [Index(value = ["documentId"]), Index(value = ["pageNumber"])]
)
data class DocumentPageEntity(
  val documentId: String,
  val pageId: String,
  val pageNumber: Int,
  val imageUri: String,
  val rotationDegrees: Int = 0,
  val ocrText: String = "",
  val filterApplied: String = "Color Scan"
)

@Entity(
  tableName = "pdf_history",
  indices = [Index(value = ["createdAtMillis"])]
)
data class PdfHistoryEntity(
  @PrimaryKey val id: String,
  val fileName: String,
  val filePath: String,
  val fileSizeBytes: Long,
  val pageCount: Int,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val pageSizeName: String = "A4",
  val orientation: String = "Portrait",
  val thumbnailUri: String? = null,
  val title: String = "",
  val author: String = "",
  val subject: String = "",
  val keywords: String = "",
  val targetSizeBytes: Long = 0L,
  val qualityPreset: String = "Balanced"
)

@Entity(
  tableName = "editing_projects",
  indices = [Index(value = ["originalMediaId"]), Index(value = ["lastModifiedMillis"])]
)
data class EditingProjectEntity(
  @PrimaryKey val id: String,
  val originalMediaId: String,
  val originalUri: String,
  val title: String,
  val thumbnailPath: String? = null,
  val lastModifiedMillis: Long = System.currentTimeMillis(),
  val stateJson: String
)

@Entity(
  tableName = "ai_image_chat_sessions",
  indices = [Index(value = ["category"]), Index(value = ["lastUpdatedMillis"])]
)
data class AiImageChatSessionEntity(
  @PrimaryKey val sessionId: String,
  val mediaId: String = "",
  val mediaUri: String,
  val mediaTitle: String = "Photo",
  val category: String = "Image Chat",
  val title: String = "Image Conversation",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val lastUpdatedMillis: Long = System.currentTimeMillis(),
  val lastPreview: String = "",
  val thumbnailUri: String? = null
)

@Entity(
  tableName = "ai_image_chat_messages",
  indices = [Index(value = ["sessionId"]), Index(value = ["timestamp"])]
)
data class AiImageChatMessageEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val isUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val taskType: String = "ASK_ANYTHING",
  val structuredJson: String? = null,
  val cropRectJson: String? = null
)

@Entity(
  tableName = "ai_image_metadata_cache",
  indices = [Index(value = ["mediaUri"]), Index(value = ["timestamp"])]
)
data class AiImageMetadataCacheEntity(
  @PrimaryKey val mediaId: String,
  val mediaUri: String,
  val detectedText: String = "",
  val aiDescription: String = "",
  val detectedObjects: String = "",
  val documentType: String = "",
  val dominantVisualAttributes: String = "",
  val userQueries: String = "",
  val timestamp: Long = System.currentTimeMillis()
)


