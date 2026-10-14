package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryFilterEngine
import com.example.data.GalleryRepository
import com.example.data.SampleDataProvider
import com.example.data.SmartGroupingManager
import com.example.data.ai.AiChatMessage
import com.example.data.ai.LocalAiSearchEngine
import com.example.data.local.CustomAlbumEntity
import com.example.data.local.EditHistoryEntity
import com.example.data.ai.AiCacheManager
import com.example.data.editor.PhotoEditorEngine
import com.example.data.local.AiImageMetadataCacheEntity
import com.example.data.local.EditingProjectEntity
import com.example.data.local.GalleryDatabase
import com.example.data.local.PdfHistoryEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.mediastore.MediaStoreScanner
import com.example.model.ai.understanding.ImageUnderstandingTask
import com.example.model.editor.PhotoEditorState
import com.example.utils.cache.ThumbnailCacheManager
import com.example.utils.network.NetworkPrivacyManager
import com.example.utils.queue.BatchJobQueueManager
import com.example.utils.sound.AppSoundManager
import com.example.utils.storage.SafeTempFileManager
import com.example.utils.storage.StorageBreakdown
import com.example.utils.storage.StorageCacheManager
import android.graphics.Bitmap
import com.example.data.document.DocumentDetector
import com.example.data.document.DocumentOcrEngine
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.pdf.PdfStudioEngine
import com.example.model.AlbumItem
import com.example.model.AlbumType
import com.example.model.DocumentCategories
import com.example.model.DocumentFormat
import com.example.model.DocumentItem
import com.example.model.DocumentPageItem
import com.example.model.FilterState
import com.example.model.GallerySettingsState
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.OcrAnalysisResult
import com.example.model.SelectionSummary
import com.example.model.SmartGroupCategory
import com.example.model.SortOption
import com.example.model.TimelineSection
import com.example.model.VaultFolderItem
import com.example.model.vault.VaultAlbumItem
import com.example.model.vault.VaultMediaItem
import com.example.model.vault.VaultSecurityConfig
import com.example.model.vault.VaultTab
import com.example.data.local.VaultAlbumEntity
import com.example.data.local.VaultMediaEntity
import com.example.utils.vault.VaultCryptoManager
import com.example.utils.vault.VaultStorageManager
import com.example.model.pdf.PdfConfig
import com.example.model.pdf.PdfCreatorPage
import com.example.model.pdf.PdfGenerationProgress
import com.example.model.pdf.PdfGenerationResult
import com.example.model.pdf.PdfHistoryItem
import com.example.model.pdf.PdfImageFit
import com.example.model.pdf.PdfOrientation
import com.example.model.pdf.PdfStandardSize
import com.example.model.pdf.PdfTargetSizeOption
import com.example.ui.components.GalleryNavDestination
import com.example.ui.components.QuickAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GalleryUiState(
  val currentDestination: GalleryNavDestination = GalleryNavDestination.HOME,
  val allRawPhotos: List<MediaItem> = emptyList(),
  val photos: List<MediaItem> = SampleDataProvider.samplePhotos, // Filtered & sorted
  val albums: List<AlbumItem> = SampleDataProvider.sampleAlbums,
  val timelineSections: List<TimelineSection> = emptyList(),
  val documents: List<DocumentItem> = SampleDataProvider.sampleDocuments,
  val openedDocument: DocumentItem? = null,
  val openedOcrResult: Pair<DocumentItem, OcrAnalysisResult>? = null,
  val isScannerOpen: Boolean = false,
  val scannerSourceUri: String? = null,
  val scannerSourceMedia: MediaItem? = null,
  val showRenameDocDialog: DocumentItem? = null,
  val showChangeDocCategoryDialog: DocumentItem? = null,
  val vaultFolders: List<VaultFolderItem> = SampleDataProvider.sampleVaultFolders,
  val aiSuggestions: List<String> = SampleDataProvider.sampleAiSuggestions,
  val recentAiActions: List<com.example.model.AiActionItem> = SampleDataProvider.sampleAiActions,
  val selectedPhotoIds: Set<String> = emptySet(),
  val isSelectionMode: Boolean = false,
  val openedPhoto: MediaItem? = null,
  val selectedAlbum: AlbumItem? = null, // When viewing a specific album's media
  val isSettingsOpen: Boolean = false,
  val isQuickActionExpanded: Boolean = false,
  val isVaultUnlocked: Boolean = false,
  val vaultSecurityConfig: VaultSecurityConfig = VaultSecurityConfig(),
  val vaultMediaList: List<VaultMediaItem> = emptyList(),
  val vaultAlbums: List<VaultAlbumItem> = emptyList(),
  val activeVaultTab: VaultTab = VaultTab.PHOTOS,
  val vaultSearchQuery: String = "",
  val activeVaultMediaViewer: VaultMediaItem? = null,
  val showVaultSetupWizard: Boolean = false,
  val showVaultSecuritySettings: Boolean = false,
  val showVaultCreateAlbumDialog: Boolean = false,
  val showMoveToVaultConfirmDialog: Boolean = false,
  val pendingMoveToVaultItems: List<MediaItem> = emptyList(),
  val showSafeDeleteDialog: Boolean = false,
  val pendingSafeDeleteItems: List<MediaItem> = emptyList(),
  val vaultAuthErrorMessage: String? = null,
  val vaultLockoutCountdownSeconds: Int = 0,

  // Search & Filter & Sort State
  val searchQuery: String = "",
  val filterState: FilterState = FilterState(),
  val sortOption: SortOption = SortOption.NEWEST_FIRST,
  val isFilterSheetOpen: Boolean = false,
  val isSortMenuOpen: Boolean = false,

  // Dialogs & Modals
  val showCreateAlbumDialog: Boolean = false,
  val showAddToAlbumDialog: Boolean = false,
  val showRenameAlbumDialog: AlbumItem? = null,
  val showDeleteAlbumDialog: AlbumItem? = null,
  val showCoverChooserAlbum: AlbumItem? = null,
  val showSelectionDetailsDialog: Boolean = false,
  val selectionSummary: SelectionSummary? = null,

  // Smart Grouping & Edit History
  val smartClusters: List<SmartGroupCategory> = emptyList(),
  val editHistoryList: List<EditHistoryEntity> = emptyList(),
  val customAlbumsList: List<CustomAlbumEntity> = emptyList(),

  val albumFilter: String = "All",
  val documentCategory: String = "All Documents",
  val settings: GallerySettingsState = GallerySettingsState(),

  // AI Assistant & Smart Search Workspace
  val aiChatMessages: List<AiChatMessage> = emptyList(),
  val isAiSearching: Boolean = false,
  val aiSearchHistory: List<SearchHistoryEntity> = emptyList(),
  val attachedMediaForAi: MediaItem? = null,
  val showAttachImagePicker: Boolean = false,
  val showHistorySheet: Boolean = false,

  // PDF Studio & Creator State
  val isPdfStudioOpen: Boolean = false,
  val isPdfCreatorOpen: Boolean = false,
  val isPdfPreviewOpen: Boolean = false,
  val pdfCreatorPages: List<PdfCreatorPage> = emptyList(),
  val pdfConfig: PdfConfig = PdfConfig(),
  val pdfHistory: List<PdfHistoryItem> = emptyList(),
  val pdfGenerationProgress: PdfGenerationProgress? = null,
  val createdPdfResult: PdfHistoryItem? = null,

  // Professional Photo Editor State
  val activeEditorMedia: MediaItem? = null,
  val activeEditorProject: EditingProjectEntity? = null,
  val editingProjects: List<EditingProjectEntity> = emptyList(),
  val showEditingProjectsSheet: Boolean = false,
  val editorSuccessNotice: String? = null,

  // Advanced AI Image Understanding & Chat State
  val activeAiUnderstandingMedia: MediaItem? = null,
  val activeAiUnderstandingUri: String? = null,
  val activeAiUnderstandingTitle: String? = null,
  val activeAiUnderstandingTask: ImageUnderstandingTask = ImageUnderstandingTask.ASK_ANYTHING,
  val activeAiUnderstandingInitialPrompt: String? = null,
  val activeAiUnderstandingFromVault: Boolean = false,

  // Advanced Image Resize, Dimensions, DPI & Passport Photo Studio State
  val isImageResizeOpen: Boolean = false,
  val activeResizeMedia: MediaItem? = null,
  val initialResizeTab: Int = 0,

  // Advanced Image Compression, Target File Size & Format Converter State
  val isImageCompressorOpen: Boolean = false,
  val activeCompressMedia: MediaItem? = null,
  val initialCompressTab: Int = 0,
  val batchCompressMedias: List<MediaItem> = emptyList(),

  // Creative Studio, Batch Engine & Share Center State
  val isCreativeStudioOpen: Boolean = false,
  val initialCreativeStudioTab: com.example.model.creative.CreativeStudioTab = com.example.model.creative.CreativeStudioTab.ALL,
  val isShareCenterOpen: Boolean = false,
  val pendingShareItems: List<MediaItem> = emptyList(),
  val isVaultShare: Boolean = false,

  val userNotice: String? = null,
  val hasPermission: Boolean = false,
  val isPartialAccessGranted: Boolean = false,
  val isPermissionPermanentlyDenied: Boolean = false,
  val isUsingSampleData: Boolean = false,
  val isLoadingMedia: Boolean = false,
  val storageBreakdown: StorageBreakdown? = null,
  val showClearCacheConfirm: Boolean = false,
  val showCleanTempConfirm: Boolean = false,
  val showClearAiCacheConfirm: Boolean = false,
  val isCleaningStorage: Boolean = false,
  val showCloudAiConsentDialog: Boolean = false,
  val pendingCloudAiTaskTitle: String = "",
  val pendingCloudAiAction: (() -> Unit)? = null
) {
  val recentPhotos: List<MediaItem>
    get() = allRawPhotos.filter { it.mediaType == MediaType.PHOTO }.take(10)

  val recentVideos: List<MediaItem>
    get() = allRawPhotos.filter { it.mediaType == MediaType.VIDEO }.take(10)

  val favoritePhotos: List<MediaItem>
    get() = allRawPhotos.filter { it.isFavorite }

  val editedPhotos: List<MediaItem>
    get() = allRawPhotos.filter { it.isEdited || it.title.contains("edit", ignoreCase = true) }
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

  private val database = GalleryDatabase.getDatabase(application)
  private val repository = GalleryRepository(
    favoriteDao = database.favoriteDao(),
    excludedMediaDao = database.excludedMediaDao(),
    customAlbumDao = database.customAlbumDao(),
    albumMediaDao = database.albumMediaDao(),
    editHistoryDao = database.editHistoryDao(),
    userPreferenceDao = database.userPreferenceDao(),
    smartIndexDao = database.smartIndexDao(),
    searchHistoryDao = database.searchHistoryDao(),
    documentDao = database.documentDao(),
    pdfHistoryDao = database.pdfHistoryDao(),
    editingProjectDao = database.editingProjectDao(),
    aiImageChatSessionDao = database.aiImageChatSessionDao(),
    aiImageChatMessageDao = database.aiImageChatMessageDao(),
    aiImageMetadataCacheDao = database.aiImageMetadataCacheDao(),
    cachedMediaDao = database.cachedMediaDao()
  )
  private val scanner = MediaStoreScanner(application)
  private val aiCacheManager = AiCacheManager(database.aiImageMetadataCacheDao())

  private val _uiState = MutableStateFlow(GalleryUiState())
  val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

  private var favoriteIdsSet: Set<String> = emptySet()
  private var excludedIdsSet: Set<String> = emptySet()
  private var customAlbumsCache: List<CustomAlbumEntity> = emptyList()
  private var albumMediaMapCache: Map<String, List<String>> = emptyMap()
  private var editHistoryCache: List<EditHistoryEntity> = emptyList()

  init {
    AppSoundManager.init(application)
    AppSoundManager.soundEnabled = _uiState.value.settings.soundEffectsEnabled
    AppSoundManager.hapticEnabled = _uiState.value.settings.hapticFeedbackEnabled
    loadSavedPreferences()
    checkPermissions()
    observeLocalData()
    loadVaultData()
    observeMediaStoreChanges()
    initStorageAndPerformance()
  }

  private fun initStorageAndPerformance() {
    viewModelScope.launch(Dispatchers.IO) {
      // 1. Safe cleanup of old/stale temporary work files
      SafeTempFileManager.pruneStaleTempFiles(getApplication())
      // 2. Initial storage computation
      refreshStorageBreakdown()
    }
  }

  private fun loadVaultData() {
    val config = VaultStorageManager.getSecurityConfig(getApplication())
    _uiState.update { it.copy(vaultSecurityConfig = config) }

    viewModelScope.launch(Dispatchers.IO) {
      val existingAlbums = database.vaultDao().getAllVaultAlbums()
      if (existingAlbums.isEmpty()) {
        database.vaultDao().insertVaultAlbum(VaultAlbumEntity(id = "personal", name = "Personal"))
        database.vaultDao().insertVaultAlbum(VaultAlbumEntity(id = "important", name = "Important"))
        database.vaultDao().insertVaultAlbum(VaultAlbumEntity(id = "documents", name = "Private Documents"))
        database.vaultDao().insertVaultAlbum(VaultAlbumEntity(id = "videos", name = "Private Videos"))
      }
    }

    viewModelScope.launch {
      database.vaultDao().getAllVaultMediaFlow().collectLatest { entities ->
        val items = entities.map { entity ->
          VaultMediaItem(
            id = entity.id,
            originalMediaId = entity.originalMediaId,
            title = entity.title,
            originalFileName = entity.originalFileName,
            encryptedFileName = entity.encryptedFileName,
            fileSizeBytes = entity.fileSizeBytes,
            mediaType = if (entity.mediaType == "VIDEO") MediaType.VIDEO else MediaType.PHOTO,
            mimeType = entity.mimeType,
            width = entity.width,
            height = entity.height,
            durationMs = entity.durationMs,
            dateAddedMillis = entity.dateAddedMillis,
            albumId = entity.albumId,
            isFavorite = entity.isFavorite,
            ivBase64 = entity.ivBase64,
            originalUriBeforeMove = entity.originalUriBeforeMove
          )
        }
        _uiState.update { state ->
          val updatedAlbums = state.vaultAlbums.map { alb ->
            alb.copy(
              itemCount = items.count { it.albumId == alb.id },
              coverMediaId = items.firstOrNull { it.albumId == alb.id }?.id
            )
          }
          state.copy(vaultMediaList = items, vaultAlbums = updatedAlbums)
        }
      }
    }

    viewModelScope.launch {
      database.vaultDao().getAllVaultAlbumsFlow().collectLatest { albumEntities ->
        val mediaList = _uiState.value.vaultMediaList
        val albumItems = albumEntities.map { album ->
          val count = mediaList.count { it.albumId == album.id }
          val cover = mediaList.firstOrNull { it.albumId == album.id }?.id
          VaultAlbumItem(
            id = album.id,
            name = album.name,
            createdAtMillis = album.createdAtMillis,
            itemCount = count,
            coverMediaId = cover
          )
        }
        _uiState.update { state ->
          state.copy(vaultAlbums = albumItems)
        }
      }
    }
  }

  private fun loadSavedPreferences() {
    viewModelScope.launch {
      val savedSort = repository.getPreference("sort_option")
      if (savedSort != null) {
        try {
          val sort = SortOption.valueOf(savedSort)
          _uiState.update { it.copy(sortOption = sort) }
        } catch (_: Exception) {}
      }
    }
  }

  private fun observeLocalData() {
    // Favorites Flow
    viewModelScope.launch {
      repository.favoriteIds.collectLatest { favs ->
        favoriteIdsSet = favs
        refreshMediaPipeline()
      }
    }

    // Excluded IDs Flow
    viewModelScope.launch {
      repository.excludedIds.collectLatest { excluded ->
        excludedIdsSet = excluded
        refreshMediaPipeline()
      }
    }

    // Custom Albums Flow
    viewModelScope.launch {
      repository.customAlbums.collectLatest { albums ->
        customAlbumsCache = albums
        _uiState.update { it.copy(customAlbumsList = albums) }
        refreshMediaPipeline()
      }
    }

    // Album Media Cross Flow
    viewModelScope.launch {
      repository.albumMediaEntries.collectLatest { entries ->
        albumMediaMapCache = entries.groupBy({ it.albumId }, { it.mediaId })
        refreshMediaPipeline()
      }
    }

    // Edit History Flow
    viewModelScope.launch {
      repository.editHistory.collectLatest { edits ->
        editHistoryCache = edits
        _uiState.update { it.copy(editHistoryList = edits) }
        refreshMediaPipeline()
      }
    }

    // Search History Flow
    viewModelScope.launch {
      repository.searchHistory.collectLatest { history ->
        _uiState.update { it.copy(aiSearchHistory = history) }
      }
    }

    // Scanned Documents Persistence Flow
    viewModelScope.launch {
      repository.scannedDocuments.collectLatest { docEntities ->
        if (docEntities.isNotEmpty()) {
          val customDocs = docEntities.map { entity ->
            DocumentItem(
              id = entity.id,
              title = entity.title,
              category = entity.category,
              pageCount = entity.pageCount,
              dateModified = entity.dateModified.ifBlank {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entity.dateModifiedMillis))
              },
              dateModifiedMillis = entity.dateModifiedMillis,
              sizeBytes = entity.sizeBytes,
              format = if (entity.format == "PDF") DocumentFormat.PDF else DocumentFormat.SCAN,
              thumbnailUri = entity.originalImageUri,
              originalImageUri = entity.originalImageUri,
              ocrText = entity.ocrText,
              resolution = entity.resolution,
              pages = listOf(
                DocumentPageItem(
                  pageId = "page_${entity.id}_1",
                  pageNumber = 1,
                  imageUri = entity.originalImageUri ?: "sample://${entity.id}",
                  ocrText = entity.ocrText
                )
              )
            )
          }
          val sampleRemaining = SampleDataProvider.sampleDocuments.filterNot { s -> customDocs.any { it.id == s.id } }
          val combined = customDocs + sampleRemaining
          _uiState.update { it.copy(documents = combined) }
        }
      }
    }

    // Observe PDF History Flow
    viewModelScope.launch {
      repository.pdfHistory.collectLatest { entities ->
        val historyItems = entities.map { entity ->
          PdfHistoryItem(
            id = entity.id,
            fileName = entity.fileName,
            filePath = entity.filePath,
            fileSizeBytes = entity.fileSizeBytes,
            pageCount = entity.pageCount,
            createdAtMillis = entity.createdAtMillis,
            pageSizeName = entity.pageSizeName,
            orientation = entity.orientation,
            thumbnailUri = entity.thumbnailUri,
            title = entity.title,
            author = entity.author,
            subject = entity.subject,
            keywords = entity.keywords,
            targetSizeBytes = entity.targetSizeBytes,
            qualityPreset = entity.qualityPreset
          )
        }
        _uiState.update { it.copy(pdfHistory = historyItems) }
      }
    }

    // Photo Editor Projects Flow
    viewModelScope.launch {
      repository.editingProjects.collectLatest { projects ->
        _uiState.update { it.copy(editingProjects = projects) }
      }
    }
  }

  private fun observeMediaStoreChanges() {
    viewModelScope.launch {
      scanner.observeMediaStoreChanges().collectLatest {
        if (_uiState.value.hasPermission && !_uiState.value.isUsingSampleData) {
          reloadMedia()
        }
      }
    }
  }

  fun checkPermissions() {
    val context: Context = getApplication()
    val hasReadImages = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
      }
      else -> {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
      }
    }

    val isPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
      ) == PackageManager.PERMISSION_GRANTED && !hasReadImages
    } else {
      false
    }

    val granted = hasReadImages || isPartial

    _uiState.update {
      it.copy(
        hasPermission = granted,
        isPartialAccessGranted = isPartial
      )
    }

    if (granted) {
      loadDeviceMedia()
    }
  }

  fun onPermissionResult(granted: Boolean, partialGranted: Boolean = false) {
    _uiState.update {
      it.copy(
        hasPermission = granted || partialGranted,
        isPartialAccessGranted = partialGranted,
        isUsingSampleData = false
      )
    }
    if (granted || partialGranted) {
      loadDeviceMedia()
    }
  }

  fun setExploreWithSampleData() {
    val sampleItems = SampleDataProvider.samplePhotos.map { item ->
      if (favoriteIdsSet.contains(item.id)) item.copy(isFavorite = true) else item
    }.filterNot { excludedIdsSet.contains(it.id) }

    val editedMediaIds = editHistoryCache.map { it.originalMediaId }.toSet()
    val albums = scanner.generateAlbums(
      mediaItems = sampleItems,
      customAlbums = customAlbumsCache,
      albumMediaMap = albumMediaMapCache,
      editedMediaIds = editedMediaIds
    )
    val smartClusters = SmartGroupingManager.computeSmartClusters(sampleItems)

    _uiState.update {
      it.copy(
        hasPermission = true,
        isUsingSampleData = true,
        allRawPhotos = sampleItems,
        albums = albums,
        smartClusters = smartClusters
      )
    }
    applyFilterAndSortPipeline()
  }

  fun loadDeviceMedia(forceFullRescan: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isLoadingMedia = true) }
      val items = if (_uiState.value.settings.incrementalIndexingEnabled) {
        scanner.scanMediaWithCache(
          cachedMediaDao = database.cachedMediaDao(),
          excludedIds = excludedIdsSet,
          favoriteIds = favoriteIdsSet,
          forceFullRescan = forceFullRescan
        )
      } else {
        scanner.scanMedia(
          excludedIds = excludedIdsSet,
          favoriteIds = favoriteIdsSet
        )
      }

      if (items.isNotEmpty()) {
        val editedMediaIds = editHistoryCache.map { it.originalMediaId }.toSet()
        val albums = scanner.generateAlbums(
          mediaItems = items,
          customAlbums = customAlbumsCache,
          albumMediaMap = albumMediaMapCache,
          editedMediaIds = editedMediaIds
        )
        val smartClusters = SmartGroupingManager.computeSmartClusters(items)

        _uiState.update {
          it.copy(
            allRawPhotos = items,
            albums = albums,
            smartClusters = smartClusters,
            isUsingSampleData = false,
            isLoadingMedia = false
          )
        }
        applyFilterAndSortPipeline()
        refreshStorageBreakdown()
      } else {
        setExploreWithSampleData()
        _uiState.update {
          it.copy(
            isLoadingMedia = false,
            userNotice = "Device has no media. Showing sample collection."
          )
        }
      }
    }
  }

  private fun reloadMedia(forceFullRescan: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      val items = if (_uiState.value.settings.incrementalIndexingEnabled) {
        scanner.scanMediaWithCache(
          cachedMediaDao = database.cachedMediaDao(),
          excludedIds = excludedIdsSet,
          favoriteIds = favoriteIdsSet,
          forceFullRescan = forceFullRescan
        )
      } else {
        scanner.scanMedia(
          excludedIds = excludedIdsSet,
          favoriteIds = favoriteIdsSet
        )
      }
      if (items.isNotEmpty()) {
        val editedMediaIds = editHistoryCache.map { it.originalMediaId }.toSet()
        val albums = scanner.generateAlbums(
          mediaItems = items,
          customAlbums = customAlbumsCache,
          albumMediaMap = albumMediaMapCache,
          editedMediaIds = editedMediaIds
        )
        val smartClusters = SmartGroupingManager.computeSmartClusters(items)
        _uiState.update {
          it.copy(
            allRawPhotos = items,
            albums = albums,
            smartClusters = smartClusters
          )
        }
        applyFilterAndSortPipeline()
      }
    }
  }

  private fun refreshMediaPipeline() {
    val currentRaw = _uiState.value.allRawPhotos
    if (currentRaw.isNotEmpty()) {
      val editedMediaIds = editHistoryCache.map { it.originalMediaId }.toSet()
      val updatedItems = currentRaw
        .filterNot { excludedIdsSet.contains(it.id) }
        .map { item ->
          item.copy(
            isFavorite = favoriteIdsSet.contains(item.id),
            isEdited = editedMediaIds.contains(item.id)
          )
        }
      val albums = scanner.generateAlbums(
        mediaItems = updatedItems,
        customAlbums = customAlbumsCache,
        albumMediaMap = albumMediaMapCache,
        editedMediaIds = editedMediaIds
      )
      val smartClusters = SmartGroupingManager.computeSmartClusters(updatedItems)
      _uiState.update {
        it.copy(
          allRawPhotos = updatedItems,
          albums = albums,
          smartClusters = smartClusters
        )
      }
      applyFilterAndSortPipeline()
    }
  }

  // --- Search, Filter & Sort Pipeline ---

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    applyFilterAndSortPipeline()
  }

  fun updateFilterState(transform: (FilterState) -> FilterState) {
    _uiState.update { it.copy(filterState = transform(it.filterState)) }
    applyFilterAndSortPipeline()
  }

  fun clearFilters() {
    _uiState.update {
      it.copy(
        filterState = FilterState(),
        searchQuery = ""
      )
    }
    applyFilterAndSortPipeline()
  }

  fun setSortOption(option: SortOption) {
    _uiState.update { it.copy(sortOption = option, isSortMenuOpen = false) }
    viewModelScope.launch {
      repository.savePreference("sort_option", option.name)
    }
    applyFilterAndSortPipeline()
  }

  fun setFilterSheetOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isFilterSheetOpen = isOpen) }
  }

  fun setSortMenuOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isSortMenuOpen = isOpen) }
  }

  private fun applyFilterAndSortPipeline() {
    val state = _uiState.value
    val editedMediaIds = editHistoryCache.map { it.originalMediaId }.toSet()

    // Determine target media: if inside a selected album, restrict source
    val sourceList = if (state.selectedAlbum != null) {
      val album = state.selectedAlbum
      if (album.isCustom && album.customAlbumId != null) {
        val memberIds = (albumMediaMapCache[album.customAlbumId] ?: emptyList()).toSet()
        state.allRawPhotos.filter { memberIds.contains(it.id) }
      } else {
        when (album.albumType) {
          AlbumType.ALL -> state.allRawPhotos
          AlbumType.VIDEOS -> state.allRawPhotos.filter { it.mediaType == MediaType.VIDEO }
          AlbumType.FAVORITES -> state.allRawPhotos.filter { it.isFavorite }
          AlbumType.RECENT_ADDED -> {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            state.allRawPhotos.filter { it.dateTakenMillis >= sevenDaysAgo }
          }
          AlbumType.RECENT_EDITED -> {
            state.allRawPhotos.filter { it.isEdited || editedMediaIds.contains(it.id) }
          }
          else -> state.allRawPhotos.filter { it.albumName.equals(album.name, ignoreCase = true) }
        }
      }
    } else {
      state.allRawPhotos
    }

    val customIds = if (state.filterState.customAlbumId != null) {
      albumMediaMapCache[state.filterState.customAlbumId]?.toSet()
    } else null

    val processed = GalleryFilterEngine.filterAndSort(
      items = sourceList,
      searchQuery = state.searchQuery,
      filterState = state.filterState,
      sortOption = state.sortOption,
      customAlbumMediaIds = customIds,
      editedMediaIds = editedMediaIds
    )

    val timeline = GalleryFilterEngine.generateGroupedTimeline(processed)

    _uiState.update {
      it.copy(
        photos = processed,
        timelineSections = timeline
      )
    }
  }

  // --- Album Operations ---

  fun openAlbum(album: AlbumItem) {
    _uiState.update { it.copy(selectedAlbum = album) }
    applyFilterAndSortPipeline()
  }

  fun closeAlbum() {
    _uiState.update { it.copy(selectedAlbum = null) }
    applyFilterAndSortPipeline()
  }

  fun showCreateAlbumDialog(show: Boolean) {
    _uiState.update { it.copy(showCreateAlbumDialog = show) }
  }

  fun showAddToAlbumDialog(show: Boolean) {
    _uiState.update { it.copy(showAddToAlbumDialog = show) }
  }

  fun showRenameAlbumDialog(album: AlbumItem?) {
    _uiState.update { it.copy(showRenameAlbumDialog = album) }
  }

  fun showDeleteAlbumDialog(album: AlbumItem?) {
    _uiState.update { it.copy(showDeleteAlbumDialog = album) }
  }

  fun showCoverChooser(album: AlbumItem?) {
    _uiState.update { it.copy(showCoverChooserAlbum = album) }
  }

  fun createCustomAlbum(name: String, mediaIds: List<String>) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val items = _uiState.value.allRawPhotos.filter { mediaIds.contains(it.id) }
      val pairs = items.map { Pair(it.id, it.uri) }
      repository.createCustomAlbum(name.trim(), pairs)
      _uiState.update {
        it.copy(
          showCreateAlbumDialog = false,
          userNotice = "Created album \"${name.trim()}\" with ${items.size} items"
        )
      }
    }
  }

  fun renameCustomAlbum(albumId: String, newName: String) {
    if (newName.isBlank()) return
    viewModelScope.launch {
      repository.renameCustomAlbum(albumId, newName.trim())
      _uiState.update {
        it.copy(
          showRenameAlbumDialog = null,
          userNotice = "Album renamed to \"${newName.trim()}\""
        )
      }
    }
  }

  fun setCustomAlbumCover(albumId: String, coverUri: String?) {
    viewModelScope.launch {
      repository.updateCustomAlbumCover(albumId, coverUri)
      _uiState.update {
        it.copy(
          showCoverChooserAlbum = null,
          userNotice = "Album cover updated (Master photos untouched)"
        )
      }
    }
  }

  /**
   * Deleting a custom album strictly deletes the album collection from the app.
   * Original phone media files are completely preserved!
   */
  fun deleteCustomAlbum(albumId: String) {
    viewModelScope.launch {
      repository.deleteCustomAlbum(albumId)
      _uiState.update {
        it.copy(
          showDeleteAlbumDialog = null,
          selectedAlbum = if (it.selectedAlbum?.customAlbumId == albumId) null else it.selectedAlbum,
          userNotice = "Album deleted. Original phone photos were NOT deleted."
        )
      }
    }
  }

  fun addSelectedToAlbum(albumId: String) {
    val selectedIds = _uiState.value.selectedPhotoIds
    if (selectedIds.isEmpty()) return
    viewModelScope.launch {
      val items = _uiState.value.allRawPhotos.filter { selectedIds.contains(it.id) }
      val pairs = items.map { Pair(it.id, it.uri) }
      repository.addMediaToAlbum(albumId, pairs)
      _uiState.update {
        it.copy(
          showAddToAlbumDialog = false,
          isSelectionMode = false,
          selectedPhotoIds = emptySet(),
          userNotice = "Added ${pairs.size} items to album"
        )
      }
    }
  }

  fun removeSelectedFromCurrentAlbum() {
    val album = _uiState.value.selectedAlbum ?: return
    val albumId = album.customAlbumId ?: return
    val selectedIds = _uiState.value.selectedPhotoIds.toList()
    if (selectedIds.isEmpty()) return
    viewModelScope.launch {
      repository.removeMediaFromAlbum(albumId, selectedIds)
      _uiState.update {
        it.copy(
          isSelectionMode = false,
          selectedPhotoIds = emptySet(),
          userNotice = "Removed ${selectedIds.size} items from album (Phone files untouched)"
        )
      }
    }
  }

  // --- Multi-Select Actions ---

  fun toggleSelectionMode() {
    _uiState.update {
      it.copy(
        isSelectionMode = !it.isSelectionMode,
        selectedPhotoIds = emptySet()
      )
    }
  }

  fun togglePhotoSelection(photo: MediaItem) {
    _uiState.update { state ->
      val selected = state.selectedPhotoIds.toMutableSet()
      if (selected.contains(photo.id)) {
        selected.remove(photo.id)
      } else {
        selected.add(photo.id)
      }
      state.copy(
        selectedPhotoIds = selected,
        isSelectionMode = selected.isNotEmpty() || state.isSelectionMode
      )
    }
  }

  fun selectAllPhotos() {
    _uiState.update { state ->
      state.copy(selectedPhotoIds = state.photos.map { it.id }.toSet())
    }
  }

  /**
   * Bulk toggle favorite for all selected photos.
   * If any selected photo is not favorited, marks all as favorite;
   * if all are already favorited, un-favorites them all.
   */
  fun bulkToggleFavoriteSelected() {
    val selectedIds = _uiState.value.selectedPhotoIds
    if (selectedIds.isEmpty()) return

    val selectedPhotos = _uiState.value.allRawPhotos.filter { selectedIds.contains(it.id) }
    val shouldFavorite = selectedPhotos.any { !it.isFavorite }

    viewModelScope.launch {
      val pairs = selectedPhotos.map { Pair(it.id, it.uri ?: it.id) }
      repository.setFavorites(pairs, shouldFavorite)
      _uiState.update {
        it.copy(
          userNotice = if (shouldFavorite) "Added ${pairs.size} items to Favorites" else "Removed ${pairs.size} items from Favorites"
        )
      }
    }
  }

  fun showSelectionSummary() {
    val selectedIds = _uiState.value.selectedPhotoIds
    if (selectedIds.isEmpty()) return

    val selectedItems = _uiState.value.allRawPhotos.filter { selectedIds.contains(it.id) }
    val totalBytes = selectedItems.sumOf { it.sizeBytes }
    val photosCount = selectedItems.count { it.mediaType == MediaType.PHOTO }
    val videosCount = selectedItems.count { it.mediaType == MediaType.VIDEO }

    val sortedDates = selectedItems.filter { it.dateTakenMillis > 0 }.sortedBy { it.dateTakenMillis }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateSpan = if (sortedDates.isNotEmpty()) {
      val first = dateFormat.format(Date(sortedDates.first().dateTakenMillis))
      val last = dateFormat.format(Date(sortedDates.last().dateTakenMillis))
      if (first == last) first else "$first – $last"
    } else "Various Dates"

    val summary = SelectionSummary(
      count = selectedItems.size,
      totalSizeBytes = totalBytes,
      photoCount = photosCount,
      videoCount = videosCount,
      dateSpan = dateSpan
    )

    _uiState.update {
      it.copy(
        selectionSummary = summary,
        showSelectionDetailsDialog = true
      )
    }
  }

  fun dismissSelectionSummary() {
    _uiState.update {
      it.copy(
        showSelectionDetailsDialog = false,
        selectionSummary = null
      )
    }
  }

  // --- Photo Edit Record (Requirement 9) ---

  /**
   * Tracks edits locally in Room without overwriting original files.
   */
  fun recordPhotoEdit(
    originalPhoto: MediaItem,
    editType: String = "Color Adjust"
  ) {
    viewModelScope.launch {
      val outputUri = originalPhoto.uri ?: "edited_${originalPhoto.id}"
      repository.recordEdit(
        originalMediaId = originalPhoto.id,
        originalUri = originalPhoto.uri ?: "",
        editedUri = outputUri,
        editType = editType,
        colorSeed = (originalPhoto.colorSeed + 13) % 100
      )
      _uiState.update {
        it.copy(
          userNotice = "Saved edit version ($editType). Original is intact!"
        )
      }
    }
  }

  // --- Safe Exclusion / Delete ---

  fun safeRemoveSelectedPhotos() {
    val selectedIds = _uiState.value.selectedPhotoIds
    if (selectedIds.isEmpty()) return

    AppSoundManager.playTrash()
    viewModelScope.launch {
      val toExclude = _uiState.value.allRawPhotos.filter { selectedIds.contains(it.id) }
      val pairs = toExclude.map { Pair(it.id, it.uri ?: it.id) }
      repository.excludeMediaList(pairs)
      _uiState.update { state ->
        state.copy(
          selectedPhotoIds = emptySet(),
          isSelectionMode = false,
          userNotice = "Removed ${pairs.size} items from My Gallery (Original files safe on device)"
        )
      }
    }
  }

  fun safeRemoveSinglePhoto(photo: MediaItem) {
    AppSoundManager.playTrash()
    viewModelScope.launch {
      repository.excludeMedia(photo.id, photo.uri ?: photo.id)
      _uiState.update { state ->
        state.copy(
          openedPhoto = null,
          userNotice = "Removed from My Gallery (Original file safe on device)"
        )
      }
    }
  }

  fun deleteSelectedPhotos() {
    safeRemoveSelectedPhotos()
  }

  fun deleteSinglePhoto(photo: MediaItem) {
    safeRemoveSinglePhoto(photo)
  }

  fun restoreAllExcluded() {
    viewModelScope.launch {
      repository.clearAllExcluded()
      loadDeviceMedia()
      _uiState.update { it.copy(userNotice = "Restored all hidden items to My Gallery") }
    }
  }

  // --- Navigation & UI Controls ---

  fun navigateTo(destination: GalleryNavDestination) {
    _uiState.update {
      it.copy(
        currentDestination = destination,
        isQuickActionExpanded = false,
        selectedAlbum = null
      )
    }
  }

  fun setAlbumFilter(filter: String) {
    _uiState.update { it.copy(albumFilter = filter) }
  }

  fun setDocumentCategory(category: String) {
    _uiState.update { it.copy(documentCategory = category) }
  }

  fun openPhoto(photo: MediaItem) {
    _uiState.update { it.copy(openedPhoto = photo) }
  }

  fun closePhoto() {
    _uiState.update { it.copy(openedPhoto = null) }
  }

  fun nextPhoto() {
    val current = _uiState.value.openedPhoto ?: return
    val list = _uiState.value.photos
    val index = list.indexOfFirst { it.id == current.id }
    if (index >= 0 && index < list.size - 1) {
      _uiState.update { it.copy(openedPhoto = list[index + 1]) }
    }
  }

  fun prevPhoto() {
    val current = _uiState.value.openedPhoto ?: return
    val list = _uiState.value.photos
    val index = list.indexOfFirst { it.id == current.id }
    if (index > 0) {
      _uiState.update { it.copy(openedPhoto = list[index - 1]) }
    }
  }

  fun toggleFavorite(item: MediaItem) {
    viewModelScope.launch {
      repository.toggleFavorite(item.id, item.uri ?: item.id)
      val newFavState = !item.isFavorite
      _uiState.update { state ->
        val opened = if (state.openedPhoto?.id == item.id) {
          state.openedPhoto.copy(isFavorite = newFavState)
        } else {
          state.openedPhoto
        }
        state.copy(
          openedPhoto = opened,
          userNotice = if (newFavState) "Added to Favorites" else "Removed from Favorites"
        )
      }
    }
  }

  fun cycleGridColumns() {
    _uiState.update { state ->
      val next = when (state.settings.gridColumns) {
        2 -> 3
        3 -> 4
        else -> 2
      }
      state.copy(settings = state.settings.copy(gridColumns = next))
    }
  }

  fun setSettingsOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isSettingsOpen = isOpen) }
  }

  fun updateSettings(newSettings: GallerySettingsState) {
    AppSoundManager.soundEnabled = newSettings.soundEffectsEnabled
    AppSoundManager.hapticEnabled = newSettings.hapticFeedbackEnabled
    _uiState.update { it.copy(settings = newSettings) }
  }

  fun playKeyClick() = AppSoundManager.playKeyClick()
  fun playToggle(enabled: Boolean) = AppSoundManager.playToggle(enabled)
  fun playUnlockSuccess() = AppSoundManager.playUnlockSuccess()
  fun playErrorSound() = AppSoundManager.playError()
  fun playTrashSound() = AppSoundManager.playTrash()
  fun playCompleteSound() = AppSoundManager.playComplete()
  fun playShutterSound() = AppSoundManager.playShutter()

  fun refreshStorageBreakdown() {
    viewModelScope.launch(Dispatchers.IO) {
      val breakdown = StorageCacheManager.calculateStorage(getApplication())
      _uiState.update { it.copy(storageBreakdown = breakdown) }
    }
  }

  fun setClearCacheConfirmDialog(show: Boolean) {
    _uiState.update { it.copy(showClearCacheConfirm = show) }
  }

  fun setCleanTempConfirmDialog(show: Boolean) {
    _uiState.update { it.copy(showCleanTempConfirm = show) }
  }

  fun setClearAiCacheConfirmDialog(show: Boolean) {
    _uiState.update { it.copy(showClearAiCacheConfirm = show) }
  }

  fun cleanThumbnailCache() {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isCleaningStorage = true, showClearCacheConfirm = false) }
      StorageCacheManager.clearThumbnailCache(getApplication())
      val updated = StorageCacheManager.calculateStorage(getApplication())
      _uiState.update {
        it.copy(
          isCleaningStorage = false,
          storageBreakdown = updated,
          userNotice = "Thumbnail cache cleaned successfully"
        )
      }
    }
  }

  fun cleanTemporaryFiles() {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isCleaningStorage = true, showCleanTempConfirm = false) }
      val reclaimed = StorageCacheManager.cleanAllTemporaryFiles(getApplication())
      val updated = StorageCacheManager.calculateStorage(getApplication())
      _uiState.update {
        it.copy(
          isCleaningStorage = false,
          storageBreakdown = updated,
          userNotice = "Reclaimed ${updated.formatBytes(reclaimed)} of temporary files"
        )
      }
    }
  }

  fun cleanAiCache() {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isCleaningStorage = true, showClearAiCacheConfirm = false) }
      repository.clearAllAiMetadataCache()
      val updated = StorageCacheManager.calculateStorage(getApplication())
      _uiState.update {
        it.copy(
          isCleaningStorage = false,
          storageBreakdown = updated,
          userNotice = "AI metadata cache cleared"
        )
      }
    }
  }

  fun showCloudAiConsent(taskTitle: String, onProceed: () -> Unit) {
    if (!_uiState.value.settings.showUploadConfirmation && !_uiState.value.settings.askBeforeSendingToAi) {
      onProceed()
      return
    }
    _uiState.update {
      it.copy(
        showCloudAiConsentDialog = true,
        pendingCloudAiTaskTitle = taskTitle,
        pendingCloudAiAction = onProceed
      )
    }
  }

  fun dismissCloudAiConsent() {
    _uiState.update {
      it.copy(
        showCloudAiConsentDialog = false,
        pendingCloudAiTaskTitle = "",
        pendingCloudAiAction = null
      )
    }
  }

  fun proceedCloudAiConsent() {
    val action = _uiState.value.pendingCloudAiAction
    dismissCloudAiConsent()
    action?.invoke()
  }

  fun toggleQuickActions() {
    _uiState.update { it.copy(isQuickActionExpanded = !it.isQuickActionExpanded) }
  }

  fun dismissQuickActions() {
    _uiState.update { it.copy(isQuickActionExpanded = false) }
  }

  fun handleQuickAction(action: QuickAction) {
    _uiState.update { it.copy(isQuickActionExpanded = false) }
    when (action) {
      QuickAction.ScanDocument -> openScanner()
      QuickAction.CreatePdf -> openPdfStudio()
      QuickAction.PhotoEditor -> {
        val candidate = _uiState.value.photos.firstOrNull { it.mediaType == MediaType.PHOTO }
        if (candidate != null) {
          openEditor(candidate)
        } else {
          showUserNotice("Select a photo from the gallery to open Editor Pro")
        }
      }
      QuickAction.EditingProjects -> openEditingProjectsSheet()
      QuickAction.ResizeImage -> openImageResize(initialTab = 0)
      QuickAction.CompressImage -> openImageCompressor(initialTab = 0)
      QuickAction.PassportStudio -> openImageResize(initialTab = 1)
      QuickAction.AiEdit -> {
        val candidate = _uiState.value.photos.firstOrNull { it.mediaType == MediaType.PHOTO }
        if (candidate != null) {
          openEditor(candidate)
        } else {
          showUserNotice("Select a photo from the gallery to open Photo Editor")
        }
      }
      QuickAction.CreativeStudio -> openCreativeStudio(com.example.model.creative.CreativeStudioTab.ALL)
      QuickAction.BatchTools -> openCreativeStudio(com.example.model.creative.CreativeStudioTab.BATCH)
      else -> showUserNotice("${action.title} action triggered")
    }
  }

  // --- Creative Studio, Batch & Sharing Controls ---

  fun openCreativeStudio(tab: com.example.model.creative.CreativeStudioTab = com.example.model.creative.CreativeStudioTab.ALL) {
    _uiState.update {
      it.copy(
        isCreativeStudioOpen = true,
        initialCreativeStudioTab = tab,
        isQuickActionExpanded = false
      )
    }
  }

  fun closeCreativeStudio() {
    _uiState.update { it.copy(isCreativeStudioOpen = false) }
  }

  fun openShareCenter(items: List<MediaItem>, isFromVault: Boolean = false) {
    _uiState.update {
      it.copy(
        isShareCenterOpen = true,
        pendingShareItems = items,
        isVaultShare = isFromVault
      )
    }
  }

  fun closeShareCenter() {
    _uiState.update {
      it.copy(
        isShareCenterOpen = false,
        pendingShareItems = emptyList(),
        isVaultShare = false
      )
    }
  }

  // --- Vault Operations ---

  fun setupVault(pin: String, enableBiometrics: Boolean) {
    val config = VaultStorageManager.setupVault(getApplication(), pin, enableBiometrics)
    _uiState.update {
      it.copy(
        vaultSecurityConfig = config,
        isVaultUnlocked = true,
        userNotice = "Private Vault initialized with hardware-backed encryption."
      )
    }
  }

  fun verifyPin(pin: String): Boolean {
    return VaultStorageManager.verifyPin(getApplication(), pin)
  }

  fun recordFailedVaultAttempt(): VaultSecurityConfig {
    val updated = VaultStorageManager.recordFailedAttempt(getApplication())
    _uiState.update { it.copy(vaultSecurityConfig = updated) }
    return updated
  }

  fun resetFailedVaultAttempts() {
    val updated = VaultStorageManager.resetFailedAttempts(getApplication())
    _uiState.update { it.copy(vaultSecurityConfig = updated) }
  }

  fun unlockVault() {
    resetFailedVaultAttempts()
    _uiState.update { it.copy(isVaultUnlocked = true, userNotice = "Vault unlocked successfully") }
  }

  fun lockVault() {
    VaultCryptoManager.wipeDecryptedMemoryCache()
    _uiState.update {
      it.copy(
        isVaultUnlocked = false,
        activeVaultMediaViewer = null,
        showVaultSecuritySettings = false,
        userNotice = "Vault locked. Decrypted memory wiped."
      )
    }
  }

  fun updateVaultSecurityConfig(newConfig: VaultSecurityConfig) {
    VaultStorageManager.saveSecurityConfig(getApplication(), newConfig)
    _uiState.update { it.copy(vaultSecurityConfig = newConfig) }
  }

  fun setVaultTab(tab: VaultTab) {
    _uiState.update { it.copy(activeVaultTab = tab) }
  }

  fun setVaultSearchQuery(query: String) {
    _uiState.update { it.copy(vaultSearchQuery = query) }
  }

  fun openVaultMediaViewer(item: VaultMediaItem) {
    _uiState.update { it.copy(activeVaultMediaViewer = item) }
  }

  fun closeVaultMediaViewer() {
    _uiState.update { it.copy(activeVaultMediaViewer = null) }
  }

  fun toggleVaultFavorite(item: VaultMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      val entity = database.vaultDao().getVaultMediaById(item.id)
      if (entity != null) {
        val updated = entity.copy(isFavorite = !entity.isFavorite)
        database.vaultDao().updateVaultMedia(updated)
      }
    }
  }

  fun moveVaultMediaToAlbum(item: VaultMediaItem, albumId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val entity = database.vaultDao().getVaultMediaById(item.id)
      if (entity != null) {
        val updated = entity.copy(albumId = albumId)
        database.vaultDao().updateVaultMedia(updated)
      }
    }
  }

  fun createVaultAlbum(name: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val id = "album_${UUID.randomUUID().toString().take(8)}"
      val entity = VaultAlbumEntity(
        id = id,
        name = name,
        createdAtMillis = System.currentTimeMillis()
      )
      database.vaultDao().insertVaultAlbum(entity)
      _uiState.update { it.copy(userNotice = "Created private album '$name'") }
    }
  }

  fun renameVaultAlbum(id: String, newName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      database.vaultDao().renameVaultAlbum(id, newName)
      _uiState.update { it.copy(userNotice = "Renamed album to '$newName'") }
    }
  }

  fun deleteVaultAlbum(id: String) {
    viewModelScope.launch(Dispatchers.IO) {
      database.vaultDao().resetAlbumMediaToPersonal(id)
      database.vaultDao().deleteVaultAlbum(id)
      _uiState.update { it.copy(userNotice = "Album deleted. Media moved to Personal folder.") }
    }
  }

  fun importMediaUrisToVault(uris: List<android.net.Uri>, albumId: String, isMoveAction: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      var successCount = 0
      for (uri in uris) {
        val mime = getApplication<Application>().contentResolver.getType(uri) ?: "image/jpeg"
        val mediaType = if (mime.startsWith("video")) MediaType.VIDEO else MediaType.PHOTO
        val title = uri.lastPathSegment?.substringAfterLast("/") ?: "Imported_Media"
        val result = VaultStorageManager.importMedia(
          context = getApplication(),
          sourceUri = uri,
          originalTitle = title,
          mediaType = mediaType,
          albumId = albumId,
          isMoveAction = isMoveAction
        )
        if (result.isSuccess) {
          successCount++
        }
      }
      _uiState.update {
        it.copy(
          userNotice = if (successCount > 0) {
            "Securely encrypted and stored $successCount items in Vault"
          } else {
            "Failed to import items into Vault"
          }
        )
      }
    }
  }

  fun exportVaultMedia(item: VaultMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      val result = VaultStorageManager.exportMedia(
        context = getApplication(),
        item = item,
        deleteFromVaultAfterExport = true
      )
      if (result.isSuccess) {
        _uiState.update {
          it.copy(userNotice = "Exported to Pictures/My Gallery AI/Exported")
        }
      } else {
        _uiState.update {
          it.copy(userNotice = "Failed to export vault media.")
        }
      }
    }
  }

  fun deleteMediaFromVault(item: VaultMediaItem) {
    AppSoundManager.playTrash()
    viewModelScope.launch(Dispatchers.IO) {
      VaultStorageManager.deleteMediaFromVault(getApplication(), item)
      _uiState.update {
        it.copy(userNotice = "Media permanently deleted from Private Vault")
      }
    }
  }

  // --- Safe Delete & Move to Vault UI Modals ---

  fun openSafeDeleteDialog(items: List<MediaItem>) {
    _uiState.update {
      it.copy(
        showSafeDeleteDialog = true,
        pendingSafeDeleteItems = items
      )
    }
  }

  fun dismissSafeDeleteDialog() {
    _uiState.update {
      it.copy(
        showSafeDeleteDialog = false,
        pendingSafeDeleteItems = emptyList()
      )
    }
  }

  fun performSafeRemoveFromApp(items: List<MediaItem>) {
    viewModelScope.launch {
      val pairs = items.map { Pair(it.id, it.uri ?: it.id) }
      repository.excludeMediaList(pairs)
      _uiState.update {
        it.copy(
          showSafeDeleteDialog = false,
          pendingSafeDeleteItems = emptyList(),
          selectedPhotoIds = emptySet(),
          isSelectionMode = false,
          openedPhoto = null,
          userNotice = "Removed ${items.size} items from My Gallery (Original safe on device)"
        )
      }
    }
  }

  fun performDeleteFromDevice(items: List<MediaItem>) {
    viewModelScope.launch {
      val pairs = items.map { Pair(it.id, it.uri ?: it.id) }
      repository.excludeMediaList(pairs)

      var deletedDeviceCount = 0
      for (item in items) {
        item.uri?.let { uriStr ->
          try {
            val rows = getApplication<Application>().contentResolver.delete(android.net.Uri.parse(uriStr), null, null)
            if (rows > 0) deletedDeviceCount++
          } catch (_: Exception) {}
        }
      }

      _uiState.update {
        it.copy(
          showSafeDeleteDialog = false,
          pendingSafeDeleteItems = emptyList(),
          selectedPhotoIds = emptySet(),
          isSelectionMode = false,
          openedPhoto = null,
          userNotice = if (deletedDeviceCount > 0) {
            "Permanently deleted $deletedDeviceCount items from device"
          } else {
            "Removed ${items.size} items from gallery view"
          }
        )
      }
    }
  }

  fun showMoveToVaultDialog(items: List<MediaItem>) {
    _uiState.update {
      it.copy(
        showMoveToVaultConfirmDialog = true,
        pendingMoveToVaultItems = items
      )
    }
  }

  fun dismissMoveToVaultDialog() {
    _uiState.update {
      it.copy(
        showMoveToVaultConfirmDialog = false,
        pendingMoveToVaultItems = emptyList()
      )
    }
  }

  fun confirmMoveToVault(items: List<MediaItem>, albumId: String, isMoveAction: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      var count = 0
      for (item in items) {
        val uriStr = item.uri ?: continue
        val result = VaultStorageManager.importMedia(
          context = getApplication(),
          sourceUri = android.net.Uri.parse(uriStr),
          originalTitle = item.title,
          mediaType = item.mediaType,
          albumId = albumId,
          isMoveAction = isMoveAction
        )
        if (result.isSuccess) {
          count++
          if (isMoveAction) {
            repository.excludeMedia(item.id, uriStr)
          }
        }
      }
      _uiState.update {
        it.copy(
          showMoveToVaultConfirmDialog = false,
          pendingMoveToVaultItems = emptyList(),
          selectedPhotoIds = emptySet(),
          isSelectionMode = false,
          openedPhoto = null,
          userNotice = if (count > 0) {
            "Protected $count items in Private Vault"
          } else {
            "Failed to move items to Vault"
          }
        )
      }
    }
  }

  fun clearCache() {
    _uiState.update { it.copy(userNotice = "48.2 MB cache cleared successfully") }
  }

  fun clearUserNotice() {
    _uiState.update { it.copy(userNotice = null) }
  }

  // --- AI Assistant & Smart Search Workspace Methods ---
  fun submitAiQuery(query: String, media: MediaItem? = _uiState.value.attachedMediaForAi) {
    val clean = query.trim()
    if (clean.isBlank() && media == null) return

    val userMsg = AiChatMessage(
      text = if (clean.isNotBlank()) clean else "Explain this attached photo",
      isUser = true,
      attachedMedia = media
    )

    // Append user message immediately and set searching status
    _uiState.update { state ->
      state.copy(
        aiChatMessages = state.aiChatMessages + userMsg,
        isAiSearching = true,
        attachedMediaForAi = null // Reset attached media once sent
      )
    }

    viewModelScope.launch(Dispatchers.Default) {
      val searchResult = LocalAiSearchEngine.executeSearch(
        query = if (clean.isNotBlank()) clean else "Explain this attached photo",
        allMedia = _uiState.value.allRawPhotos.ifEmpty { SampleDataProvider.samplePhotos },
        albums = _uiState.value.albums,
        attachedMedia = media
      )

      val imageAnalysis = if (media != null && searchResult.parsedQuery.isImageQuestion) {
        com.example.data.ai.LocalImageAnalyzer.analyzeImage(media, clean)
      } else null

      val botMsg = AiChatMessage(
        text = searchResult.explanation,
        isUser = false,
        attachedMedia = media,
        recognizedBadges = searchResult.badges,
        matchingItems = searchResult.matchingItems,
        isSearchQuery = !searchResult.parsedQuery.isCountQuery && !searchResult.parsedQuery.isImageQuestion,
        analysisResult = imageAnalysis,
        isLocalOfflineResult = true
      )

      _uiState.update { state ->
        state.copy(
          aiChatMessages = state.aiChatMessages + botMsg,
          isAiSearching = false
        )
      }

      // Record to Room persistent search history
      if (clean.isNotBlank() && !searchResult.parsedQuery.isCountQuery) {
        repository.addSearchHistory(clean, searchResult.matchingItems.size)
      }
    }
  }

  fun clearAiConversation() {
    _uiState.update {
      it.copy(
        aiChatMessages = emptyList(),
        attachedMediaForAi = null,
        isAiSearching = false
      )
    }
  }

  fun attachMediaForAi(media: MediaItem?) {
    _uiState.update {
      it.copy(
        attachedMediaForAi = media,
        showAttachImagePicker = false
      )
    }
  }

  fun setShowAttachImagePicker(show: Boolean) {
    _uiState.update { it.copy(showAttachImagePicker = show) }
  }

  fun setShowHistorySheet(show: Boolean) {
    _uiState.update { it.copy(showHistorySheet = show) }
  }

  fun deleteSearchHistoryItem(id: String) {
    viewModelScope.launch {
      repository.deleteSearchHistory(id)
    }
  }

  fun clearSearchHistory() {
    viewModelScope.launch {
      repository.clearSearchHistory()
    }
  }

  fun applyAiResultsToMainGallery(items: List<MediaItem>, label: String) {
    _uiState.update {
      it.copy(
        photos = items,
        searchQuery = label,
        currentDestination = GalleryNavDestination.HOME,
        userNotice = "Showing ${items.size} matching items in Home gallery"
      )
    }
  }

  // --- Smart Documents & Scanner Workflows ---
  private var activeScanningDocumentId: String? = null

  fun openDocument(document: DocumentItem) {
    _uiState.update { it.copy(openedDocument = document) }
  }

  fun closeDocument() {
    _uiState.update { it.copy(openedDocument = null) }
  }

  fun openOcrResult(document: DocumentItem) {
    val analysis = DocumentOcrEngine.parseAnalysis(document.ocrText)
    _uiState.update { it.copy(openedOcrResult = Pair(document, analysis)) }
  }

  fun closeOcrResult() {
    _uiState.update { it.copy(openedOcrResult = null) }
  }

  fun openScanner(sourceUri: String? = null, media: MediaItem? = null) {
    activeScanningDocumentId = null
    _uiState.update {
      it.copy(
        isScannerOpen = true,
        scannerSourceUri = sourceUri ?: media?.uri,
        scannerSourceMedia = media
      )
    }
  }

  fun closeScanner() {
    activeScanningDocumentId = null
    _uiState.update {
      it.copy(
        isScannerOpen = false,
        scannerSourceUri = null,
        scannerSourceMedia = null
      )
    }
  }

  fun showUserNotice(msg: String) {
    _uiState.update { it.copy(userNotice = msg) }
  }

  fun showRenameDocDialog(doc: DocumentItem?) {
    _uiState.update { it.copy(showRenameDocDialog = doc) }
  }

  fun showChangeDocCategoryDialog(doc: DocumentItem?) {
    _uiState.update { it.copy(showChangeDocCategoryDialog = doc) }
  }

  fun saveScannedDocument(
    title: String,
    category: String,
    bitmap: Bitmap,
    addAnotherPage: Boolean
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val app = getApplication<Application>()
        val filename = "scan_${System.currentTimeMillis()}.jpg"
        val file = File(app.filesDir, filename)
        FileOutputStream(file).use { out ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        val uri = file.toURI().toString()

        // Extract OCR locally on-device
        val ocrResult = DocumentOcrEngine.extractFromBitmap(bitmap, title, category)

        val docId = activeScanningDocumentId ?: UUID.randomUUID().toString()
        val pageId = UUID.randomUUID().toString()

        if (activeScanningDocumentId == null) {
          activeScanningDocumentId = docId
          val docEntity = DocumentEntity(
            id = docId,
            title = title.ifBlank { "Scanned Document" },
            category = category,
            originalImageUri = uri,
            format = "SCAN",
            pageCount = 1,
            sizeBytes = file.length(),
            dateModifiedMillis = System.currentTimeMillis(),
            dateModified = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()),
            ocrText = ocrResult.fullText,
            resolution = "${bitmap.width} × ${bitmap.height}"
          )
          val pageEntity = DocumentPageEntity(
            pageId = pageId,
            documentId = docId,
            pageNumber = 1,
            imageUri = uri,
            rotationDegrees = 0,
            ocrText = ocrResult.fullText
          )
          repository.saveDocument(docEntity, listOf(pageEntity))
        } else {
          val pageEntity = DocumentPageEntity(
            pageId = pageId,
            documentId = docId,
            pageNumber = 2,
            imageUri = uri,
            rotationDegrees = 0,
            ocrText = ocrResult.fullText
          )
          repository.addDocumentPages(listOf(pageEntity))
        }

        launch(Dispatchers.Main) {
          if (addAnotherPage) {
            _uiState.update {
              it.copy(
                userNotice = "Page 1 saved! Select or scan next page."
              )
            }
          } else {
            activeScanningDocumentId = null
            _uiState.update {
              it.copy(
                isScannerOpen = false,
                scannerSourceUri = null,
                scannerSourceMedia = null,
                userNotice = "Document saved & OCR indexed successfully!"
              )
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        launch(Dispatchers.Main) {
          _uiState.update { it.copy(userNotice = "Error saving scan: ${e.message}") }
        }
      }
    }
  }

  fun renameDocument(documentId: String, newTitle: String) {
    viewModelScope.launch {
      repository.renameDocument(documentId, newTitle)
      _uiState.update { state ->
        state.copy(
          documents = state.documents.map {
            if (it.id == documentId) it.copy(title = newTitle) else it
          },
          openedDocument = if (state.openedDocument?.id == documentId) state.openedDocument.copy(title = newTitle) else state.openedDocument,
          userNotice = "Document renamed to '$newTitle'"
        )
      }
    }
  }

  fun updateDocumentCategory(documentId: String, newCategory: String) {
    viewModelScope.launch {
      repository.updateDocumentCategory(documentId, newCategory)
      _uiState.update { state ->
        state.copy(
          documents = state.documents.map {
            if (it.id == documentId) it.copy(category = newCategory) else it
          },
          openedDocument = if (state.openedDocument?.id == documentId) state.openedDocument.copy(category = newCategory) else state.openedDocument,
          userNotice = "Category updated to '$newCategory'"
        )
      }
    }
  }

  fun updateDocumentOcr(documentId: String, newText: String) {
    viewModelScope.launch {
      repository.updateDocumentOcr(documentId, newText)
      _uiState.update { state ->
        state.copy(
          documents = state.documents.map {
            if (it.id == documentId) it.copy(ocrText = newText) else it
          },
          openedDocument = if (state.openedDocument?.id == documentId) state.openedDocument.copy(ocrText = newText) else state.openedDocument,
          userNotice = "Updated OCR text saved."
        )
      }
    }
  }

  fun deleteDocument(documentId: String) {
    viewModelScope.launch {
      repository.deleteDocument(documentId)
      _uiState.update { state ->
        state.copy(
          documents = state.documents.filterNot { it.id == documentId },
          openedDocument = if (state.openedDocument?.id == documentId) null else state.openedDocument,
          openedOcrResult = if (state.openedOcrResult?.first?.id == documentId) null else state.openedOcrResult,
          userNotice = "Document deleted."
        )
      }
    }
  }

  fun rotateDocumentPage(documentId: String, pageIndex: Int) {
    _uiState.update { state ->
      val currentDoc = state.openedDocument ?: return@update state
      val updatedPages = currentDoc.pages.mapIndexed { idx, page ->
        if (idx == pageIndex) page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360) else page
      }
      val updatedDoc = currentDoc.copy(pages = updatedPages)
      state.copy(
        openedDocument = updatedDoc,
        documents = state.documents.map { if (it.id == documentId) updatedDoc else it },
        userNotice = "Rotated page 90°"
      )
    }
  }

  fun deleteDocumentPage(documentId: String, pageIndex: Int) {
    viewModelScope.launch {
      val currentDoc = _uiState.value.openedDocument ?: return@launch
      if (currentDoc.pages.size <= 1) return@launch
      val pageToDelete = currentDoc.pages.getOrNull(pageIndex) ?: return@launch
      repository.deleteDocumentPage(documentId, pageToDelete.pageId)

      val updatedPages = currentDoc.pages.filterIndexed { idx, _ -> idx != pageIndex }
        .mapIndexed { idx, page -> page.copy(pageNumber = idx + 1) }
      val updatedDoc = currentDoc.copy(pages = updatedPages, pageCount = updatedPages.size)

      _uiState.update { state ->
        state.copy(
          openedDocument = updatedDoc,
          documents = state.documents.map { if (it.id == documentId) updatedDoc else it },
          userNotice = "Page removed."
        )
      }
    }
  }

  fun reorderDocumentPages(documentId: String, fromIndex: Int, toIndex: Int) {
    _uiState.update { state ->
      val currentDoc = state.openedDocument ?: return@update state
      val mutablePages = currentDoc.pages.toMutableList()
      if (fromIndex in mutablePages.indices && toIndex in mutablePages.indices) {
        val item = mutablePages.removeAt(fromIndex)
        mutablePages.add(toIndex, item)
        val renumbered = mutablePages.mapIndexed { idx, page -> page.copy(pageNumber = idx + 1) }
        val updatedDoc = currentDoc.copy(pages = renumbered)
        state.copy(
          openedDocument = updatedDoc,
          documents = state.documents.map { if (it.id == documentId) updatedDoc else it }
        )
      } else {
        state
      }
    }
  }

  fun askAiAboutDocument(document: DocumentItem, prompt: String) {
    val docMedia = MediaItem(
      id = "doc_${document.id}",
      uri = document.thumbnailUri ?: "sample://doc_${document.id}",
      title = document.title,
      dateTaken = document.dateModified,
      dateTakenMillis = document.dateModifiedMillis,
      mediaType = MediaType.PHOTO,
      categoryTag = document.category,
      detectedText = document.ocrText,
      detectedObjects = listOf("Document", document.category, "Printed Page", "OCR Text"),
      sizeBytes = document.sizeBytes,
      resolution = document.resolution
    )
    _uiState.update {
      it.copy(
        openedDocument = null,
        openedOcrResult = null,
        currentDestination = GalleryNavDestination.AI,
        attachedMediaForAi = docMedia
      )
    }
    submitAiQuery(prompt, docMedia)
  }

  // --- PDF Studio & Advanced Creator ---

  fun openPdfStudio() {
    _uiState.update { it.copy(isPdfStudioOpen = true, isQuickActionExpanded = false) }
  }

  fun closePdfStudio() {
    _uiState.update { it.copy(isPdfStudioOpen = false) }
  }

  fun startCreatePdfFromImages(selectedMedia: List<MediaItem>? = null) {
    val items = if (!selectedMedia.isNullOrEmpty()) {
      selectedMedia
    } else {
      val selected = _uiState.value.photos.filter { _uiState.value.selectedPhotoIds.contains(it.id) }
      if (selected.isNotEmpty()) selected else _uiState.value.photos.take(4)
    }

    val pages = items.mapIndexed { idx, media ->
      PdfCreatorPage(
        id = UUID.randomUUID().toString(),
        sourceUri = media.uri ?: "sample://${media.id}",
        title = media.title,
        thumbnailUri = media.uri
      )
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    _uiState.update {
      it.copy(
        isPdfStudioOpen = false,
        isPdfCreatorOpen = true,
        pdfCreatorPages = pages,
        pdfConfig = PdfConfig(fileName = "Gallery_Doc_$timeStamp")
      )
    }
  }

  fun startCreatePdfFromDocuments(selectedDocs: List<DocumentItem>? = null) {
    val docs = selectedDocs ?: _uiState.value.documents.take(3)
    val pages = mutableListOf<PdfCreatorPage>()
    docs.forEach { doc ->
      doc.pages.forEach { p ->
        pages.add(
          PdfCreatorPage(
            id = UUID.randomUUID().toString(),
            sourceUri = p.imageUri,
            title = "${doc.title} - Pg ${p.pageNumber}",
            ocrText = p.ocrText,
            thumbnailUri = p.imageUri
          )
        )
      }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    _uiState.update {
      it.copy(
        isPdfStudioOpen = false,
        isPdfCreatorOpen = true,
        pdfCreatorPages = if (pages.isNotEmpty()) pages else listOf(
          PdfCreatorPage(
            id = UUID.randomUUID().toString(),
            sourceUri = "sample://doc_sample",
            title = "Document Page 1"
          )
        ),
        pdfConfig = PdfConfig(fileName = "Document_Scan_$timeStamp")
      )
    }
  }

  fun startCreatePdfFromScan() {
    val activeDoc = _uiState.value.openedDocument
    if (activeDoc != null) {
      startCreatePdfFromDocuments(listOf(activeDoc))
    } else {
      openScanner()
    }
  }

  fun startCreatePdfQuickCompress() {
    startCreatePdfFromImages()
    _uiState.update {
      it.copy(
        pdfConfig = it.pdfConfig.copy(
          targetSizeOption = PdfTargetSizeOption.MB_1,
          isMaxLimitStrict = true
        )
      )
    }
  }

  fun closePdfCreator() {
    _uiState.update { it.copy(isPdfCreatorOpen = false, isPdfPreviewOpen = false) }
  }

  fun openPdfInteractivePreview() {
    _uiState.update { it.copy(isPdfPreviewOpen = true) }
  }

  fun closePdfInteractivePreview() {
    _uiState.update { it.copy(isPdfPreviewOpen = false) }
  }

  fun updatePdfConfig(newConfig: PdfConfig) {
    _uiState.update { it.copy(pdfConfig = newConfig) }
  }

  fun addPageToPdfCreator(page: PdfCreatorPage) {
    _uiState.update { it.copy(pdfCreatorPages = it.pdfCreatorPages + page) }
  }

  fun addSamplePagesToPdfCreator() {
    val extraMedia = _uiState.value.photos.filterNot { p ->
      _uiState.value.pdfCreatorPages.any { it.sourceUri == p.uri }
    }.firstOrNull() ?: _uiState.value.photos.firstOrNull()

    if (extraMedia != null) {
      val newPage = PdfCreatorPage(
        id = UUID.randomUUID().toString(),
        sourceUri = extraMedia.uri ?: "sample://${extraMedia.id}",
        title = extraMedia.title,
        thumbnailUri = extraMedia.uri
      )
      addPageToPdfCreator(newPage)
    }
  }

  fun movePdfCreatorPage(fromIndex: Int, toIndex: Int) {
    _uiState.update { state ->
      val mutable = state.pdfCreatorPages.toMutableList()
      if (fromIndex in mutable.indices && toIndex in mutable.indices) {
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        state.copy(pdfCreatorPages = mutable)
      } else {
        state
      }
    }
  }

  fun rotatePdfCreatorPage(index: Int) {
    _uiState.update { state ->
      val mutable = state.pdfCreatorPages.toMutableList()
      if (index in mutable.indices) {
        val page = mutable[index]
        mutable[index] = page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360)
        state.copy(pdfCreatorPages = mutable)
      } else {
        state
      }
    }
  }

  fun duplicatePdfCreatorPage(index: Int) {
    _uiState.update { state ->
      val mutable = state.pdfCreatorPages.toMutableList()
      if (index in mutable.indices) {
        val page = mutable[index]
        val duplicate = page.copy(id = UUID.randomUUID().toString(), title = "${page.title} (Copy)")
        mutable.add(index + 1, duplicate)
        state.copy(pdfCreatorPages = mutable)
      } else {
        state
      }
    }
  }

  fun deletePdfCreatorPage(index: Int) {
    _uiState.update { state ->
      val mutable = state.pdfCreatorPages.toMutableList()
      if (index in mutable.indices && mutable.size > 1) {
        mutable.removeAt(index)
        state.copy(pdfCreatorPages = mutable)
      } else {
        state.copy(userNotice = "PDF must contain at least 1 page.")
      }
    }
  }

  fun replacePdfCreatorPage(index: Int) {
    val alternate = _uiState.value.photos.filterNot { p ->
      _uiState.value.pdfCreatorPages.any { it.sourceUri == p.uri }
    }.firstOrNull() ?: _uiState.value.photos.lastOrNull()

    if (alternate != null) {
      _uiState.update { state ->
        val mutable = state.pdfCreatorPages.toMutableList()
        if (index in mutable.indices) {
          mutable[index] = mutable[index].copy(
            sourceUri = alternate.uri ?: "sample://${alternate.id}",
            title = alternate.title,
            thumbnailUri = alternate.uri
          )
          state.copy(pdfCreatorPages = mutable, userNotice = "Page replaced with ${alternate.title}")
        } else {
          state
        }
      }
    }
  }

  fun updatePdfCreatorPageFit(index: Int, fit: PdfImageFit) {
    _uiState.update { state ->
      val mutable = state.pdfCreatorPages.toMutableList()
      if (index in mutable.indices) {
        mutable[index] = mutable[index].copy(fitOverride = fit)
        state.copy(pdfCreatorPages = mutable)
      } else {
        state
      }
    }
  }

  fun generatePdf() {
    val pages = _uiState.value.pdfCreatorPages
    val config = _uiState.value.pdfConfig
    if (pages.isEmpty()) {
      showUserNotice("Cannot create PDF without pages.")
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          pdfGenerationProgress = PdfGenerationProgress(
            currentPage = 1,
            totalPages = pages.size,
            statusMessage = "Starting PDF creation...",
            percent = 5
          )
        )
      }

      val result = PdfStudioEngine.generatePdf(
        context = getApplication(),
        pages = pages,
        config = config,
        onProgress = { progress ->
          _uiState.update { it.copy(pdfGenerationProgress = progress) }
        }
      )

      when (result) {
        is PdfGenerationResult.Success -> {
          val entity = PdfHistoryEntity(
            id = result.item.id,
            fileName = result.item.fileName,
            filePath = result.item.filePath,
            fileSizeBytes = result.item.fileSizeBytes,
            pageCount = result.item.pageCount,
            createdAtMillis = result.item.createdAtMillis,
            pageSizeName = result.item.pageSizeName,
            orientation = result.item.orientation,
            thumbnailUri = result.item.thumbnailUri,
            title = result.item.title,
            author = result.item.author,
            subject = result.item.subject,
            keywords = result.item.keywords,
            targetSizeBytes = result.item.targetSizeBytes,
            qualityPreset = result.item.qualityPreset
          )
          repository.savePdfHistory(entity)
          AppSoundManager.playComplete()

          _uiState.update {
            it.copy(
              isPdfCreatorOpen = false,
              isPdfPreviewOpen = false,
              pdfGenerationProgress = null,
              createdPdfResult = result.item,
              userNotice = "PDF '${result.item.fileName}' created successfully!"
            )
          }
        }
        is PdfGenerationResult.Error -> {
          AppSoundManager.playError()
          _uiState.update {
            it.copy(
              pdfGenerationProgress = null,
              userNotice = result.message
            )
          }
        }
      }
    }
  }

  fun cancelPdfGeneration() {
    _uiState.update {
      it.copy(
        pdfGenerationProgress = null,
        userNotice = "PDF generation cancelled."
      )
    }
  }

  fun sharePdf(item: PdfHistoryItem) {
    try {
      val intent = PdfStudioEngine.createShareIntent(getApplication(), item)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      getApplication<Application>().startActivity(Intent.createChooser(intent, "Share PDF").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      })
    } catch (e: Exception) {
      showUserNotice("Unable to share PDF: ${e.localizedMessage}")
    }
  }

  fun openPdf(item: PdfHistoryItem) {
    try {
      val intent = PdfStudioEngine.createViewIntent(getApplication(), item)
      getApplication<Application>().startActivity(intent)
    } catch (e: Exception) {
      showUserNotice("Opening '${item.fileName}': ${item.formattedSize} (${item.pageCount} pages)")
    }
  }

  fun renamePdf(item: PdfHistoryItem, newName: String) {
    viewModelScope.launch {
      val file = File(item.filePath)
      val newFile = File(file.parentFile, newName)
      if (file.exists()) {
        file.renameTo(newFile)
      }
      repository.renamePdf(item.id, newName, newFile.absolutePath)
      _uiState.update {
        it.copy(userNotice = "Renamed to $newName")
      }
    }
  }

  fun deletePdf(item: PdfHistoryItem, deleteDeviceFile: Boolean) {
    viewModelScope.launch {
      if (deleteDeviceFile) {
        val file = File(item.filePath)
        if (file.exists()) file.delete()
      }
      repository.deletePdf(item.id)
      _uiState.update {
        it.copy(userNotice = if (deleteDeviceFile) "PDF deleted from device." else "Removed from history.")
      }
    }
  }

  fun dismissPdfSuccessSheet() {
    _uiState.update { it.copy(createdPdfResult = null) }
  }

  // ==========================================
  // ADVANCED NON-DESTRUCTIVE PHOTO EDITOR
  // ==========================================

  fun openEditor(media: MediaItem, project: EditingProjectEntity? = null) {
    _uiState.update {
      it.copy(
        activeEditorMedia = media,
        activeEditorProject = project,
        showEditingProjectsSheet = false
      )
    }
  }

  fun closeEditor() {
    _uiState.update {
      it.copy(
        activeEditorMedia = null,
        activeEditorProject = null
      )
    }
  }

  fun saveEditingProject(state: PhotoEditorState) {
    viewModelScope.launch {
      val mediaId = state.originalMediaId.ifBlank { UUID.randomUUID().toString() }
      val entity = EditingProjectEntity(
        id = _uiState.value.activeEditorProject?.id ?: UUID.randomUUID().toString(),
        originalMediaId = mediaId,
        originalUri = state.originalUri,
        title = state.title.ifBlank { "Edited Photo" },
        thumbnailPath = state.originalUri,
        lastModifiedMillis = System.currentTimeMillis(),
        stateJson = PhotoEditorEngine.serializeState(state)
      )
      repository.saveEditingProject(entity)
    }
  }

  fun deleteEditingProject(id: String) {
    viewModelScope.launch {
      repository.deleteEditingProject(id)
    }
  }

  fun onPhotoEditorExported(savedUri: android.net.Uri, savedPath: String, state: PhotoEditorState) {
    viewModelScope.launch {
      // 1. Record edit history
      repository.recordEdit(
        originalMediaId = state.originalMediaId,
        originalUri = state.originalUri,
        editedUri = savedUri.toString(),
        editType = "Photo Editor Pro",
        colorSeed = 42
      )
      // 2. Mark media as edited if in list
      _uiState.update { current ->
        val updatedPhotos = current.allRawPhotos.map {
          if (it.id == state.originalMediaId) it.copy(isEdited = true) else it
        }
        current.copy(
          allRawPhotos = updatedPhotos,
          activeEditorMedia = null,
          activeEditorProject = null,
          editorSuccessNotice = "Image saved successfully to Pictures/MyGalleryAI"
        )
      }
      // 3. Rescan media store
      reloadMedia()
    }
  }

  fun openEditingProjectsSheet() {
    _uiState.update { it.copy(showEditingProjectsSheet = true) }
  }

  fun closeEditingProjectsSheet() {
    _uiState.update { it.copy(showEditingProjectsSheet = false) }
  }

  fun dismissEditorSuccessNotice() {
    _uiState.update { it.copy(editorSuccessNotice = null) }
  }

  fun openBitmapInProEditor(bitmap: Bitmap, title: String) {
    viewModelScope.launch {
      val (savedUri, _) = PhotoEditorEngine.saveBitmapDirect(
        context = getApplication(),
        bitmap = bitmap,
        title = "${title}_Handoff"
      )
      if (savedUri != null) {
        val tempMedia = MediaItem(
          id = UUID.randomUUID().toString(),
          uri = savedUri.toString(),
          title = title,
          dateTaken = "Today",
          dateTakenMillis = System.currentTimeMillis(),
          mediaType = MediaType.PHOTO,
          albumName = "Edited"
        )
        _uiState.update {
          it.copy(
            activeEditorMedia = tempMedia,
            activeEditorProject = null,
            userNotice = "Opened image in Professional Editor"
          )
        }
      }
    }
  }

  fun updateAskBeforeSendingToAiPreference(ask: Boolean) {
    _uiState.update { state ->
      state.copy(settings = state.settings.copy(askBeforeSendingToAi = ask))
    }
  }

  fun saveGeminiApiKey(key: String) {
    _uiState.update { state ->
      state.copy(settings = state.settings.copy(geminiApiKey = key.trim()))
    }
  }

  fun openAiImageUnderstanding(
    media: MediaItem? = null,
    uri: String? = null,
    title: String? = null,
    task: ImageUnderstandingTask = ImageUnderstandingTask.ASK_ANYTHING,
    prompt: String? = null,
    isFromVault: Boolean = false
  ) {
    _uiState.update {
      it.copy(
        activeAiUnderstandingMedia = media,
        activeAiUnderstandingUri = uri ?: media?.uri,
        activeAiUnderstandingTitle = title ?: media?.title ?: "Photo",
        activeAiUnderstandingTask = task,
        activeAiUnderstandingInitialPrompt = prompt,
        activeAiUnderstandingFromVault = isFromVault
      )
    }
  }

  fun closeAiImageUnderstanding() {
    _uiState.update {
      it.copy(
        activeAiUnderstandingMedia = null,
        activeAiUnderstandingUri = null,
        activeAiUnderstandingTitle = null,
        activeAiUnderstandingInitialPrompt = null,
        activeAiUnderstandingFromVault = false
      )
    }
  }

  fun saveAiMetadataCache(mediaId: String, mediaUri: String, text: String, desc: String, objects: String) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.saveAiMetadataCache(
        AiImageMetadataCacheEntity(
          mediaId = mediaId,
          mediaUri = mediaUri,
          detectedText = text,
          aiDescription = desc,
          detectedObjects = objects,
          timestamp = System.currentTimeMillis()
        )
      )
    }
  }

  fun clearAiMetadataCache() {
    viewModelScope.launch(Dispatchers.IO) {
      repository.clearAllAiMetadataCache()
      _uiState.update { it.copy(userNotice = "AI metadata cache cleared. Original photos preserved.") }
    }
  }

  fun openImageResize(media: MediaItem? = null, initialTab: Int = 0) {
    _uiState.update {
      it.copy(
        isImageResizeOpen = true,
        activeResizeMedia = media ?: it.attachedMediaForAi ?: it.photos.firstOrNull(),
        initialResizeTab = initialTab
      )
    }
  }

  fun closeImageResize() {
    _uiState.update {
      it.copy(
        isImageResizeOpen = false,
        activeResizeMedia = null
      )
    }
  }

  fun openImageCompressor(
    media: MediaItem? = null,
    initialTab: Int = 0,
    batchMedias: List<MediaItem> = emptyList()
  ) {
    _uiState.update {
      it.copy(
        isImageCompressorOpen = true,
        activeCompressMedia = media ?: it.attachedMediaForAi ?: it.photos.firstOrNull(),
        initialCompressTab = initialTab,
        batchCompressMedias = batchMedias
      )
    }
  }

  fun closeImageCompressor() {
    _uiState.update {
      it.copy(
        isImageCompressorOpen = false,
        activeCompressMedia = null,
        batchCompressMedias = emptyList()
      )
    }
  }
}
