package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GalleryTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.DocumentCategories
import com.example.model.MediaTypeFilter
import com.example.ui.components.AddToAlbumBottomSheet
import com.example.ui.components.AlbumCoverChooserSheet
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.DeleteAlbumDialog
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.GalleryNavDestination
import com.example.ui.components.PremiumBottomNav
import com.example.ui.components.QuickActionMenu
import com.example.ui.components.QuickActionOverlay
import com.example.ui.components.RenameAlbumDialog
import com.example.ui.components.SelectionSummaryDialog
import com.example.ui.screens.AiScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.document.DocumentScannerScreen
import com.example.ui.screens.document.DocumentViewerScreen
import com.example.ui.screens.document.OcrResultScreen
import com.example.ui.screens.pdf.PdfCreatorScreen
import com.example.ui.screens.pdf.PdfInteractivePreviewScreen
import com.example.ui.screens.pdf.PdfProgressDialog
import com.example.ui.screens.pdf.PdfStudioHomeScreen
import com.example.ui.screens.pdf.PdfSuccessSheet
import com.example.ui.screens.editor.PhotoEditorScreen
import com.example.ui.screens.editor.EditingProjectsSheet
import com.example.ui.screens.ai.editor.AiPhotoEditorScreen
import com.example.ui.screens.ai.understanding.AiImageUnderstandingScreen
import com.example.model.ai.understanding.ImageUnderstandingTask
import com.example.ui.screens.resize.ImageResizeScreen
import com.example.ui.screens.compression.ImageCompressorScreen
import com.example.ui.screens.creative.CreativeStudioScreen
import com.example.ui.screens.creative.components.SharePreviewDialog
import com.example.utils.creative.NativeShareHelper
import kotlinx.coroutines.launch
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode

@Composable
fun GalleryApp(
  viewModel: GalleryViewModel = viewModel()
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Theme resolution
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (uiState.settings.themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
  }

  // Permission Launcher (Android 14+, Android 13, and legacy)
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val hasImages = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
      permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
    val isPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true && !hasImages
    } else {
      false
    }
    viewModel.onPermissionResult(granted = hasImages, partialGranted = isPartial)
  }

  // Request permissions helper
  val requestStoragePermissions = {
    val perms = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
      )
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
      )
      else -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
    }
    permissionLauncher.launch(perms)
  }

  // Handle user notices
  LaunchedEffect(uiState.userNotice) {
    uiState.userNotice?.let { notice ->
      snackbarHostState.showSnackbar(
        message = notice,
        duration = SnackbarDuration.Short
      )
      viewModel.clearUserNotice()
    }
  }

  // Handle editor export notice
  LaunchedEffect(uiState.editorSuccessNotice) {
    uiState.editorSuccessNotice?.let { notice ->
      snackbarHostState.showSnackbar(
        message = notice,
        duration = SnackbarDuration.Short
      )
      viewModel.dismissEditorSuccessNotice()
    }
  }

  MyApplicationTheme(darkTheme = darkTheme) {
    // Show Permission Screen if permissions are not granted and user hasn't chosen sample data
    if (!uiState.hasPermission) {
      PermissionScreen(
        onRequestPermission = { requestStoragePermissions() },
        onExploreWithSampleData = { viewModel.setExploreWithSampleData() },
        isPermanentlyDenied = uiState.isPermissionPermanentlyDenied,
        isPartialAccessGranted = uiState.isPartialAccessGranted
      )
      return@MyApplicationTheme
    }

    // Back navigation handlers
    if (uiState.isShareCenterOpen) {
      BackHandler { viewModel.closeShareCenter() }
    } else if (uiState.isCreativeStudioOpen) {
      BackHandler { viewModel.closeCreativeStudio() }
    } else if (uiState.isImageCompressorOpen) {
      BackHandler { viewModel.closeImageCompressor() }
    } else if (uiState.isImageResizeOpen) {
      BackHandler { viewModel.closeImageResize() }
    } else if (uiState.activeAiEditorMedia != null || uiState.activeAiEditorUri != null) {
      BackHandler { viewModel.closeAiPhotoEditor() }
    } else if (uiState.activeEditorMedia != null) {
      BackHandler { viewModel.closeEditor() }
    } else if (uiState.isPdfPreviewOpen) {
      BackHandler { viewModel.closePdfInteractivePreview() }
    } else if (uiState.isPdfCreatorOpen) {
      BackHandler { viewModel.closePdfCreator() }
    } else if (uiState.isPdfStudioOpen) {
      BackHandler { viewModel.closePdfStudio() }
    } else if (uiState.isScannerOpen) {
      BackHandler { viewModel.closeScanner() }
    } else if (uiState.openedOcrResult != null) {
      BackHandler { viewModel.closeOcrResult() }
    } else if (uiState.openedDocument != null) {
      BackHandler { viewModel.closeDocument() }
    } else if (uiState.openedPhoto != null) {
      BackHandler { viewModel.closePhoto() }
    } else if (uiState.isSettingsOpen) {
      BackHandler { viewModel.setSettingsOpen(false) }
    } else if (uiState.selectedAlbum != null) {
      BackHandler { viewModel.closeAlbum() }
    } else if (uiState.isSelectionMode) {
      BackHandler { viewModel.toggleSelectionMode() }
    } else if (uiState.currentDestination != GalleryNavDestination.HOME) {
      BackHandler { viewModel.navigateTo(GalleryNavDestination.HOME) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        bottomBar = {
          if (uiState.openedPhoto == null && !uiState.isSettingsOpen &&
            !uiState.isScannerOpen && uiState.openedDocument == null && uiState.openedOcrResult == null &&
            !uiState.isPdfStudioOpen && !uiState.isPdfCreatorOpen && !uiState.isPdfPreviewOpen &&
            uiState.activeEditorMedia == null &&
            uiState.activeAiEditorMedia == null && uiState.activeAiEditorUri == null &&
            uiState.activeAiUnderstandingMedia == null && uiState.activeAiUnderstandingUri == null &&
            !uiState.isImageResizeOpen && !uiState.isImageCompressorOpen &&
            !uiState.isCreativeStudioOpen && !uiState.isShareCenterOpen
          ) {
            PremiumBottomNav(
              currentDestination = uiState.currentDestination,
              onDestinationSelected = { destination ->
                viewModel.navigateTo(destination)
              }
            )
          }
        },
        snackbarHost = {
          SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(bottom = 76.dp)
          )
        }
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = if (uiState.openedPhoto == null && !uiState.isSettingsOpen) 0.dp else 0.dp)
        ) {
          // Tab Content switcher
          AnimatedContent(
            targetState = uiState.currentDestination,
            transitionSpec = {
              fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "tab_navigation"
          ) { destination ->
            when (destination) {
              GalleryNavDestination.HOME -> {
                HomeScreen(
                  photos = uiState.photos,
                  recentPhotos = uiState.recentPhotos,
                  timelineSections = uiState.timelineSections,
                  smartClusters = uiState.smartClusters,
                  selectedIds = uiState.selectedPhotoIds,
                  isSelectionMode = uiState.isSelectionMode,
                  gridColumns = uiState.settings.gridColumns,
                  searchQuery = uiState.searchQuery,
                  filterState = uiState.filterState,
                  sortOption = uiState.sortOption,
                  onSearchQueryChange = { viewModel.setSearchQuery(it) },
                  onOpenFilters = { viewModel.setFilterSheetOpen(true) },
                  onSortOptionSelected = { viewModel.setSortOption(it) },
                  onQuickFilterSelected = { tab ->
                    when (tab) {
                      "All Media" -> viewModel.clearFilters()
                      "Photos" -> viewModel.updateFilterState { it.copy(mediaType = MediaTypeFilter.PHOTOS) }
                      "Videos" -> viewModel.updateFilterState { it.copy(mediaType = MediaTypeFilter.VIDEOS) }
                      "Favorites" -> viewModel.updateFilterState { it.copy(onlyFavorites = !it.onlyFavorites) }
                      "Edited" -> viewModel.updateFilterState { it.copy(onlyEdited = !it.onlyEdited) }
                      "Screenshots" -> viewModel.updateFilterState { it.copy(onlyScreenshots = !it.onlyScreenshots) }
                    }
                  },
                  onResetFilters = { viewModel.clearFilters() },
                  onPhotoClick = { viewModel.openPhoto(it) },
                  onPhotoLongClick = { viewModel.togglePhotoSelection(it) },
                  onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                  onSelectAll = { viewModel.selectAllPhotos() },
                  onDeleteSelected = {
                    val selectedPhotos = uiState.photos.filter { uiState.selectedPhotoIds.contains(it.id) }
                    if (selectedPhotos.isNotEmpty()) {
                      viewModel.openSafeDeleteDialog(selectedPhotos)
                    }
                  },
                  onShareSelected = {
                    val selectedPhotos = uiState.photos.filter { uiState.selectedPhotoIds.contains(it.id) }
                    if (selectedPhotos.isNotEmpty()) {
                      viewModel.openShareCenter(selectedPhotos)
                    }
                  },
                  onBulkFavoriteSelected = { viewModel.bulkToggleFavoriteSelected() },
                  onAddToAlbumSelected = { viewModel.showAddToAlbumDialog(true) },
                  onCreatePdfSelected = { viewModel.startCreatePdfFromImages() },
                  onCompressSelected = {
                    val selectedPhotos = uiState.photos.filter { uiState.selectedPhotoIds.contains(it.id) }
                    if (selectedPhotos.isNotEmpty()) {
                      viewModel.toggleSelectionMode()
                      if (selectedPhotos.size == 1) {
                        viewModel.openImageCompressor(media = selectedPhotos.first(), initialTab = 0)
                      } else {
                        viewModel.openImageCompressor(initialTab = 1, batchMedias = selectedPhotos)
                      }
                    }
                  },
                  onEditSelected = {
                    val singleSelected = uiState.photos.find { uiState.selectedPhotoIds.contains(it.id) }
                    if (singleSelected != null) {
                      viewModel.toggleSelectionMode()
                      viewModel.openEditor(singleSelected)
                    }
                  },
                  onAiEditSelected = {
                    val singleSelected = uiState.photos.find { uiState.selectedPhotoIds.contains(it.id) }
                    if (singleSelected != null) {
                      viewModel.toggleSelectionMode()
                      viewModel.openAiPhotoEditor(singleSelected)
                    }
                  },
                  onShowSelectionDetails = { viewModel.showSelectionSummary() },
                  onMoveToVaultSelected = {
                    val selectedPhotos = uiState.photos.filter { uiState.selectedPhotoIds.contains(it.id) }
                    if (selectedPhotos.isNotEmpty()) {
                      viewModel.showMoveToVaultDialog(selectedPhotos)
                    }
                  },
                  onRemoveFromGallerySelected = { viewModel.safeRemoveSelectedPhotos() },
                  onGridColumnsCycle = { viewModel.cycleGridColumns() },
                  onOpenSettings = { viewModel.setSettingsOpen(true) },
                  onOpenAiSearch = { viewModel.navigateTo(GalleryNavDestination.AI) },
                  onNavigateToVault = { viewModel.navigateTo(GalleryNavDestination.VAULT) },
                  onNavigateToDocuments = { viewModel.navigateTo(GalleryNavDestination.DOCUMENTS) },
                  onNavigateToCreativeStudio = { viewModel.openCreativeStudio() }
                )
              }

              GalleryNavDestination.ALBUMS -> {
                AlbumsScreen(
                  albums = uiState.albums,
                  smartClusters = uiState.smartClusters,
                  selectedFilter = uiState.albumFilter,
                  onFilterSelected = { viewModel.setAlbumFilter(it) },
                  onAlbumClick = {
                    viewModel.openAlbum(it)
                    viewModel.navigateTo(GalleryNavDestination.HOME)
                  },
                  onCreateAlbumClick = {
                    viewModel.showCreateAlbumDialog(true)
                  },
                  onRenameAlbumClick = { album ->
                    viewModel.showRenameAlbumDialog(album)
                  },
                  onChangeCoverClick = { album ->
                    viewModel.showCoverChooser(album)
                  },
                  onDeleteAlbumClick = { album ->
                    viewModel.showDeleteAlbumDialog(album)
                  }
                )
              }

              GalleryNavDestination.AI -> {
                AiScreen(
                  chatMessages = uiState.aiChatMessages,
                  isAiSearching = uiState.isAiSearching,
                  suggestions = uiState.aiSuggestions,
                  recentActions = uiState.recentAiActions,
                  searchHistory = uiState.aiSearchHistory,
                  attachedMedia = uiState.attachedMediaForAi,
                  allPhotos = uiState.allRawPhotos.ifEmpty { uiState.photos },
                  onSubmitQuery = { query, media -> viewModel.submitAiQuery(query, media) },
                  onClearConversation = { viewModel.clearAiConversation() },
                  onAttachMedia = { media -> viewModel.attachMediaForAi(media) },
                  onRemoveAttachedMedia = { viewModel.attachMediaForAi(null) },
                  onDeleteHistoryItem = { id -> viewModel.deleteSearchHistoryItem(id) },
                  onClearHistory = { viewModel.clearSearchHistory() },
                  onViewInMainGallery = { items, label -> viewModel.applyAiResultsToMainGallery(items, label) },
                  onPhotoClick = { photo -> viewModel.openPhoto(photo) },
                  onOpenAiEditor = {
                    val candidate = uiState.attachedMediaForAi ?: uiState.photos.firstOrNull()
                    viewModel.openAiPhotoEditor(candidate)
                  },
                  onOpenAiEditorWithMedia = { item ->
                    viewModel.openAiPhotoEditor(item)
                  },
                  onOpenVisualAnalysis = {
                    viewModel.navigateTo(GalleryNavDestination.HOME)
                  },
                  onOpenAiUnderstanding = { item, task ->
                    val candidate = item ?: uiState.attachedMediaForAi ?: uiState.photos.firstOrNull()
                    viewModel.openAiImageUnderstanding(media = candidate, task = task)
                  },
                  onOpenImageResize = { item, tab ->
                    val candidate = item ?: uiState.attachedMediaForAi ?: uiState.photos.firstOrNull()
                    viewModel.openImageResize(media = candidate, initialTab = tab)
                  },
                  onOpenCompressor = { item, tab ->
                    val candidate = item ?: uiState.attachedMediaForAi ?: uiState.photos.firstOrNull()
                    viewModel.openImageCompressor(media = candidate, initialTab = tab)
                  }
                )
              }

              GalleryNavDestination.DOCUMENTS -> {
                DocumentsScreen(
                  documents = uiState.documents,
                  selectedCategory = uiState.documentCategory,
                  onCategorySelected = { viewModel.setDocumentCategory(it) },
                  onCreatePdfClick = {
                    viewModel.openPdfStudio()
                  },
                  onScanDocClick = {
                    viewModel.openScanner()
                  },
                  onImportPhotoClick = {
                    viewModel.openScanner()
                  },
                  onResizeClick = {
                    viewModel.openImageResize()
                  },
                  onCompressClick = {
                    viewModel.startCreatePdfQuickCompress()
                  },
                  onDocumentClick = { doc ->
                    viewModel.openDocument(doc)
                  },
                  onOcrClick = { doc ->
                    viewModel.openOcrResult(doc)
                  },
                  onAskAiClick = { doc ->
                    viewModel.askAiAboutDocument(doc, "इस document में क्या लिखा है?")
                  },
                  onRenameClick = { doc ->
                    viewModel.showRenameDocDialog(doc)
                  },
                  onChangeCategoryClick = { doc ->
                    viewModel.showChangeDocCategoryDialog(doc)
                  },
                  onDeleteClick = { doc ->
                    viewModel.deleteDocument(doc.id)
                  }
                )
              }

              GalleryNavDestination.VAULT -> {
                VaultScreen(
                  isUnlocked = uiState.isVaultUnlocked,
                  securityConfig = uiState.vaultSecurityConfig,
                  vaultMediaList = uiState.vaultMediaList,
                  vaultAlbums = uiState.vaultAlbums,
                  activeTab = uiState.activeVaultTab,
                  searchQuery = uiState.vaultSearchQuery,
                  onTabSelected = { viewModel.setVaultTab(it) },
                  onSearchQueryChange = { viewModel.setVaultSearchQuery(it) },
                  onUnlockSuccess = { viewModel.unlockVault() },
                  onLockVault = { viewModel.lockVault() },
                  onSetupCompleted = { pin, enableBio -> viewModel.setupVault(pin, enableBio) },
                  onUpdateSecurityConfig = { viewModel.updateVaultSecurityConfig(it) },
                  onVerifyPin = { viewModel.verifyPin(it) },
                  onRecordFailedAttempt = { viewModel.recordFailedVaultAttempt() },
                  onResetFailedAttempts = { viewModel.resetFailedVaultAttempts() },
                  onImportMediaUris = { uris, albumId, isMove -> viewModel.importMediaUrisToVault(uris, albumId, isMove) },
                  onExportMedia = { viewModel.exportVaultMedia(it) },
                  onDeleteMediaFromVault = { viewModel.deleteMediaFromVault(it) },
                  onToggleFavorite = { viewModel.toggleVaultFavorite(it) },
                  onMoveMediaToAlbum = { item, albumId -> viewModel.moveVaultMediaToAlbum(item, albumId) },
                  onCreateAlbum = { viewModel.createVaultAlbum(it) },
                  onRenameAlbum = { id, name -> viewModel.renameVaultAlbum(id, name) },
                  onDeleteAlbum = { viewModel.deleteVaultAlbum(it) },
                  onOpenInAiEdit = { bitmap ->
                    viewModel.handOffAiImageToProEditor(bitmap, "Vault_Edit")
                  },
                  onOpenInAiChat = { bitmap ->
                    viewModel.openAiImageUnderstanding(prompt = "Analyze this private vault photo", isFromVault = true)
                  },
                  onOpenInOcr = { bitmap ->
                    viewModel.openScanner()
                  }
                )
              }
            }
          }

          // Floating Quick Action System (on Home and Albums tabs)
          if (uiState.openedPhoto == null && !uiState.isSettingsOpen &&
            !uiState.isScannerOpen && uiState.openedDocument == null && uiState.openedOcrResult == null &&
            !uiState.isPdfStudioOpen && !uiState.isPdfCreatorOpen && !uiState.isPdfPreviewOpen &&
            uiState.activeEditorMedia == null &&
            uiState.activeAiEditorMedia == null && uiState.activeAiEditorUri == null &&
            uiState.activeAiUnderstandingMedia == null && uiState.activeAiUnderstandingUri == null &&
            !uiState.isImageResizeOpen && !uiState.isImageCompressorOpen &&
            !uiState.isCreativeStudioOpen && !uiState.isShareCenterOpen &&
            uiState.currentDestination in listOf(GalleryNavDestination.HOME, GalleryNavDestination.ALBUMS)
          ) {
            QuickActionMenu(
              isExpanded = uiState.isQuickActionExpanded,
              onToggle = { viewModel.toggleQuickActions() },
              onActionClick = { viewModel.handleQuickAction(it) },
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
            )
          }
        }
      }

      // Quick action backdrop scrim
      QuickActionOverlay(
        isExpanded = uiState.isQuickActionExpanded,
        onDismiss = { viewModel.dismissQuickActions() },
        onActionClick = { viewModel.handleQuickAction(it) }
      )

      // Settings Screen Overlay
      AnimatedVisibility(
        visible = uiState.isSettingsOpen,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
      ) {
        SettingsScreen(
          settings = uiState.settings,
          onUpdateSettings = { viewModel.updateSettings(it) },
          onBack = { viewModel.setSettingsOpen(false) },
          onClearCache = { viewModel.cleanThumbnailCache() },
          onCleanTemporaryFiles = { viewModel.cleanTemporaryFiles() },
          onRestoreExcluded = { viewModel.restoreAllExcluded() },
          onClearAiMetadata = { viewModel.cleanAiCache() },
          storageBreakdown = uiState.storageBreakdown
        )
      }

      // Full-screen Photo Viewer Overlay
      AnimatedVisibility(
        visible = uiState.openedPhoto != null,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { it / 4 })
      ) {
        uiState.openedPhoto?.let { photo ->
          PhotoViewerScreen(
            photo = photo,
            onClose = { viewModel.closePhoto() },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onShare = { item ->
              viewModel.openShareCenter(listOf(item))
            },
            onEdit = { media, editType ->
              viewModel.recordPhotoEdit(media, editType)
            },
            onAiAction = { item ->
              viewModel.closePhoto()
              viewModel.navigateTo(GalleryNavDestination.AI)
              viewModel.attachMediaForAi(item)
            },
            onScanAsDoc = { item ->
              viewModel.closePhoto()
              viewModel.openScanner(media = item)
            },
            onCreatePdf = { item ->
              viewModel.closePhoto()
              viewModel.startCreatePdfFromImages(listOf(item))
            },
            onOpenPhotoEditor = { item ->
              viewModel.closePhoto()
              viewModel.openEditor(item)
            },
            onOpenAiPhotoEditor = { item ->
              viewModel.closePhoto()
              viewModel.openAiPhotoEditor(item)
            },
            onOpenAiUnderstanding = { item, task ->
              viewModel.closePhoto()
              viewModel.openAiImageUnderstanding(media = item, task = task)
            },
            onOpenImageResize = { item ->
              viewModel.closePhoto()
              viewModel.openImageResize(media = item, initialTab = 0)
            },
            onOpenPassportStudio = { item ->
              viewModel.closePhoto()
              viewModel.openImageResize(media = item, initialTab = 1)
            },
            onOpenCompressor = { item ->
              viewModel.closePhoto()
              viewModel.openImageCompressor(media = item, initialTab = 0)
            },
            onOpenFormatConverter = { item ->
              viewModel.closePhoto()
              viewModel.openImageCompressor(media = item, initialTab = 2)
            },
            onDelete = { photo ->
              viewModel.openSafeDeleteDialog(listOf(photo))
            },
            onSafeRemove = { photo ->
              viewModel.safeRemoveSinglePhoto(photo)
            },
            onMoveToVault = { photo ->
              viewModel.showMoveToVaultDialog(listOf(photo))
            },
            onNextPhoto = { viewModel.nextPhoto() },
            onPrevPhoto = { viewModel.prevPhoto() }
          )
        }
      }

      // Full-screen Document Viewer Overlay
      AnimatedVisibility(
        visible = uiState.openedDocument != null,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        uiState.openedDocument?.let { doc ->
          DocumentViewerScreen(
            document = doc,
            onClose = { viewModel.closeDocument() },
            onOpenOcr = { viewModel.openOcrResult(doc) },
            onAskAi = { prompt -> viewModel.askAiAboutDocument(doc, prompt) },
            onRenameDocument = { newTitle -> viewModel.renameDocument(doc.id, newTitle) },
            onChangeCategory = { newCategory -> viewModel.updateDocumentCategory(doc.id, newCategory) },
            onRotatePage = { pageIdx -> viewModel.rotateDocumentPage(doc.id, pageIdx) },
            onDeletePage = { pageIdx -> viewModel.deleteDocumentPage(doc.id, pageIdx) },
            onReorderPages = { from, to -> viewModel.reorderDocumentPages(doc.id, from, to) },
            onAddPage = { viewModel.openScanner() },
            onDeleteDocument = { viewModel.deleteDocument(doc.id) }
          )
        }
      }

      // Full-screen OCR Text Workspace Overlay
      AnimatedVisibility(
        visible = uiState.openedOcrResult != null,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        uiState.openedOcrResult?.let { (doc, result) ->
          OcrResultScreen(
            document = doc,
            ocrResult = result,
            onSaveEditedText = { newText -> viewModel.updateDocumentOcr(doc.id, newText) },
            onAskAi = { prompt -> viewModel.askAiAboutDocument(doc, prompt) },
            onClose = { viewModel.closeOcrResult() }
          )
        }
      }

      // Full-screen Document Scanner & Warper Overlay
      AnimatedVisibility(
        visible = uiState.isScannerOpen,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        DocumentScannerScreen(
          initialImageUri = uiState.scannerSourceUri,
          initialMedia = uiState.scannerSourceMedia,
          onSaveDocument = { title, category, bitmap, addAnother ->
            viewModel.saveScannedDocument(title, category, bitmap, addAnother)
          },
          onClose = { viewModel.closeScanner() }
        )
      }

      // Rename Document Dialog
      uiState.showRenameDocDialog?.let { doc ->
        var renameText by remember(doc.id) { mutableStateOf(doc.title) }
        AlertDialog(
          onDismissRequest = { viewModel.showRenameDocDialog(null) },
          title = { Text("Rename Document") },
          text = {
            OutlinedTextField(
              value = renameText,
              onValueChange = { renameText = it },
              singleLine = true,
              label = { Text("Document Title") },
              modifier = Modifier.fillMaxWidth()
            )
          },
          confirmButton = {
            Button(onClick = {
              if (renameText.isNotBlank()) {
                viewModel.renameDocument(doc.id, renameText.trim())
              }
              viewModel.showRenameDocDialog(null)
            }) {
              Text("Save")
            }
          },
          dismissButton = {
            TextButton(onClick = { viewModel.showRenameDocDialog(null) }) {
              Text("Cancel")
            }
          }
        )
      }

      // Change Document Category Dialog
      uiState.showChangeDocCategoryDialog?.let { doc ->
        AlertDialog(
          onDismissRequest = { viewModel.showChangeDocCategoryDialog(null) },
          title = { Text("Change Category") },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              DocumentCategories.EDITABLE_CATEGORIES.forEach { category ->
                val isSelected = doc.category.equals(category, ignoreCase = true)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      viewModel.updateDocumentCategory(doc.id, category)
                      viewModel.showChangeDocCategoryDialog(null)
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  RadioButton(
                    selected = isSelected,
                    onClick = {
                      viewModel.updateDocumentCategory(doc.id, category)
                      viewModel.showChangeDocCategoryDialog(null)
                    }
                  )
                  Spacer(Modifier.width(8.dp))
                  Text(category)
                }
              }
            }
          },
          confirmButton = {},
          dismissButton = {
            TextButton(onClick = { viewModel.showChangeDocCategoryDialog(null) }) {
              Text("Cancel")
            }
          }
        )
      }

      // --- Modals & Sheets ---

      // 1. Filter Bottom Sheet
      if (uiState.isFilterSheetOpen) {
        val folderOptions = remember(uiState.albums) {
          uiState.albums.map { it.name }.distinct()
        }
        FilterBottomSheet(
          filterState = uiState.filterState,
          sortOption = uiState.sortOption,
          folderOptions = folderOptions,
          onFilterChange = { transform -> viewModel.updateFilterState(transform) },
          onSortChange = { option -> viewModel.setSortOption(option) },
          onClearFilters = { viewModel.clearFilters() },
          onDismiss = { viewModel.setFilterSheetOpen(false) }
        )
      }

      // 2. Create Custom Album Dialog
      if (uiState.showCreateAlbumDialog) {
        CreateAlbumDialog(
          selectedMediaCount = uiState.selectedPhotoIds.size,
          onDismiss = { viewModel.showCreateAlbumDialog(false) },
          onConfirm = { albumName ->
            viewModel.createCustomAlbum(albumName, uiState.selectedPhotoIds.toList())
          }
        )
      }

      // 3. Add to Album Bottom Sheet
      if (uiState.showAddToAlbumDialog) {
        AddToAlbumBottomSheet(
          albums = uiState.albums,
          onAlbumSelected = { albumId ->
            viewModel.addSelectedToAlbum(albumId)
          },
          onCreateNewAlbum = {
            viewModel.showAddToAlbumDialog(false)
            viewModel.showCreateAlbumDialog(true)
          },
          onDismiss = { viewModel.showAddToAlbumDialog(false) }
        )
      }

      // 4. Rename Album Dialog
      uiState.showRenameAlbumDialog?.let { album ->
        album.customAlbumId?.let { customId ->
          RenameAlbumDialog(
            currentAlbum = album,
            onDismiss = { viewModel.showRenameAlbumDialog(null) },
            onConfirm = { newName ->
              viewModel.renameCustomAlbum(customId, newName)
            }
          )
        }
      }

      // 5. Delete Album Dialog
      uiState.showDeleteAlbumDialog?.let { album ->
        album.customAlbumId?.let { customId ->
          DeleteAlbumDialog(
            album = album,
            onDismiss = { viewModel.showDeleteAlbumDialog(null) },
            onConfirm = {
              viewModel.deleteCustomAlbum(customId)
            }
          )
        }
      }

      // 6. Choose Album Cover Sheet
      uiState.showCoverChooserAlbum?.let { album ->
        album.customAlbumId?.let { customId ->
          AlbumCoverChooserSheet(
            album = album,
            mediaItems = uiState.photos,
            onCoverChosen = { uri ->
              viewModel.setCustomAlbumCover(customId, uri)
            },
            onDismiss = { viewModel.showCoverChooser(null) }
          )
        }
      }

      // 7. Selection Summary Dialog
      if (uiState.showSelectionDetailsDialog && uiState.selectionSummary != null) {
        SelectionSummaryDialog(
          summary = uiState.selectionSummary!!,
          onDismiss = { viewModel.dismissSelectionSummary() }
        )
      }

      // 8. Full-screen PDF Studio Home Overlay
      AnimatedVisibility(
        visible = uiState.isPdfStudioOpen,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        PdfStudioHomeScreen(
          history = uiState.pdfHistory,
          onImagesToPdfClick = { viewModel.startCreatePdfFromImages() },
          onScanToPdfClick = { viewModel.startCreatePdfFromScan() },
          onDocumentsToPdfClick = { viewModel.startCreatePdfFromDocuments() },
          onCompressPdfClick = { viewModel.startCreatePdfQuickCompress() },
          onOpenPdf = { viewModel.openPdf(it) },
          onSharePdf = { viewModel.sharePdf(it) },
          onRenamePdf = { item, newName -> viewModel.renamePdf(item, newName) },
          onDeletePdf = { item, deleteDeviceFile -> viewModel.deletePdf(item, deleteDeviceFile) },
          onClose = { viewModel.closePdfStudio() }
        )
      }

      // 9. Full-screen PDF Creator Workspace Overlay
      AnimatedVisibility(
        visible = uiState.isPdfCreatorOpen,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        PdfCreatorScreen(
          pages = uiState.pdfCreatorPages,
          config = uiState.pdfConfig,
          onConfigChange = { viewModel.updatePdfConfig(it) },
          onAddPagesFromGallery = { viewModel.addSamplePagesToPdfCreator() },
          onAddPageFromScanner = { viewModel.openScanner() },
          onMovePage = { from, to -> viewModel.movePdfCreatorPage(from, to) },
          onRotatePage = { viewModel.rotatePdfCreatorPage(it) },
          onDuplicatePage = { viewModel.duplicatePdfCreatorPage(it) },
          onDeletePage = { viewModel.deletePdfCreatorPage(it) },
          onReplacePage = { viewModel.replacePdfCreatorPage(it) },
          onPageFitChange = { idx, fit -> viewModel.updatePdfCreatorPageFit(idx, fit) },
          onPreviewClick = { viewModel.openPdfInteractivePreview() },
          onCreatePdfClick = { viewModel.generatePdf() },
          onClose = { viewModel.closePdfCreator() }
        )
      }

      // 10. Full-screen PDF Interactive Preview Overlay
      AnimatedVisibility(
        visible = uiState.isPdfPreviewOpen,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
      ) {
        PdfInteractivePreviewScreen(
          pages = uiState.pdfCreatorPages,
          config = uiState.pdfConfig,
          onBackToEdit = { viewModel.closePdfInteractivePreview() },
          onGeneratePdf = { viewModel.generatePdf() }
        )
      }

      // 11. PDF Generation Progress Dialog
      uiState.pdfGenerationProgress?.let { progress ->
        PdfProgressDialog(
          progress = progress,
          onCancel = { viewModel.cancelPdfGeneration() }
        )
      }

      // 12. PDF Success Sheet
      uiState.createdPdfResult?.let { createdItem ->
        PdfSuccessSheet(
          item = createdItem,
          onDismiss = { viewModel.dismissPdfSuccessSheet() },
          onOpen = {
            viewModel.dismissPdfSuccessSheet()
            viewModel.openPdf(createdItem)
          },
          onShare = {
            viewModel.sharePdf(createdItem)
          },
          onRename = {
            viewModel.showUserNotice("PDF saved as '${createdItem.fileName}'. You can rename anytime in PDF Studio.")
          },
          onCreateAnother = {
            viewModel.dismissPdfSuccessSheet()
            viewModel.startCreatePdfFromImages()
          }
        )
      }

      // 13. Full-screen Advanced Photo Editor Pro Overlay
      AnimatedVisibility(
        visible = uiState.activeEditorMedia != null,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        uiState.activeEditorMedia?.let { media ->
          PhotoEditorScreen(
            sourceUri = Uri.parse(media.uri ?: ""),
            mediaId = media.id,
            title = media.title,
            initialStateJson = uiState.activeEditorProject?.stateJson,
            onSaveExport = { savedUri, savedPath, state ->
              viewModel.onPhotoEditorExported(savedUri, savedPath, state)
            },
            onSaveProject = { state ->
              viewModel.saveEditingProject(state)
            },
            onOpenAiEditor = { uri, title ->
              viewModel.closeEditor()
              viewModel.openAiPhotoEditor(uri = uri.toString(), title = title)
            },
            onClose = { viewModel.closeEditor() }
          )
        }
      }

      // 14. Editing Projects Bottom Sheet
      if (uiState.showEditingProjectsSheet) {
        EditingProjectsSheet(
          projects = uiState.editingProjects,
          onSelectProject = { project ->
            val media = uiState.photos.find { it.id == project.originalMediaId }
              ?: com.example.model.MediaItem(
                id = project.originalMediaId,
                uri = project.originalUri,
                title = project.title,
                mediaType = com.example.model.MediaType.PHOTO,
                dateTaken = "Project",
                dateTakenMillis = project.lastModifiedMillis,
                albumName = "Edited"
              )
            viewModel.openEditor(media, project)
          },
          onDeleteProject = { viewModel.deleteEditingProject(it) },
          onDismiss = { viewModel.closeEditingProjectsSheet() }
        )
      }

      // 15. Full-screen Gemini Online AI Photo Editor Overlay
      AnimatedVisibility(
        visible = uiState.activeAiEditorMedia != null || uiState.activeAiEditorUri != null,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        val uriStr = uiState.activeAiEditorUri ?: uiState.activeAiEditorMedia?.uri
        if (uriStr != null) {
          AiPhotoEditorScreen(
            sourceUri = Uri.parse(uriStr),
            mediaTitle = uiState.activeAiEditorTitle ?: uiState.activeAiEditorMedia?.title ?: "Photo",
            userApiKey = uiState.settings.geminiApiKey,
            askBeforeSendingToAi = uiState.settings.askBeforeSendingToAi,
            initialPrompt = uiState.activeAiEditorInitialPrompt,
            onSaveExport = { bmp, title ->
              viewModel.onAiPhotoEditorExported(bmp, title)
            },
            onOpenInProEditor = { bmp, title ->
              viewModel.handOffAiImageToProEditor(bmp, title)
            },
            onUpdateAskBeforeSendingPreference = { ask ->
              viewModel.updateAskBeforeSendingToAiPreference(ask)
            },
            onSaveApiKey = { key ->
              viewModel.saveGeminiApiKey(key)
            },
            onClose = { viewModel.closeAiPhotoEditor() }
          )
        }
      }

      // 16. Full-screen Advanced AI Image Understanding & Chat Workspace
      AnimatedVisibility(
        visible = uiState.activeAiUnderstandingMedia != null || uiState.activeAiUnderstandingUri != null,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        val uriStr = uiState.activeAiUnderstandingUri ?: uiState.activeAiUnderstandingMedia?.uri
        if (uriStr != null) {
          AiImageUnderstandingScreen(
            sourceUri = Uri.parse(uriStr),
            mediaTitle = uiState.activeAiUnderstandingTitle ?: uiState.activeAiUnderstandingMedia?.title ?: "Photo",
            isFromVault = uiState.activeAiUnderstandingFromVault,
            userApiKey = uiState.settings.geminiApiKey,
            askBeforeSendingToAi = uiState.settings.askBeforeSendingToAi,
            initialTask = uiState.activeAiUnderstandingTask,
            initialPrompt = uiState.activeAiUnderstandingInitialPrompt,
            allGalleryPhotos = uiState.allRawPhotos.ifEmpty { uiState.photos },
            onSaveMetadataToCache = { mId, text, desc, objs ->
              viewModel.saveAiMetadataCache(mId, uriStr, text, desc, objs)
            },
            onUpdateAskBeforeSendingPreference = { ask ->
              viewModel.updateAskBeforeSendingToAiPreference(ask)
            },
            onSaveApiKey = { key ->
              viewModel.saveGeminiApiKey(key)
            },
            onClose = { viewModel.closeAiImageUnderstanding() }
          )
        }
      }

      // 17. Full-screen Advanced Image Resize, Dimensions, DPI & Passport Photo Studio Overlay
      AnimatedVisibility(
        visible = uiState.isImageResizeOpen,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        ImageResizeScreen(
          initialMedia = uiState.activeResizeMedia,
          allGalleryPhotos = uiState.allRawPhotos.ifEmpty { uiState.photos },
          initialTab = uiState.initialResizeTab,
          onOpenInViewer = { uri ->
            viewModel.closeImageResize()
            val media = uiState.photos.find { it.uri == uri.toString() } ?: com.example.model.MediaItem(
              id = uri.toString(),
              title = uri.lastPathSegment ?: "Resized Photo",
              uri = uri.toString()
            )
            viewModel.openPhoto(media)
          },
          onOpenInProEditor = { uri ->
            viewModel.closeImageResize()
            val media = com.example.model.MediaItem(
              id = uri.toString(),
              title = uri.lastPathSegment ?: "Resized Photo",
              uri = uri.toString()
            )
            viewModel.openEditor(media)
          },
          onCreatePdfFromUri = { uri ->
            viewModel.closeImageResize()
            viewModel.openPdfStudio()
          },
          onClose = { viewModel.closeImageResize() }
        )
      }

      // 18. Full-screen Advanced Image Compression, Target File Size & Format Converter Overlay
      AnimatedVisibility(
        visible = uiState.isImageCompressorOpen,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        ImageCompressorScreen(
          initialMedia = uiState.activeCompressMedia,
          initialBatchMedias = uiState.batchCompressMedias,
          allGalleryPhotos = uiState.allRawPhotos.ifEmpty { uiState.photos },
          initialTab = uiState.initialCompressTab,
          onOpenInViewer = { uri ->
            viewModel.closeImageCompressor()
            val media = uiState.photos.find { it.uri == uri.toString() } ?: com.example.model.MediaItem(
              id = uri.toString(),
              title = uri.lastPathSegment ?: "Compressed Photo",
              uri = uri.toString()
            )
            viewModel.openPhoto(media)
          },
          onOpenInProEditor = { uri ->
            viewModel.closeImageCompressor()
            val media = com.example.model.MediaItem(
              id = uri.toString(),
              title = uri.lastPathSegment ?: "Compressed Photo",
              uri = uri.toString()
            )
            viewModel.openEditor(media)
          },
          onOpenInResize = { uri ->
            viewModel.closeImageCompressor()
            val media = com.example.model.MediaItem(
              id = uri.toString(),
              title = uri.lastPathSegment ?: "Compressed Photo",
              uri = uri.toString()
            )
            viewModel.openImageResize(media = media, initialTab = 0)
          },
          onCreatePdfFromUri = { uri ->
            viewModel.closeImageCompressor()
            val media = com.example.model.MediaItem(
              id = uri.toString(),
              title = uri.lastPathSegment ?: "Compressed Photo",
              uri = uri.toString()
            )
            viewModel.startCreatePdfFromImages(listOf(media))
          },
          onCreatePdfFromBatch = { uris ->
            viewModel.closeImageCompressor()
            val medias = uris.map { u ->
              com.example.model.MediaItem(
                id = u.toString(),
                title = u.lastPathSegment ?: "Compressed Photo",
                uri = u.toString()
              )
            }
            viewModel.startCreatePdfFromImages(medias)
          },
          onClose = { viewModel.closeImageCompressor() }
        )
      }

      // Global Safe Delete Confirm Modal
      if (uiState.showSafeDeleteDialog && uiState.pendingSafeDeleteItems.isNotEmpty()) {
        com.example.ui.screens.vault.components.SafeDeleteConfirmDialog(
          mediaItems = uiState.pendingSafeDeleteItems,
          onRemoveFromAppOnly = {
            viewModel.performSafeRemoveFromApp(uiState.pendingSafeDeleteItems)
          },
          onDeleteFromDevice = {
            viewModel.performDeleteFromDevice(uiState.pendingSafeDeleteItems)
          },
          onDismiss = {
            viewModel.dismissSafeDeleteDialog()
          }
        )
      }

      // Global Move to Vault Confirm Modal
      if (uiState.showMoveToVaultConfirmDialog && uiState.pendingMoveToVaultItems.isNotEmpty()) {
        com.example.ui.screens.vault.components.MoveToVaultConfirmDialog(
          itemCount = uiState.pendingMoveToVaultItems.size,
          availableAlbums = uiState.vaultAlbums,
          onConfirm = { isMoveAction, albumId ->
            viewModel.confirmMoveToVault(uiState.pendingMoveToVaultItems, albumId, isMoveAction)
          },
          onDismiss = {
            viewModel.dismissMoveToVaultDialog()
          }
        )
      }

      // 19. Full-screen Creative Studio, Collage, Posters, Watermark & Batch Studio Overlay
      AnimatedVisibility(
        visible = uiState.isCreativeStudioOpen,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 6 }),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(targetOffsetY = { it / 6 })
      ) {
        CreativeStudioScreen(
          allGalleryPhotos = uiState.allRawPhotos.ifEmpty { uiState.photos },
          initialTab = uiState.initialCreativeStudioTab,
          onOpenPhotoViewer = { photo ->
            viewModel.closeCreativeStudio()
            viewModel.openPhoto(photo)
          },
          onOpenPassportStudio = {
            viewModel.closeCreativeStudio()
            viewModel.openImageResize(initialTab = 1)
          },
          onOpenPdfStudio = {
            viewModel.closeCreativeStudio()
            viewModel.openPdfStudio()
          },
          onOpenImageResize = { media ->
            viewModel.closeCreativeStudio()
            viewModel.openImageResize(media = media, initialTab = 0)
          },
          onOpenImageCompressor = { media ->
            viewModel.closeCreativeStudio()
            viewModel.openImageCompressor(media = media, initialTab = 0)
          },
          onClose = { viewModel.closeCreativeStudio() }
        )
      }

      // Global Native Share Preview & Format Customizer Dialog
      if (uiState.isShareCenterOpen && uiState.pendingShareItems.isNotEmpty()) {
        SharePreviewDialog(
          items = uiState.pendingShareItems,
          isFromVault = uiState.isVaultShare,
          onConfirmShare = { mode ->
            val itemsToShare = uiState.pendingShareItems
            viewModel.closeShareCenter()
            coroutineScope.launch {
              NativeShareHelper.shareMediaItems(context, itemsToShare, mode)
            }
          },
          onDismiss = { viewModel.closeShareCenter() }
        )
      }

      // Cloud AI Explicit Consent Modal
      if (uiState.showCloudAiConsentDialog) {
        AlertDialog(
          onDismissRequest = { viewModel.dismissCloudAiConsent() },
          title = { Text("Cloud AI Processing Consent") },
          text = {
            Column {
              Text(
                "My Gallery AI operates strictly offline-first. The requested task '${uiState.pendingCloudAiTaskTitle}' connects to Google Gemini AI cloud servers to analyze your selected media.",
                fontSize = 13.sp,
                lineHeight = 18.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                "• Your media is never automatically uploaded.\n• Only the selected image is sent for processing.\n• You can toggle this confirmation in Settings.",
                fontSize = 12.sp,
                color = GalleryTheme.colors.secondaryText
              )
            }
          },
          confirmButton = {
            Button(onClick = { viewModel.proceedCloudAiConsent() }) {
              Text("Authorize & Proceed")
            }
          },
          dismissButton = {
            TextButton(onClick = { viewModel.dismissCloudAiConsent() }) {
              Text("Cancel")
            }
          }
        )
      }
    }
  }
}
