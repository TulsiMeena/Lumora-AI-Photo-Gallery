package com.example.ui.screens.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.vault.VaultAlbumItem
import com.example.ui.theme.GalleryTheme

@Composable
fun MoveToVaultConfirmDialog(
  itemCount: Int,
  availableAlbums: List<VaultAlbumItem>,
  onConfirm: (isMoveAction: Boolean, albumId: String) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  var isMoveAction by remember { mutableStateOf(true) }
  var selectedAlbumId by remember { mutableStateOf("personal") }
  var showAlbumDropdown by remember { mutableStateOf(false) }

  val selectedAlbumName = availableAlbums.find { it.id == selectedAlbumId }?.name ?: "Personal"

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
        .testTag("dialog_move_to_vault_confirm")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Lock Icon Header
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = if (itemCount > 1) "Add $itemCount Items to Vault?" else "Add to Private Vault?",
          color = colors.primaryText,
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Selected media will be encrypted with hardware-backed AES-256 GCM in app-private storage.",
          color = colors.secondaryText,
          fontSize = 12.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target Vault Album Selector
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showAlbumDropdown = true }
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text("Target Album", color = colors.secondaryText, fontSize = 10.sp)
                Text(selectedAlbumName, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }

            Box {
              Text("Select", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              DropdownMenu(
                expanded = showAlbumDropdown,
                onDismissRequest = { showAlbumDropdown = false }
              ) {
                availableAlbums.forEach { album ->
                  DropdownMenuItem(
                    text = { Text(album.name) },
                    onClick = {
                      selectedAlbumId = album.id
                      showAlbumDropdown = false
                    }
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Move vs Copy Radio Options
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.background)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Option 1: Move (Verified cleanup)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { isMoveAction = true },
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = isMoveAction,
              onClick = { isMoveAction = true },
              colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text("Move to Vault (Recommended)", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Only removes original from public gallery after verified decryption test", color = colors.secondaryText, fontSize = 10.sp)
            }
          }

          // Option 2: Copy
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { isMoveAction = false },
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = !isMoveAction,
              onClick = { isMoveAction = false },
              colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text("Copy to Vault", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Keeps original media in your normal public gallery", color = colors.secondaryText, fontSize = 10.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", color = colors.secondaryText)
          }

          Button(
            onClick = {
              onConfirm(isMoveAction, selectedAlbumId)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_confirm_add_vault")
          ) {
            Text(if (isMoveAction) "Move" else "Copy", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
