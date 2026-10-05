package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    CachedMediaEntity::class,
    FavoriteEntity::class,
    ExcludedMediaEntity::class,
    CustomAlbumEntity::class,
    AlbumMediaCrossEntity::class,
    EditHistoryEntity::class,
    UserPreferenceEntity::class,
    SmartIndexEntity::class,
    SearchHistoryEntity::class,
    DocumentEntity::class,
    DocumentPageEntity::class,
    PdfHistoryEntity::class,
    EditingProjectEntity::class,
    AiImageChatSessionEntity::class,
    AiImageChatMessageEntity::class,
    AiImageMetadataCacheEntity::class,
    VaultMediaEntity::class,
    VaultAlbumEntity::class
  ],
  version = 9,
  exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
  abstract fun cachedMediaDao(): CachedMediaDao
  abstract fun favoriteDao(): FavoriteDao
  abstract fun excludedMediaDao(): ExcludedMediaDao
  abstract fun customAlbumDao(): CustomAlbumDao
  abstract fun albumMediaDao(): AlbumMediaDao
  abstract fun editHistoryDao(): EditHistoryDao
  abstract fun userPreferenceDao(): UserPreferenceDao
  abstract fun smartIndexDao(): SmartIndexDao
  abstract fun searchHistoryDao(): SearchHistoryDao
  abstract fun documentDao(): DocumentDao
  abstract fun pdfHistoryDao(): PdfHistoryDao
  abstract fun editingProjectDao(): EditingProjectDao
  abstract fun aiImageChatSessionDao(): AiImageChatSessionDao
  abstract fun aiImageChatMessageDao(): AiImageChatMessageDao
  abstract fun aiImageMetadataCacheDao(): AiImageMetadataCacheDao
  abstract fun vaultDao(): VaultDao

  companion object {
    @Volatile
    private var INSTANCE: GalleryDatabase? = null

    fun getDatabase(context: Context): GalleryDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          GalleryDatabase::class.java,
          "my_gallery_ai.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
