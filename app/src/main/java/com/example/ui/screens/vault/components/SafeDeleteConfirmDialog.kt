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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.MediaItem
import com.example.ui.theme.GalleryTheme

@Composable
fun SafeDeleteConfirmDialog(
  mediaItems: List<MediaItem>,
  onRemoveFromAppOnly: () -> Unit,
  onDeleteFromDevice: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val count = mediaItems.size
  val titleText = if (count > 1) "Manage $count Selected Items" else "Manage Photo/Video"

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
        .testTag("dialog_safe_delete_confirm")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Warning Icon
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(colors.warning.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Rounded.Warning, contentDescription = null, tint = colors.warning, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = titleText,
          color = colors.primaryText,
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Choose how you want to handle these files. Deletion from device cannot be undone.",
          color = colors.secondaryText,
          fontSize = 12.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Action 1: Remove from My Gallery ONLY (Safe, preserves original file on phone)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              onDismiss()
              onRemoveFromAppOnly()
            }
            .testTag("btn_remove_from_app_only")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Remove from My Gallery", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Only hides from this app. Original stays safely on your phone.", color = colors.secondaryText, fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action 2: Delete from Device (Permanent MediaStore delete)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = colors.warning.copy(alpha = 0.12f)),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.warning.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable {
              onDismiss()
              onDeleteFromDevice()
            }
            .testTag("btn_delete_from_device")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.warning.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = colors.warning, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Delete from Device", color = colors.warning, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Permanently deletes the original file from phone storage.", color = colors.primaryText.copy(alpha = 0.8f), fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Cancel", color = colors.secondaryText)
        }
      }
    }
  }
}
