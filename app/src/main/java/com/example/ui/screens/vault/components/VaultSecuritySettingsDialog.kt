package com.example.ui.screens.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.vault.VaultSecurityConfig
import com.example.ui.theme.GalleryTheme

@Composable
fun VaultSecuritySettingsDialog(
  config: VaultSecurityConfig,
  isBiometricHardwareAvailable: Boolean,
  onUpdateConfig: (VaultSecurityConfig) -> Unit,
  onChangePinClick: () -> Unit,
  onLockVaultNow: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val scrollState = rememberScrollState()

  var isBiometricEnabled by remember { mutableStateOf(config.isBiometricEnabled) }
  var autoLockSeconds by remember { mutableStateOf(config.autoLockTimeoutSeconds) }
  var isSecureScreenEnabled by remember { mutableStateOf(config.isSecureScreenEnabled) }
  var askBeforeAiUpload by remember { mutableStateOf(config.askBeforeAiUpload) }
  var exportWarningEnabled by remember { mutableStateOf(config.exportWarningEnabled) }

  var showAutoLockDropdown by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = colors.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .testTag("dialog_vault_security_settings")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(scrollState)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Vault Security", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              Text("Privacy & protection settings", color = colors.secondaryText, fontSize = 11.sp)
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.primaryText)
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1. PIN Management
        SettingsActionCard(
          icon = Icons.Rounded.Pin,
          title = "Change Vault PIN",
          subtitle = "Update your 4 to 8 digit access PIN",
          onClick = {
            onDismiss()
            onChangePinClick()
          }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Biometric Unlock Toggle
        if (isBiometricHardwareAvailable) {
          SettingsToggleCard(
            icon = Icons.Rounded.Fingerprint,
            title = "Biometric Unlock",
            subtitle = "Use fingerprint or face recognition",
            checked = isBiometricEnabled,
            onCheckedChange = {
              isBiometricEnabled = it
              onUpdateConfig(config.copy(isBiometricEnabled = it))
            }
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Auto-Lock Timeout Selector
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showAutoLockDropdown = true }
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Rounded.Timer, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Auto-Lock Timeout", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                val label = when (autoLockSeconds) {
                  0 -> "Immediately on background"
                  30 -> "30 seconds"
                  60 -> "1 minute (Default)"
                  300 -> "5 minutes"
                  900 -> "15 minutes"
                  else -> "$autoLockSeconds seconds"
                }
                Text(label, color = colors.secondaryText, fontSize = 11.sp)
              }
            }

            Box {
              Text("Change", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              DropdownMenu(
                expanded = showAutoLockDropdown,
                onDismissRequest = { showAutoLockDropdown = false }
              ) {
                listOf(
                  0 to "Immediately",
                  30 to "30 seconds",
                  60 to "1 minute (Default)",
                  300 to "5 minutes",
                  900 to "15 minutes"
                ).forEach { (sec, text) ->
                  DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                      showAutoLockDropdown = false
                      autoLockSeconds = sec
                      onUpdateConfig(config.copy(autoLockTimeoutSeconds = sec))
                    }
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Screen Capture Protection (FLAG_SECURE)
        SettingsToggleCard(
          icon = Icons.Rounded.Screenshot,
          title = "Screen Capture Protection",
          subtitle = "Blocks screenshots and hides recent-app previews",
          checked = isSecureScreenEnabled,
          onCheckedChange = {
            isSecureScreenEnabled = it
            onUpdateConfig(config.copy(isSecureScreenEnabled = it))
          }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Ask before AI Upload Confirmation
        SettingsToggleCard(
          icon = Icons.Rounded.AutoFixHigh,
          title = "Ask Before AI Processing",
          subtitle = "Requires explicit consent before sending any vault image to AI",
          checked = askBeforeAiUpload,
          onCheckedChange = {
            askBeforeAiUpload = it
            onUpdateConfig(config.copy(askBeforeAiUpload = it))
          }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Export Warning Confirmation
        SettingsToggleCard(
          icon = Icons.Rounded.Warning,
          title = "Confirm Unprotected Export",
          subtitle = "Warns when moving or sharing media outside the vault",
          checked = exportWarningEnabled,
          onCheckedChange = {
            exportWarningEnabled = it
            onUpdateConfig(config.copy(exportWarningEnabled = it))
          }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Lock Vault Now Button
        Button(
          onClick = {
            onDismiss()
            onLockVaultNow()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colors.warning),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("btn_lock_vault_now")
        ) {
          Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Lock Vault Now", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SettingsActionCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Text(subtitle, color = colors.secondaryText, fontSize = 11.sp)
        }
      }
      Text("Change", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun SettingsToggleCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  val colors = GalleryTheme.colors
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Text(subtitle, color = colors.secondaryText, fontSize = 11.sp)
        }
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
      )
    }
  }
}
