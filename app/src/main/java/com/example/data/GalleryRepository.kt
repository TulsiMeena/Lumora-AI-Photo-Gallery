package com.example.data

import com.example.data.local.AlbumMediaCrossEntity
import com.example.data.local.AlbumMediaDao
import com.example.data.local.CachedMediaDao
import com.example.data.local.CachedMediaEntity
import com.example.data.local.CustomAlbumDao
import com.example.data.local.CustomAlbumEntity
import com.example.data.local.DocumentDao
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.local.EditHistoryDao
import com.example.data.local.EditHistoryEntity
import com.example.data.local.ExcludedMediaDao
import com.example.data.local.ExcludedMediaEntity
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import com.example.data.local.PdfHistoryDao
import com.example.data.local.PdfHistoryEntity
import com.example.data.local.EditingProjectDao
import com.example.data.local.EditingProjectEntity
import com.example.data.local.AiImageChatSessionDao
import com.example.data.local.AiImageChatSessionEntity
import com.example.data.local.AiImageChatMessageDao
import com.example.data.local.AiImageChatMessageEntity
import com.example.data.local.AiImageMetadataCacheDao
import com.example.data.local.AiImageMetadataCacheEntity
import com.example.data.local.SmartIndexDao
import com.example.data.local.SmartIndexEntity
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserPreferenceDao
import com.example.data.local.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GalleryRepository(
  private val favoriteDao: FavoriteDao,
  private val excludedMediaDao: ExcludedMediaDao,
  private val customAlbumDao: CustomAlbumDao,
  private val albumMediaDao: AlbumMediaDao,
  private val editHistoryDao: EditHistoryDao,
  private val userPreferenceDao: UserPreferenceDao,
  private val smartIndexDao: SmartIndexDao,
  private val searchHistoryDao: SearchHistoryDao,
  private val documentDao: DocumentDao,
  private val pdfHistoryDao: PdfHistoryDao,
  private val editingProjectDao: EditingProjectDao,
  private val aiImageChatSessionDao: AiImageChatSessionDao,
  private val aiImageChatMessageDao: AiImageChatMessageDao,
  private val aiImageMetadataCacheDao: AiImageMetadataCacheDao,
  private val cachedMediaDao: CachedMediaDao? = null
) {
  val favoriteIds: Flow<Set<String>> = favoriteDao.getAllFavoriteIds().map { it.toSet() }
  val excludedIds: Flow<Set<String>> = excludedMediaDao.getAllExcludedIds().map { it.toSet() }
  val customAlbums: Flow<List<CustomAlbumEntity>> = customAlbumDao.getAllCustomAlbums()
  val albumMediaEntries: Flow<List<AlbumMediaCrossEntity>> = albumMediaDao.getAllAlbumMedia()
  val editHistory: Flow<List<EditHistoryEntity>> = editHistoryDao.getAllEditHistory()
  val smartIndexEntries: Flow<List<SmartIndexEntity>> = smartIndexDao.getAllIndexed()
  val searchHistory: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getAllSearchHistory()
  val scannedDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
  val pdfHistory: Flow<List<PdfHistoryEntity>> = pdfHistoryDao.getAllPdfs()
  val editingProjects: Flow<List<EditingProjectEntity>> = editingProjectDao.getAllProjects()
  val aiImageChatSessions: Flow<List<AiImageChatSessionEntity>> = aiImageChatSessionDao.getAllSessions()
  val allCachedAiMetadata: Flow<List<AiImageMetadataCacheEntity>> = aiImageMetadataCacheDao.getAllCachedMetadata()

  // --- Favorites ---
  suspend fun toggleFavorite(mediaId: String, mediaUri: String) {
    if (favoriteDao.isFavorite(mediaId)) {
      favoriteDao.deleteFavorite(mediaId)
    } else {
      favoriteDao.insertFavorite(FavoriteEntity(mediaId = mediaId, mediaUri = mediaUri))
    }
  }

  suspend fun setFavorites(mediaItems: List<Pair<String, String>>, favorite: Boolean) {
    if (favorite) {
      val entities = mediaItems.map { FavoriteEntity(mediaId = it.first, mediaUri = it.second) }
      favoriteDao.insertFavorites(entities)
    } else {
      favoriteDao.deleteFavorites(mediaItems.map { it.first })
    }
  }

  suspend fun isFavorite(mediaId: String): Boolean {
    return favoriteDao.isFavorite(mediaId)
  }

  // --- Safe Exclusion / Remove from My Gallery ---
  suspend fun excludeMedia(mediaId: String, mediaUri: String) {
    excludedMediaDao.insertExcluded(ExcludedMediaEntity(mediaId = mediaId, mediaUri = mediaUri))
  }

  suspend fun excludeMediaList(mediaItems: List<Pair<String, String>>) {
    val entities = mediaItems.map { ExcludedMediaEntity(mediaId = it.first, mediaUri = it.second) }
    excludedMediaDao.insertExcludedList(entities)
  }

  suspend fun restoreExcluded(mediaId: String) {
    excludedMediaDao.restoreExcluded(mediaId)
  }

  suspend fun clearAllExcluded() {
    excludedMediaDao.clearAllExcluded()
  }

  // --- Custom Albums ---
  suspend fun createCustomAlbum(name: String, mediaIds: List<Pair<String, String?>>): String {
    val albumId = UUID.randomUUID().toString()
    val initialCoverUri = mediaIds.firstOrNull()?.second
    val album = CustomAlbumEntity(
      id = albumId,
      name = name.trim(),
      customCoverUri = initialCoverUri
    )
    customAlbumDao.insertAlbum(album)

    if (mediaIds.isNotEmpty()) {
      val crossEntities = mediaIds.map {
        AlbumMediaCrossEntity(
          albumId = albumId,
          mediaId = it.first,
          mediaUri = it.second
        )
      }
      albumMediaDao.insertAlbumMedia(crossEntities)
    }
    return albumId
  }

  suspend fun renameCustomAlbum(albumId: String, newName: String) {
    customAlbumDao.renameAlbum(albumId, newName.trim())
  }

  suspend fun updateCustomAlbumCover(albumId: String, coverUri: String?) {
    customAlbumDao.updateAlbumCover(albumId, coverUri)
  }

  /**
   * Deleting a custom album deletes the album record and the album membership mapping,
   * completely preserving the user's original phone media untouched!
   */
  suspend fun deleteCustomAlbum(albumId: String) {
    albumMediaDao.deleteAlbumEntries(albumId)
    customAlbumDao.deleteAlbum(albumId)
  }

  suspend fun addMediaToAlbum(albumId: String, mediaItems: List<Pair<String, String?>>) {
    val crossEntities = mediaItems.map {
      AlbumMediaCrossEntity(
        albumId = albumId,
        mediaId = it.first,
        mediaUri = it.second
      )
    }
    albumMediaDao.insertAlbumMedia(crossEntities)
  }

  suspend fun removeMediaFromAlbum(albumId: String, mediaIds: List<String>) {
    albumMediaDao.removeMediaFromAlbum(albumId, mediaIds)
  }

  // --- Recently Edited ---
  suspend fun recordEdit(
    originalMediaId: String,
    originalUri: String,
    editedUri: String,
    editType: String,
    colorSeed: Int = 0
  ) {
    val id = UUID.randomUUID().toString()
    val entity = EditHistoryEntity(
      id = id,
      originalMediaId = originalMediaId,
      originalUri = originalUri,
      editedUri = editedUri,
      editType = editType,
      editDateMillis = System.currentTimeMillis(),
      previewColorSeed = colorSeed
    )
    editHistoryDao.insertEdit(entity)
  }

  // --- Preferences ---
  suspend fun savePreference(key: String, value: String) {
    userPreferenceDao.setPreference(UserPreferenceEntity(key = key, value = value))
  }

  suspend fun getPreference(key: String): String? {
    return userPreferenceDao.getPreference(key)
  }

  // --- Smart Indexing ---
  suspend fun updateSmartIndex(index: SmartIndexEntity) {
    smartIndexDao.insertIndex(index)
  }

  // --- Search History ---
  suspend fun addSearchHistory(query: String, resultCount: Int) {
    if (query.isBlank()) return
    val entity = SearchHistoryEntity(
      id = UUID.randomUUID().toString(),
      query = query.trim(),
      timestamp = System.currentTimeMillis(),
      resultCount = resultCount
    )
    searchHistoryDao.insertSearch(entity)
  }

  suspend fun deleteSearchHistory(id: String) {
    searchHistoryDao.deleteSearch(id)
  }

  suspend fun clearSearchHistory() {
    searchHistoryDao.clearSearchHistory()
  }

  // --- Scanned Documents & Multi-page Persistence ---
  fun getPagesForDocument(documentId: String): Flow<List<DocumentPageEntity>> {
    return documentDao.getPagesForDocument(documentId)
  }

  suspend fun saveDocument(document: DocumentEntity, pages: List<DocumentPageEntity>) {
    documentDao.insertDocument(document)
    if (pages.isNotEmpty()) {
      documentDao.insertPages(pages)
    }
  }

  suspend fun renameDocument(documentId: String, newTitle: String) {
    documentDao.renameDocument(documentId, newTitle.trim())
  }

  suspend fun updateDocumentCategory(documentId: String, newCategory: String) {
    documentDao.updateDocumentCategory(documentId, newCategory)
  }

  suspend fun updateDocumentOcr(documentId: String, newOcrText: String) {
    documentDao.updateDocumentOcr(documentId, newOcrText)
  }

  suspend fun deleteDocument(documentId: String) {
    documentDao.deletePagesForDocument(documentId)
    documentDao.deleteDocument(documentId)
  }

  suspend fun deleteDocumentPage(documentId: String, pageId: String) {
    documentDao.deletePage(documentId, pageId)
  }

  suspend fun addDocumentPages(pages: List<DocumentPageEntity>) {
    documentDao.insertPages(pages)
  }

  // --- PDF Studio & History Persistence ---
  suspend fun savePdfHistory(pdf: PdfHistoryEntity) {
    pdfHistoryDao.insertPdf(pdf)
  }

  suspend fun renamePdf(id: String, newFileName: String, newPath: String) {
    pdfHistoryDao.renamePdf(id, newFileName, newPath)
  }

  suspend fun deletePdf(id: String) {
    pdfHistoryDao.deletePdf(id)
  }

  // --- Photo Editor Non-Destructive Projects ---
  suspend fun saveEditingProject(project: EditingProjectEntity) {
    editingProjectDao.insertProject(project)
  }

  suspend fun deleteEditingProject(id: String) {
    editingProjectDao.deleteProject(id)
  }

  suspend fun getEditingProjectByMediaId(mediaId: String): EditingProjectEntity? {
    return editingProjectDao.getProjectByMediaId(mediaId)
  }

  // --- AI Image Understanding Sessions & History ---
  suspend fun saveAiChatSession(session: AiImageChatSessionEntity) {
    aiImageChatSessionDao.insertSession(session)
  }

  suspend fun renameAiChatSession(sessionId: String, newTitle: String) {
    aiImageChatSessionDao.renameSession(sessionId, newTitle)
  }

  suspend fun deleteAiChatSession(sessionId: String) {
    aiImageChatMessageDao.deleteMessagesForSession(sessionId)
    aiImageChatSessionDao.deleteSession(sessionId)
  }

  suspend fun clearAllAiChatSessions() {
    aiImageChatSessionDao.clearAllSessions()
  }

  fun getMessagesForAiSession(sessionId: String): Flow<List<AiImageChatMessageEntity>> {
    return aiImageChatMessageDao.getMessagesForSession(sessionId)
  }

  suspend fun saveAiChatMessage(message: AiImageChatMessageEntity) {
    aiImageChatMessageDao.insertMessage(message)
    aiImageChatSessionDao.updateSessionPreview(
      sessionId = message.sessionId,
      lastPreview = message.text.take(120)
    )
  }

  // --- AI Image Metadata Cache (Search Integration & Privacy) ---
  suspend fun saveAiMetadataCache(cache: AiImageMetadataCacheEntity) {
    aiImageMetadataCacheDao.insertOrUpdateMetadata(cache)
  }

  suspend fun getAiMetadataForMedia(mediaId: String): AiImageMetadataCacheEntity? {
    return aiImageMetadataCacheDao.getMetadataForMedia(mediaId)
  }

  suspend fun clearAllAiMetadataCache() {
    aiImageMetadataCacheDao.clearAllAiMetadata()
  }

  suspend fun getCachedMediaCount(): Int {
    return cachedMediaDao?.getMediaCount() ?: 0
  }

  suspend fun clearCachedMedia() {
    cachedMediaDao?.clearAll()
  }
}

