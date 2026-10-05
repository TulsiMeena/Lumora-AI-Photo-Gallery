package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.MediaType
import com.example.model.VaultFolderItem
import com.example.model.vault.VaultAlbumItem
import com.example.model.vault.VaultMediaItem
import com.example.model.vault.VaultSecurityConfig
import com.example.model.vault.VaultTab
import com.example.ui.screens.vault.components.MoveToVaultConfirmDialog
import com.example.ui.screens.vault.components.SafeDeleteConfirmDialog
import com.example.ui.screens.vault.components.VaultMediaViewerDialog
import com.example.ui.screens.vault.components.VaultPinKeypadDialog
import com.example.ui.screens.vault.components.VaultSecuritySettingsDialog
import com.example.ui.screens.vault.components.VaultSetupWizardDialog
import com.example.ui.theme.GalleryTheme
import com.example.utils.vault.VaultBiometricHelper
import com.example.utils.vault.VaultCryptoManager
import com.example.utils.vault.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VaultScreen(
  isUnlocked: Boolean,
  securityConfig: VaultSecurityConfig,
  vaultMediaList: List<VaultMediaItem>,
  vaultAlbums: List<VaultAlbumItem>,
  activeTab: VaultTab,
  searchQuery: String,
  onTabSelected: (VaultTab) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onUnlockSuccess: () -> Unit,
  onLockVault: () -> Unit,
  onSetupCompleted: (pin: String, enableBiometrics: Boolean) -> Unit,
  onUpdateSecurityConfig: (VaultSecurityConfig) -> Unit,
  onVerifyPin: (String) -> Boolean,
  onRecordFailedAttempt: () -> VaultSecurityConfig,
  onResetFailedAttempts: () -> Unit,
  onImportMediaUris: (uris: List<Uri>, albumId: String, isMoveAction: Boolean) -> Unit,
  onExportMedia: (VaultMediaItem) -> Unit,
  onDeleteMediaFromVault: (VaultMediaItem) -> Unit,
  onToggleFavorite: (VaultMediaItem) -> Unit,
  onMoveMediaToAlbum: (VaultMediaItem, String) -> Unit,
  onCreateAlbum: (String) -> Unit,
  onRenameAlbum: (String, String) -> Unit,
  onDeleteAlbum: (String) -> Unit,
  onOpenInAiEdit: ((Bitmap) -> Unit)? = null,
  onOpenInAiChat: ((Bitmap) -> Unit)? = null,
  onOpenInOcr: ((Bitmap) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  val activity = context as? Activity
  val fragmentActivity = context as? FragmentActivity
  val lifecycleOwner = LocalLifecycleOwner.current

  // Dialog States
  var showPinDialog by remember { mutableStateOf(false) }
  var showSetupWizard by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showCreateAlbumDialog by remember { mutableStateOf(false) }
  var showRenameAlbumDialog by remember { mutableStateOf<VaultAlbumItem?>(null) }
  var showDeleteAlbumConfirm by remember { mutableStateOf<VaultAlbumItem?>(null) }
  var activeViewerMedia by remember { mutableStateOf<VaultMediaItem?>(null) }
  var pendingImportUris by remember { mutableStateOf<List<Uri>?>(null) }
  var selectedAlbumFilterId by remember { mutableStateOf<String?>(null) }
  var pinErrorMessage by remember { mutableStateOf<String?>(null) }
  var newAlbumNameInput by remember { mutableStateOf("") }
  var isSearchExpanded by remember { mutableStateOf(false) }

  // System Photo Picker for Vault Import
  val visualMediaPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    if (uris.isNotEmpty()) {
      pendingImportUris = uris
    }
  }

  // 1. Screen Security (FLAG_SECURE): blocks screenshots and app switcher sneak-peeks
  DisposableEffect(isUnlocked, securityConfig.isSecureScreenEnabled) {
    if (isUnlocked && securityConfig.isSecureScreenEnabled) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  // 2. Auto-Lock on App Background
  DisposableEffect(lifecycleOwner, isUnlocked, securityConfig.autoLockTimeoutSeconds) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP && isUnlocked) {
        onLockVault()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Compute lockout remaining time
  val currentMillis = System.currentTimeMillis()
  val lockoutSeconds = if (securityConfig.lockoutUntilMillis > currentMillis) {
    ((securityConfig.lockoutUntilMillis - currentMillis) / 1000).toInt()
  } else 0

  // Filter media based on current tab, album selection, and search query
  val filteredMedia = remember(vaultMediaList, activeTab, selectedAlbumFilterId, searchQuery) {
    vaultMediaList.filter { item ->
      val matchesTab = when (activeTab) {
        VaultTab.PHOTOS -> item.mediaType == MediaType.PHOTO
        VaultTab.VIDEOS -> item.mediaType == MediaType.VIDEO
        VaultTab.ALBUMS -> selectedAlbumFilterId == null || item.albumId == selectedAlbumFilterId
        VaultTab.FAVORITES -> item.isFavorite
      }
      val matchesAlbum = if (selectedAlbumFilterId != null && activeTab != VaultTab.ALBUMS) {
        item.albumId == selectedAlbumFilterId
      } else true
      val matchesSearch = if (searchQuery.isNotBlank()) {
        item.title.contains(searchQuery, ignoreCase = true) ||
          item.originalFileName.contains(searchQuery, ignoreCase = true)
      } else true

      matchesTab && matchesAlbum && matchesSearch
    }
  }

  // Handle back button when viewing an album's contents inside Vault
  if (selectedAlbumFilterId != null) {
    BackHandler { selectedAlbumFilterId = null }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // Top Bar
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = colors.background
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (selectedAlbumFilterId != null) {
            IconButton(
              onClick = { selectedAlbumFilterId = null },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.primaryText
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          } else {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) colors.success.copy(alpha = 0.15f) else colors.warning.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (isUnlocked) colors.success else colors.warning,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
          }

          Column {
            val title = if (selectedAlbumFilterId != null) {
              vaultAlbums.find { it.id == selectedAlbumFilterId }?.name ?: "Private Album"
            } else {
              "Private Vault"
            }
            Text(
              text = title,
              color = colors.primaryText,
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (isUnlocked) "AES-256 GCM • Active Session" else "Hardware Keystore • Offline Only",
              color = if (isUnlocked) colors.success else colors.secondaryText,
              fontSize = 11.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isUnlocked) {
            IconButton(
              onClick = { isSearchExpanded = !isSearchExpanded },
              modifier = Modifier.size(38.dp).testTag("btn_vault_search_toggle")
            ) {
              Icon(
                imageVector = if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = "Search Vault",
                tint = colors.primaryText,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = { showSettingsDialog = true },
              modifier = Modifier.size(38.dp).testTag("btn_vault_settings")
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Vault Settings",
                tint = colors.primaryText,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
              onClick = onLockVault,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.primaryText
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_lock_vault")
            ) {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    if (!isUnlocked) {
      // --- LOCKED VAULT HOME ---
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp)
          .testTag("vault_locked_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .border(2.dp, colors.warning.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = "Locked",
            tint = colors.warning,
            modifier = Modifier.size(52.dp)
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
          text = "Secure Private Vault",
          color = colors.primaryText,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Your private photos, documents, and videos are protected with local AES-256 encryption. Zero cloud exposure.",
          color = colors.secondaryText,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
          lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy Guarantee Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Protected by Android Keystore (AES-256 GCM)", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Shield, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Zero Cloud Uploads • Decrypted Only in RAM", color = colors.secondaryText, fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!securityConfig.isSetupCompleted) {
          // Setup Wizard Button
          Button(
            onClick = { showSetupWizard = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("btn_setup_vault")
          ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Set Up Private Vault", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          // Unlock Action Button
          Button(
            onClick = {
              pinErrorMessage = null
              showPinDialog = true
              // If biometric is enabled, also trigger prompt immediately
              if (securityConfig.isBiometricEnabled && fragmentActivity != null && VaultBiometricHelper.isBiometricAvailable(context)) {
                VaultBiometricHelper.authenticateWithBiometrics(
                  activity = fragmentActivity,
                  onSuccess = {
                    showPinDialog = false
                    onResetFailedAttempts()
                    onUnlockSuccess()
                  },
                  onError = { /* fallback to PIN */ }
                )
              }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("btn_unlock_vault")
          ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Unlock with PIN or Biometrics", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      // --- UNLOCKED VAULT INTERFACE ---

      // Search Bar when expanded
      AnimatedVisibility(visible = isSearchExpanded) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search private media by title...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.accent) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = colors.secondaryText)
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.accent,
              unfocusedBorderColor = colors.border,
              focusedContainerColor = colors.surface,
              unfocusedContainerColor = colors.surface
            ),
            modifier = Modifier.fillMaxWidth().testTag("vault_search_field")
          )
        }
      }

      // Vault Tabs
      if (selectedAlbumFilterId == null) {
        ScrollableTabRow(
          selectedTabIndex = activeTab.ordinal,
          edgePadding = 16.dp,
          containerColor = colors.background,
          contentColor = colors.accent,
          indicator = { tabPositions ->
            if (activeTab.ordinal < tabPositions.size) {
              TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                color = colors.accent,
                height = 3.dp
              )
            }
          },
          divider = {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.5f)))
          }
        ) {
          VaultTab.entries.forEach { tab ->
            val count = when (tab) {
              VaultTab.PHOTOS -> vaultMediaList.count { it.mediaType == MediaType.PHOTO }
              VaultTab.VIDEOS -> vaultMediaList.count { it.mediaType == MediaType.VIDEO }
              VaultTab.ALBUMS -> vaultAlbums.size
              VaultTab.FAVORITES -> vaultMediaList.count { it.isFavorite }
            }
            Tab(
              selected = activeTab == tab,
              onClick = { onTabSelected(tab) },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = tab.label,
                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (activeTab == tab) colors.primaryText else colors.secondaryText
                  )
                  if (count > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                      modifier = Modifier
                        .clip(CircleShape)
                        .background(if (activeTab == tab) colors.accent.copy(alpha = 0.2f) else colors.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == tab) colors.accent else colors.secondaryText
                      )
                    }
                  }
                }
              },
              modifier = Modifier.testTag("vault_tab_${tab.name}")
            )
          }
        }
      }

      // Action Header / Quick Add Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = when {
            selectedAlbumFilterId != null -> "${filteredMedia.size} items in album"
            activeTab == VaultTab.ALBUMS -> "${vaultAlbums.size} private albums"
            else -> "${filteredMedia.size} encrypted items"
          },
          color = colors.secondaryText,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (activeTab == VaultTab.ALBUMS && selectedAlbumFilterId == null) {
            Button(
              onClick = {
                newAlbumNameInput = ""
                showCreateAlbumDialog = true
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.accent),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("btn_create_vault_album")
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New Album", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Button(
            onClick = {
              visualMediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
              )
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_add_to_vault_picker")
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add to Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Content View
      if (activeTab == VaultTab.ALBUMS && selectedAlbumFilterId == null) {
        // Vault Albums Grid
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize().testTag("vault_albums_grid")
        ) {
          items(vaultAlbums, key = { it.id }) { album ->
            VaultAlbumCard(
              album = album,
              coverMedia = vaultMediaList.find { it.albumId == album.id },
              onClick = { selectedAlbumFilterId = album.id },
              onRenameClick = { showRenameAlbumDialog = album },
              onDeleteClick = { showDeleteAlbumConfirm = album }
            )
          }
        }
      } else {
        // Encrypted Media Grid
        if (filteredMedia.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .clip(CircleShape)
                  .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = when (activeTab) {
                    VaultTab.VIDEOS -> Icons.Rounded.VideoLibrary
                    VaultTab.FAVORITES -> Icons.Rounded.Favorite
                    else -> Icons.Rounded.Lock
                  },
                  contentDescription = null,
                  tint = colors.secondaryText,
                  modifier = Modifier.size(36.dp)
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = if (searchQuery.isNotBlank()) "No matching private items" else "No encrypted media here yet",
                color = colors.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Tap 'Add to Vault' above to securely encrypt and store private photos or videos.",
                color = colors.secondaryText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().testTag("vault_media_grid")
          ) {
            items(filteredMedia, key = { it.id }) { mediaItem ->
              VaultMediaGridItem(
                item = mediaItem,
                onClick = { activeViewerMedia = mediaItem }
              )
            }
          }
        }
      }
    }
  }

  // --- DIALOGS ---

  // 1. PIN Keypad Modal
  if (showPinDialog) {
    VaultPinKeypadDialog(
      expectedPinLength = securityConfig.pinLength,
      isBiometricAvailable = fragmentActivity != null && VaultBiometricHelper.isBiometricAvailable(context),
      isBiometricEnabled = securityConfig.isBiometricEnabled,
      lockoutRemainingSeconds = lockoutSeconds,
      errorMessage = pinErrorMessage,
      onPinEntered = { pin ->
        val isValid = onVerifyPin(pin)
        if (isValid) {
          com.example.utils.sound.AppSoundManager.playUnlockSuccess()
          pinErrorMessage = null
          showPinDialog = false
          onResetFailedAttempts()
          onUnlockSuccess()
        } else {
          com.example.utils.sound.AppSoundManager.playError()
          val updated = onRecordFailedAttempt()
          if (updated.lockoutUntilMillis > System.currentTimeMillis()) {
            pinErrorMessage = "Locked out. Too many failed attempts."
          } else {
            pinErrorMessage = "Incorrect PIN. Try again."
          }
        }
      },
      onBiometricClick = {
        if (fragmentActivity != null) {
          VaultBiometricHelper.authenticateWithBiometrics(
            activity = fragmentActivity,
            onSuccess = {
              com.example.utils.sound.AppSoundManager.playUnlockSuccess()
              showPinDialog = false
              onResetFailedAttempts()
              onUnlockSuccess()
            },
            onError = { err ->
              com.example.utils.sound.AppSoundManager.playError()
              pinErrorMessage = err
            }
          )
        }
      },
      onDismiss = {
        showPinDialog = false
        pinErrorMessage = null
      }
    )
  }

  // 2. Vault Setup Wizard Dialog
  if (showSetupWizard) {
    VaultSetupWizardDialog(
      isBiometricHardwareAvailable = VaultBiometricHelper.isBiometricAvailable(context),
      onSetupCompleted = { pin, enableBio ->
        showSetupWizard = false
        onSetupCompleted(pin, enableBio)
      },
      onDismiss = { showSetupWizard = false }
    )
  }

  // 3. Vault Security Settings Dialog
  if (showSettingsDialog) {
    VaultSecuritySettingsDialog(
      config = securityConfig,
      isBiometricHardwareAvailable = VaultBiometricHelper.isBiometricAvailable(context),
      onUpdateConfig = { newConfig ->
        onUpdateSecurityConfig(newConfig)
      },
      onChangePinClick = {
        showSettingsDialog = false
        showSetupWizard = true
      },
      onLockVaultNow = {
        showSettingsDialog = false
        onLockVault()
      },
      onDismiss = { showSettingsDialog = false }
    )
  }

  // 4. Move to Vault Confirm Dialog (from Picker)
  if (pendingImportUris != null) {
    MoveToVaultConfirmDialog(
      itemCount = pendingImportUris!!.size,
      availableAlbums = vaultAlbums,
      onConfirm = { isMoveAction, albumId ->
        val uris = pendingImportUris!!
        pendingImportUris = null
        onImportMediaUris(uris, albumId, isMoveAction)
      },
      onDismiss = { pendingImportUris = null }
    )
  }

  // 5. Full-Screen Vault Media Viewer
  activeViewerMedia?.let { currentItem ->
    VaultMediaViewerDialog(
      item = currentItem,
      askBeforeAiUpload = securityConfig.askBeforeAiUpload,
      exportWarningEnabled = securityConfig.exportWarningEnabled,
      onToggleFavorite = { updated ->
        onToggleFavorite(updated)
        activeViewerMedia = updated.copy(isFavorite = !updated.isFavorite)
      },
      onExportToGallery = { item ->
        onExportMedia(item)
        activeViewerMedia = null
      },
      onDeleteFromVault = { item ->
        onDeleteMediaFromVault(item)
        activeViewerMedia = null
      },
      onOpenInAiEdit = onOpenInAiEdit,
      onOpenInAiChat = onOpenInAiChat,
      onOpenInOcr = onOpenInOcr,
      onDismiss = { activeViewerMedia = null }
    )
  }

  // 6. Create Album Dialog
  if (showCreateAlbumDialog) {
    AlertDialog(
      onDismissRequest = { showCreateAlbumDialog = false },
      title = { Text("Create Private Album", color = colors.primaryText, fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = newAlbumNameInput,
          onValueChange = { newAlbumNameInput = it },
          label = { Text("Album Name") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("input_new_vault_album_name")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newAlbumNameInput.isNotBlank()) {
              onCreateAlbum(newAlbumNameInput.trim())
              showCreateAlbumDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) {
          Text("Create")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showCreateAlbumDialog = false }) {
          Text("Cancel", color = colors.secondaryText)
        }
      },
      containerColor = colors.surface
    )
  }

  // 7. Rename Album Dialog
  showRenameAlbumDialog?.let { album ->
    var renameInput by remember(album.id) { mutableStateOf(album.name) }
    AlertDialog(
      onDismissRequest = { showRenameAlbumDialog = null },
      title = { Text("Rename Album", color = colors.primaryText, fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = renameInput,
          onValueChange = { renameInput = it },
          label = { Text("New Name") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("input_rename_vault_album")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (renameInput.isNotBlank()) {
              onRenameAlbum(album.id, renameInput.trim())
              showRenameAlbumDialog = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showRenameAlbumDialog = null }) {
          Text("Cancel", color = colors.secondaryText)
        }
      },
      containerColor = colors.surface
    )
  }

  // 8. Delete Album Confirm Dialog
  showDeleteAlbumConfirm?.let { album ->
    AlertDialog(
      onDismissRequest = { showDeleteAlbumConfirm = null },
      title = { Text("Delete Album '${album.name}'?", color = colors.primaryText, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Items inside this album will NOT be deleted. They will safely be moved back to the 'Personal' folder.",
          color = colors.secondaryText,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAlbum(album.id)
            showDeleteAlbumConfirm = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.error)
        ) {
          Text("Delete Album")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteAlbumConfirm = null }) {
          Text("Cancel", color = colors.secondaryText)
        }
      },
      containerColor = colors.surface
    )
  }
}

/**
 * Grid Item for an encrypted photo or video.
 * Decodes the thumbnail directly in memory with zero disk cache.
 */
@Composable
private fun VaultMediaGridItem(
  item: VaultMediaItem,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
  var isLoading by remember(item.id) { mutableStateOf(true) }

  LaunchedEffect(item.id, item.encryptedFileName) {
    withContext(Dispatchers.IO) {
      val mediaDir = VaultStorageManager.getVaultMediaDir(context)
      val encFile = File(mediaDir, item.encryptedFileName)
      val iv = try {
        android.util.Base64.decode(item.ivBase64, android.util.Base64.NO_WRAP)
      } catch (_: Exception) {
        null
      }
      if (iv != null && encFile.exists()) {
        val decoded = VaultCryptoManager.decodeEncryptedBitmap(encFile, iv, maxDim = 384)
        withContext(Dispatchers.Main) {
          bitmap = decoded
          isLoading = false
        }
      } else {
        withContext(Dispatchers.Main) {
          isLoading = false
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surfaceVariant)
      .border(0.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("vault_item_${item.id}")
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = item.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else if (isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.accent)
      }
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
          imageVector = if (item.mediaType == MediaType.VIDEO) Icons.Rounded.PlayCircle else Icons.Rounded.Image,
          contentDescription = null,
          tint = colors.secondaryText,
          modifier = Modifier.size(28.dp)
        )
      }
    }

    // Overlay Badges: Video indicator & Size
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(6.dp)
    ) {
      if (item.mediaType == MediaType.VIDEO) {
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(4.dp)
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
      }

      if (item.isFavorite) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(4.dp)
        ) {
          Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.error, modifier = Modifier.size(12.dp))
        }
      }

      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .clip(RoundedCornerShape(4.dp))
          .background(Color.Black.copy(alpha = 0.65f))
          .padding(horizontal = 4.dp, vertical = 2.dp)
      ) {
        Text(
          text = item.formattedSize,
          color = Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * Album Card for encrypted album categories.
 */
@Composable
private fun VaultAlbumCard(
  album: VaultAlbumItem,
  coverMedia: VaultMediaItem?,
  onClick: () -> Unit,
  onRenameClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  var menuExpanded by remember { mutableStateOf(false) }

  var coverBitmap by remember(coverMedia?.id) { mutableStateOf<Bitmap?>(null) }
  LaunchedEffect(coverMedia?.id) {
    if (coverMedia != null) {
      withContext(Dispatchers.IO) {
        val mediaDir = VaultStorageManager.getVaultMediaDir(context)
        val encFile = File(mediaDir, coverMedia.encryptedFileName)
        val iv = try {
          android.util.Base64.decode(coverMedia.ivBase64, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
          null
        }
        if (iv != null && encFile.exists()) {
          val decoded = VaultCryptoManager.decodeEncryptedBitmap(encFile, iv, maxDim = 384)
          withContext(Dispatchers.Main) {
            coverBitmap = decoded
          }
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surface)
      .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag("vault_album_${album.id}")
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.2f)
          .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        if (coverBitmap != null) {
          Image(
            bitmap = coverBitmap!!.asImageBitmap(),
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(44.dp)
          )
        }

        // Encryption Badge
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text("AES-256", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = album.name,
            color = colors.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${album.itemCount} items",
            color = colors.secondaryText,
            fontSize = 11.sp
          )
        }

        // Three-dot menu for Custom Albums (allow rename and delete)
        if (album.id != "personal" && album.id != "important") {
          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Rounded.Edit, contentDescription = "Manage", tint = colors.secondaryText, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false },
              modifier = Modifier.background(colors.surface)
            ) {
              DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.accent) },
                onClick = {
                  menuExpanded = false
                  onRenameClick()
                }
              )
              DropdownMenuItem(
                text = { Text("Delete Album", color = colors.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
                onClick = {
                  menuExpanded = false
                  onDeleteClick()
                }
              )
            }
          }
        }
      }
    }
  }
}
