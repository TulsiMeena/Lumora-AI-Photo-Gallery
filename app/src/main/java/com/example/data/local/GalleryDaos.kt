package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
  @Query("SELECT * FROM favorites ORDER BY dateFavorited DESC")
  fun getAllFavorites(): Flow<List<FavoriteEntity>>

  @Query("SELECT mediaId FROM favorites")
  fun getAllFavoriteIds(): Flow<List<String>>

  @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
  suspend fun isFavorite(mediaId: String): Boolean

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorite(favorite: FavoriteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorites(favorites: List<FavoriteEntity>)

  @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
  suspend fun deleteFavorite(mediaId: String)

  @Query("DELETE FROM favorites WHERE mediaId IN (:mediaIds)")
  suspend fun deleteFavorites(mediaIds: List<String>)
}

@Dao
interface ExcludedMediaDao {
  @Query("SELECT * FROM excluded_media ORDER BY dateExcluded DESC")
  fun getAllExcluded(): Flow<List<ExcludedMediaEntity>>

  @Query("SELECT mediaId FROM excluded_media")
  fun getAllExcludedIds(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExcluded(excluded: ExcludedMediaEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExcludedList(excluded: List<ExcludedMediaEntity>)

  @Query("DELETE FROM excluded_media WHERE mediaId = :mediaId")
  suspend fun restoreExcluded(mediaId: String)

  @Query("DELETE FROM excluded_media")
  suspend fun clearAllExcluded()
}

@Dao
interface CustomAlbumDao {
  @Query("SELECT * FROM custom_albums ORDER BY createdAt DESC")
  fun getAllCustomAlbums(): Flow<List<CustomAlbumEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAlbum(album: CustomAlbumEntity)

  @Query("UPDATE custom_albums SET name = :newName WHERE id = :albumId")
  suspend fun renameAlbum(albumId: String, newName: String)

  @Query("UPDATE custom_albums SET customCoverUri = :coverUri WHERE id = :albumId")
  suspend fun updateAlbumCover(albumId: String, coverUri: String?)

  @Query("DELETE FROM custom_albums WHERE id = :albumId")
  suspend fun deleteAlbum(albumId: String)
}

@Dao
interface AlbumMediaDao {
  @Query("SELECT mediaId FROM album_media WHERE albumId = :albumId ORDER BY addedAt DESC")
  fun getMediaIdsForAlbum(albumId: String): Flow<List<String>>

  @Query("SELECT * FROM album_media")
  fun getAllAlbumMedia(): Flow<List<AlbumMediaCrossEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAlbumMedia(items: List<AlbumMediaCrossEntity>)

  @Query("DELETE FROM album_media WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
  suspend fun removeMediaFromAlbum(albumId: String, mediaIds: List<String>)

  @Query("DELETE FROM album_media WHERE albumId = :albumId")
  suspend fun deleteAlbumEntries(albumId: String)
}

@Dao
interface EditHistoryDao {
  @Query("SELECT * FROM edit_history ORDER BY editDateMillis DESC")
  fun getAllEditHistory(): Flow<List<EditHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEdit(edit: EditHistoryEntity)

  @Query("SELECT * FROM edit_history WHERE originalMediaId = :mediaId")
  fun getEditsForMedia(mediaId: String): Flow<List<EditHistoryEntity>>
}

@Dao
interface UserPreferenceDao {
  @Query("SELECT value FROM user_preferences WHERE `key` = :key")
  suspend fun getPreference(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setPreference(pref: UserPreferenceEntity)
}

@Dao
interface SmartIndexDao {
  @Query("SELECT * FROM smart_index")
  fun getAllIndexed(): Flow<List<SmartIndexEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertIndex(item: SmartIndexEntity)
}

@Dao
interface SearchHistoryDao {
  @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 30")
  fun getAllSearchHistory(): Flow<List<SearchHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSearch(search: SearchHistoryEntity)

  @Query("DELETE FROM search_history WHERE id = :id")
  suspend fun deleteSearch(id: String)

  @Query("DELETE FROM search_history")
  suspend fun clearSearchHistory()
}

@Dao
interface DocumentDao {
  @Query("SELECT * FROM scanned_documents ORDER BY dateModifiedMillis DESC")
  fun getAllDocuments(): Flow<List<DocumentEntity>>

  @Query("SELECT * FROM scanned_documents WHERE id = :id")
  suspend fun getDocumentById(id: String): DocumentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocument(document: DocumentEntity)

  @Query("UPDATE scanned_documents SET title = :newTitle WHERE id = :id")
  suspend fun renameDocument(id: String, newTitle: String)

  @Query("UPDATE scanned_documents SET category = :newCategory WHERE id = :id")
  suspend fun updateDocumentCategory(id: String, newCategory: String)

  @Query("UPDATE scanned_documents SET ocrText = :newOcrText WHERE id = :id")
  suspend fun updateDocumentOcr(id: String, newOcrText: String)

  @Query("DELETE FROM scanned_documents WHERE id = :id")
  suspend fun deleteDocument(id: String)

  @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
  fun getPagesForDocument(documentId: String): Flow<List<DocumentPageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPages(pages: List<DocumentPageEntity>)

  @Query("DELETE FROM document_pages WHERE documentId = :documentId")
  suspend fun deletePagesForDocument(documentId: String)

  @Query("DELETE FROM document_pages WHERE documentId = :documentId AND pageId = :pageId")
  suspend fun deletePage(documentId: String, pageId: String)
}

@Dao
interface PdfHistoryDao {
  @Query("SELECT * FROM pdf_history ORDER BY createdAtMillis DESC")
  fun getAllPdfs(): Flow<List<PdfHistoryEntity>>

  @Query("SELECT * FROM pdf_history WHERE id = :id")
  suspend fun getPdfById(id: String): PdfHistoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPdf(pdf: PdfHistoryEntity)

  @Query("UPDATE pdf_history SET fileName = :newFileName, filePath = :newPath WHERE id = :id")
  suspend fun renamePdf(id: String, newFileName: String, newPath: String)

  @Query("DELETE FROM pdf_history WHERE id = :id")
  suspend fun deletePdf(id: String)
}

@Dao
interface EditingProjectDao {
  @Query("SELECT * FROM editing_projects ORDER BY lastModifiedMillis DESC")
  fun getAllProjects(): Flow<List<EditingProjectEntity>>

  @Query("SELECT * FROM editing_projects WHERE id = :id")
  suspend fun getProjectById(id: String): EditingProjectEntity?

  @Query("SELECT * FROM editing_projects WHERE originalMediaId = :mediaId ORDER BY lastModifiedMillis DESC LIMIT 1")
  suspend fun getProjectByMediaId(mediaId: String): EditingProjectEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProject(project: EditingProjectEntity)

  @Query("DELETE FROM editing_projects WHERE id = :id")
  suspend fun deleteProject(id: String)

  @Query("DELETE FROM editing_projects WHERE originalMediaId = :mediaId")
  suspend fun deleteProjectsForMedia(mediaId: String)
}

@Dao
interface AiImageChatSessionDao {
  @Query("SELECT * FROM ai_image_chat_sessions ORDER BY lastUpdatedMillis DESC")
  fun getAllSessions(): Flow<List<AiImageChatSessionEntity>>

  @Query("SELECT * FROM ai_image_chat_sessions WHERE sessionId = :sessionId")
  suspend fun getSessionById(sessionId: String): AiImageChatSessionEntity?

  @Query("SELECT * FROM ai_image_chat_sessions WHERE category = :category ORDER BY lastUpdatedMillis DESC")
  fun getSessionsByCategory(category: String): Flow<List<AiImageChatSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: AiImageChatSessionEntity)

  @Query("UPDATE ai_image_chat_sessions SET title = :newTitle, lastUpdatedMillis = :updatedAt WHERE sessionId = :sessionId")
  suspend fun renameSession(sessionId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE ai_image_chat_sessions SET lastPreview = :lastPreview, lastUpdatedMillis = :updatedAt WHERE sessionId = :sessionId")
  suspend fun updateSessionPreview(sessionId: String, lastPreview: String, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM ai_image_chat_sessions WHERE sessionId = :sessionId")
  suspend fun deleteSession(sessionId: String)

  @Query("DELETE FROM ai_image_chat_sessions")
  suspend fun clearAllSessions()
}

@Dao
interface AiImageChatMessageDao {
  @Query("SELECT * FROM ai_image_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
  fun getMessagesForSession(sessionId: String): Flow<List<AiImageChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: AiImageChatMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<AiImageChatMessageEntity>)

  @Query("DELETE FROM ai_image_chat_messages WHERE sessionId = :sessionId")
  suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface AiImageMetadataCacheDao {
  @Query("SELECT * FROM ai_image_metadata_cache WHERE mediaId = :mediaId")
  suspend fun getMetadataForMedia(mediaId: String): AiImageMetadataCacheEntity?

  @Query("SELECT * FROM ai_image_metadata_cache WHERE mediaUri = :mediaUri")
  suspend fun getMetadataForUri(mediaUri: String): AiImageMetadataCacheEntity?

  @Query("SELECT * FROM ai_image_metadata_cache")
  fun getAllCachedMetadata(): Flow<List<AiImageMetadataCacheEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateMetadata(cache: AiImageMetadataCacheEntity)

  @Query("DELETE FROM ai_image_metadata_cache")
  suspend fun clearAllAiMetadata()
}

@Dao
interface CachedMediaDao {
  @Query("SELECT * FROM cached_media_metadata ORDER BY dateTakenMillis DESC")
  fun getAllCachedMediaFlow(): Flow<List<CachedMediaEntity>>

  @Query("SELECT * FROM cached_media_metadata ORDER BY dateTakenMillis DESC LIMIT :limit OFFSET :offset")
  suspend fun getPagedMedia(limit: Int, offset: Int): List<CachedMediaEntity>

  @Query("SELECT COUNT(*) FROM cached_media_metadata")
  suspend fun getMediaCount(): Int

  @Query("SELECT MAX(dateModifiedMillis) FROM cached_media_metadata")
  suspend fun getLatestModifiedTimestamp(): Long?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMediaList(list: List<CachedMediaEntity>)

  @Query("DELETE FROM cached_media_metadata WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<String>)

  @Query("DELETE FROM cached_media_metadata")
  suspend fun clearAll()
}



