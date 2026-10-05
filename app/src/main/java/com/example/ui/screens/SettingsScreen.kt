package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VolumeUp
import com.example.utils.sound.AppSoundManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GallerySettingsState
import com.example.model.SortOption
import com.example.ui.theme.GalleryTheme
import com.example.ui.theme.ThemeMode
import com.example.utils.storage.StorageBreakdown

@Composable
fun SettingsScreen(
  settings: GallerySettingsState,
  onUpdateSettings: (GallerySettingsState) -> Unit,
  onBack: () -> Unit,
  onClearCache: () -> Unit,
  onCleanTemporaryFiles: (() -> Unit)? = null,
  onRestoreExcluded: (() -> Unit)? = null,
  onClearAiMetadata: (() -> Unit)? = null,
  storageBreakdown: StorageBreakdown? = null,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  var showApiKeyDialog by remember { mutableStateOf(false) }
  var apiKeyInput by remember { mutableStateOf(settings.geminiApiKey) }

  var showClearCacheConfirm by remember { mutableStateOf(false) }
  var showCleanTempConfirm by remember { mutableStateOf(false) }
  var showClearAiCacheConfirm by remember { mutableStateOf(false) }

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
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_settings_back")
        ) {
          Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = colors.primaryText
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Settings",
          color = colors.primaryText,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("settings_list")
    ) {
      // 1. APPEARANCE
      item {
        SettingsSectionHeader(title = "Appearance", icon = Icons.Rounded.DarkMode)
        SettingsCard {
          Text(
            text = "Theme Preference",
            color = colors.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ThemeOptionButton(
              title = "System",
              isSelected = settings.themeMode == ThemeMode.SYSTEM,
              onClick = { onUpdateSettings(settings.copy(themeMode = ThemeMode.SYSTEM)) },
              modifier = Modifier.weight(1f)
            )
            ThemeOptionButton(
              title = "Light",
              isSelected = settings.themeMode == ThemeMode.LIGHT,
              onClick = { onUpdateSettings(settings.copy(themeMode = ThemeMode.LIGHT)) },
              modifier = Modifier.weight(1f)
            )
            ThemeOptionButton(
              title = "Dark",
              isSelected = settings.themeMode == ThemeMode.DARK,
              onClick = { onUpdateSettings(settings.copy(themeMode = ThemeMode.DARK)) },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 2. GALLERY PREFERENCES
      item {
        SettingsSectionHeader(title = "Gallery", icon = Icons.Rounded.PhotoSizeSelectActual)
        SettingsCard {
          // Grid size
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Default Grid Columns",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Currently ${settings.gridColumns} columns per row",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(2, 3, 4).forEach { cols ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (settings.gridColumns == cols) colors.accent else colors.surfaceVariant)
                    .clickable { onUpdateSettings(settings.copy(gridColumns = cols)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = "$cols",
                    color = if (settings.gridColumns == cols) Color.White else colors.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Sorting
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Sorting Order",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = settings.sortOption.displayName,
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant)
                .clickable {
                  val entries = SortOption.entries
                  val currentIndex = entries.indexOf(settings.sortOption)
                  val next = entries[(currentIndex + 1) % entries.size]
                  onUpdateSettings(settings.copy(sortOption = next))
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = "Change",
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Smooth Animations
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Smooth Transitions & Haptics",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Fluid 60/120fps motion curves",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.smoothAnimations,
              onCheckedChange = { onUpdateSettings(settings.copy(smoothAnimations = it)) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }
        }
      }

      // 3. PRIVACY & SECURITY
      item {
        SettingsSectionHeader(title = "Privacy & Vault", icon = Icons.Rounded.Security)
        SettingsCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Biometric Vault Authentication",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Require Fingerprint or PIN to view vault items",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.bioAuthEnabled,
              onCheckedChange = { onUpdateSettings(settings.copy(bioAuthEnabled = it)) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Local-Only Mode",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Strict offline processing; no media leaves device",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.localOnlyAi,
              onCheckedChange = { onUpdateSettings(settings.copy(localOnlyAi = it)) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }
        }
      }

      // 4. AI CONFIGURATION
      item {
        SettingsSectionHeader(title = "AI Engine", icon = Icons.Rounded.AutoAwesome)
        SettingsCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "On-Device Offline AI",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Instant OCR and local face/object tagging",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.offlineAiEnabled,
              onCheckedChange = { onUpdateSettings(settings.copy(offlineAiEnabled = it)) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Gemini Cloud AI",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "High-precision multimodal reasoning & complex edits",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.onlineAiEnabled,
              onCheckedChange = { onUpdateSettings(settings.copy(onlineAiEnabled = it)) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(colors.surfaceVariant)
              .clickable { showApiKeyDialog = true }
              .padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Gemini API Key",
                  color = colors.primaryText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = if (settings.geminiApiKey.isNotBlank()) "•••••••••••• Key Configured" else "Configured via AI Studio Secrets",
                  color = colors.secondaryText,
                  fontSize = 11.sp
                )
              }
              Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ask Before Sending Images to AI",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Always show a confirmation prompt before any cloud transmission",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.askBeforeSendingToAi,
              onCheckedChange = { onUpdateSettings(settings.copy(askBeforeSendingToAi = it)) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          if (onClearAiMetadata != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Clear AI Metadata Cache",
                  color = colors.primaryText,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "Delete locally saved OCR, AI descriptions & visual tags without removing photos",
                  color = colors.secondaryText,
                  fontSize = 12.sp
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = { showClearAiCacheConfirm = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = colors.surfaceVariant,
                  contentColor = colors.warning
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                 Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      // 5. SOUND & TACTILE FEEDBACK
      item {
        SettingsSectionHeader(title = "Sound & Tactile Feedback", icon = Icons.Rounded.VolumeUp)
        SettingsCard {
          // Interactive Sound Effects
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Interactive Sound Effects",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Play micro-sounds for toggles, Vault PIN keypad, unlock, delete, and exports",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.soundEffectsEnabled,
              onCheckedChange = {
                AppSoundManager.playToggle(it)
                onUpdateSettings(settings.copy(soundEffectsEnabled = it))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Tactile Haptic Vibration
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tactile Haptic Feedback",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Subtle vibration clicks on taps, toggle changes, and PIN entry",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.hapticFeedbackEnabled,
              onCheckedChange = {
                AppSoundManager.playToggle(it)
                onUpdateSettings(settings.copy(hapticFeedbackEnabled = it))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Preview Interactive Sounds",
            color = colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { AppSoundManager.playKeyClick() },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.primaryText
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("PIN Tick", fontSize = 11.sp, maxLines = 1)
            }

            Button(
              onClick = { AppSoundManager.playToggle(true) },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.primaryText
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Toggle", fontSize = 11.sp, maxLines = 1)
            }

            Button(
              onClick = { AppSoundManager.playUnlockSuccess() },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.success
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Unlock", fontSize = 11.sp, maxLines = 1)
            }

            Button(
              onClick = { AppSoundManager.playComplete() },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.accent
              ),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Chime", fontSize = 11.sp, maxLines = 1)
            }
          }
        }
      }

      // 6. PERFORMANCE & LARGE GALLERY
      item {
        SettingsSectionHeader(title = "Performance & Architecture", icon = Icons.Rounded.Speed)
        SettingsCard {
          // Incremental Indexing
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Incremental MediaStore Sync",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Syncs delta changes without rescanning phone storage; prevents battery drain and lag on 10,000+ photo libraries",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.incrementalIndexingEnabled,
              onCheckedChange = {
                AppSoundManager.playToggle(it)
                onUpdateSettings(settings.copy(incrementalIndexingEnabled = it))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // High-Performance Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Low-Memory Grid Decoding",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Decodes thumbnails with 50% less RAM (RGB_565) to prevent OutOfMemory during fast scrolling",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.highPerformanceMode,
              onCheckedChange = {
                AppSoundManager.playToggle(it)
                onUpdateSettings(settings.copy(highPerformanceMode = it))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Cloud AI Upload Consent Gate
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Cloud AI Upload Gate",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Never automatically upload photos; require explicit user confirmation before any Gemini API transmission",
                color = colors.secondaryText,
                fontSize = 12.sp
              )
            }
            Switch(
              checked = settings.showUploadConfirmation,
              onCheckedChange = {
                AppSoundManager.playToggle(it)
                onUpdateSettings(settings.copy(showUploadConfirmation = it))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
              )
            )
          }
        }
      }

      // 6. STORAGE & CACHE MANAGER
      item {
        SettingsSectionHeader(title = "Storage & Cache Manager", icon = Icons.Rounded.Storage)
        SettingsCard {
          Text(
            text = "Storage & Memory Attribution",
            color = colors.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))

          val thumbBytes = storageBreakdown?.thumbnailCacheBytes ?: (48L * 1024L * 1024L)
          val tempBytes = storageBreakdown?.let { it.tempFilesBytes + it.editorTempBytes + it.pdfTempBytes + it.batchTempBytes } ?: (16L * 1024L * 1024L)
          val vaultBytes = storageBreakdown?.vaultStorageBytes ?: (64L * 1024L * 1024L)
          val dbBytes = storageBreakdown?.appDatabaseBytes ?: (4L * 1024L * 1024L)

          val totalAllocated = (thumbBytes + tempBytes + vaultBytes + dbBytes).coerceAtLeast(1L).toFloat()

          // Proportional breakdown bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
          ) {
            Box(modifier = Modifier.weight((thumbBytes / totalAllocated).coerceAtLeast(0.05f)).fillMaxSize().background(colors.accent))
            Box(modifier = Modifier.weight((tempBytes / totalAllocated).coerceAtLeast(0.05f)).fillMaxSize().background(Color(0xFF8B5CF6)))
            Box(modifier = Modifier.weight((vaultBytes / totalAllocated).coerceAtLeast(0.05f)).fillMaxSize().background(Color(0xFFF59E0B)))
            Box(modifier = Modifier.weight((dbBytes / totalAllocated).coerceAtLeast(0.05f)).fillMaxSize().background(Color(0xFF10B981)))
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Thumbs (${storageBreakdown?.formatBytes(thumbBytes) ?: "48 MB"})", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("Temp (${storageBreakdown?.formatBytes(tempBytes) ?: "16 MB"})", color = Color(0xFF8B5CF6), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("Vault (${storageBreakdown?.formatBytes(vaultBytes) ?: "64 MB"})", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("DB (${storageBreakdown?.formatBytes(dbBytes) ?: "4 MB"})", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Medium)
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 1. Thumbnail Cache Cleanup
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Thumbnail Cache",
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "${storageBreakdown?.formatBytes(thumbBytes) ?: "48 MB"} • Regenerates on viewing",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }
            Button(
              onClick = { showClearCacheConfirm = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.primaryText
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.CleaningServices,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear Cache", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 2. Temporary Processing Files Cleanup
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Temporary Pipeline Files",
                color = colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "${storageBreakdown?.formatBytes(tempBytes) ?: "16 MB"} • Editor, PDF & batch buffers",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }
            Button(
              onClick = { showCleanTempConfirm = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.primaryText
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.CleaningServices,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clean Temp", fontSize = 12.sp)
            }
          }

          if (onRestoreExcluded != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Hidden / Excluded Items",
                  color = colors.primaryText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "Unhide items removed via 'Remove from My Gallery'",
                  color = colors.secondaryText,
                  fontSize = 11.sp
                )
              }
              Button(
                onClick = onRestoreExcluded,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = colors.surfaceVariant,
                  contentColor = colors.accent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text("Restore All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      // 6. ABOUT
      item {
        SettingsSectionHeader(title = "About", icon = Icons.Rounded.Info)
        SettingsCard {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.accentGlow),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "My Gallery AI",
              color = colors.primaryText,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = "Created by Amit Meena",
              color = colors.accent,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )

            Text(
              text = "Version 1.0.0 (Build 100) • Prompt 1 Foundation",
              color = colors.secondaryText,
              fontSize = 11.sp,
              modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "A modern, offline-first Android gallery designed for lightning speed, zero privacy compromises, and next-generation AI intelligence.",
              color = colors.secondaryText,
              fontSize = 11.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 16.sp
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }

  // Gemini API Key Dialog
  if (showApiKeyDialog) {
    Dialog(onDismissRequest = { showApiKeyDialog = false }) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Gemini API Configuration",
            color = colors.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Enter a custom key or use the Secrets Panel in AI Studio.",
            color = colors.secondaryText,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = { Text("AIzaSy...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = { showApiKeyDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.primaryText)
            ) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                onUpdateSettings(settings.copy(geminiApiKey = apiKeyInput))
                showApiKeyDialog = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White)
            ) {
              Text("Save")
            }
          }
        }
      }
    }
  }

  // Clear Cache Confirmation Dialog
  if (showClearCacheConfirm) {
    AlertDialog(
      onDismissRequest = { showClearCacheConfirm = false },
      title = { Text("Clear Thumbnail Cache?") },
      text = {
        Text("This will purge locally cached image thumbnails to reclaim storage. Your original photos and videos are NEVER deleted. Thumbnails will automatically regenerate when you view photos.")
      },
      confirmButton = {
        Button(
          onClick = {
            showClearCacheConfirm = false
            onClearCache()
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) {
          Text("Clear Cache")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearCacheConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Clean Temporary Files Confirmation Dialog
  if (showCleanTempConfirm) {
    AlertDialog(
      onDismissRequest = { showCleanTempConfirm = false },
      title = { Text("Clean Temporary Files?") },
      text = {
        Text("This will clean temporary export renders, PDF staging files, and background batch buffers. Exported PDFs, saved edits, and original photos are 100% safe and intact.")
      },
      confirmButton = {
        Button(
          onClick = {
            showCleanTempConfirm = false
            onCleanTemporaryFiles?.invoke()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
          Text("Clean Files")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCleanTempConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Clear AI Metadata Confirmation Dialog
  if (showClearAiCacheConfirm) {
    AlertDialog(
      onDismissRequest = { showClearAiCacheConfirm = false },
      title = { Text("Clear AI Metadata Cache?") },
      text = {
        Text("This deletes cached OCR text, document categories, and AI descriptions without deleting any photos. Note: Vault items are never stored in this cache.")
      },
      confirmButton = {
        Button(
          onClick = {
            showClearAiCacheConfirm = false
            onClearAiMetadata?.invoke()
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.warning)
        ) {
          Text("Clear AI Cache")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAiCacheConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun SettingsSectionHeader(
  title: String,
  icon: ImageVector
) {
  val colors = GalleryTheme.colors
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(bottom = 6.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = colors.accent,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = title,
      color = colors.primaryText,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun SettingsCard(
  content: @Composable () -> Unit
) {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surface)
      .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Column {
      content()
    }
  }
}

@Composable
private fun ThemeOptionButton(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) colors.accent else colors.surfaceVariant)
      .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = if (isSelected) Color.White else colors.primaryText,
      fontSize = 12.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )
  }
}
