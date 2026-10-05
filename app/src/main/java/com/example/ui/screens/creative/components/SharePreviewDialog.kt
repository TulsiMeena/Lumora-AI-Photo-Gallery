package com.example.ui.screens.creative.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.model.creative.ShareMode
import com.example.ui.theme.GalleryTheme

@Composable
fun SharePreviewDialog(
  items: List<MediaItem>,
  isFromVault: Boolean = false,
  onConfirmShare: (ShareMode) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  var selectedMode by remember { mutableStateOf(ShareMode.ORIGINAL) }
  var vaultConfirmed by remember { mutableStateOf(!isFromVault) }

  val totalBytes = items.sumOf { it.sizeBytes }
  val formattedTotalSize = when {
    totalBytes >= 1024 * 1024 -> "%.1f MB".format(totalBytes / (1024.0 * 1024.0))
    totalBytes >= 1024 -> "%.0f KB".format(totalBytes / 1024.0)
    else -> "$totalBytes B"
  }

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
        .testTag("dialog_share_preview")
    ) {
      Column(
        modifier = Modifier.padding(22.dp)
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
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Advanced Share Center",
                color = colors.primaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${items.size} ${if (items.size == 1) "file" else "files"} selected • ~$formattedTotalSize",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Media Horizontal Carousel
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(items, key = { it.id }) { item ->
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
              if (!item.uri.isNullOrBlank()) {
                AsyncImage(
                  model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(160, 160)
                    .crossfade(true)
                    .build(),
                  contentDescription = item.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Icon(Icons.Rounded.Photo, contentDescription = null, tint = colors.secondaryText)
                }
              }

              Box(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .clip(RoundedCornerShape(topEnd = 6.dp))
                  .background(Color.Black.copy(alpha = 0.65f))
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(
                  text = item.formattedSize,
                  color = Color.White,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Choose Quality & Export Mode",
          color = colors.primaryText,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Share Options
        ShareMode.entries.forEach { mode ->
          val isSelected = selectedMode == mode
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, colors.accent) else null,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable { selectedMode = mode }
              .testTag("share_mode_${mode.name}")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { selectedMode = mode },
                colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = mode.label,
                  color = colors.primaryText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = mode.subtitle,
                  color = colors.secondaryText,
                  fontSize = 11.sp
                )
              }
            }
          }
        }

        // Vault Warning Section if sharing from Vault
        if (isFromVault) {
          Spacer(modifier = Modifier.height(14.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(colors.warning.copy(alpha = 0.15f))
              .border(1.dp, colors.warning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Private Vault Notice",
                  color = colors.warning,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sharing these items decrypts a copy that leaves your protected Vault. The receiving application will have full access.",
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { vaultConfirmed = !vaultConfirmed }
              ) {
                Checkbox(
                  checked = vaultConfirmed,
                  onCheckedChange = { vaultConfirmed = it },
                  colors = CheckboxDefaults.colors(checkedColor = colors.warning)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "I understand and confirm export",
                  color = colors.primaryText,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
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
              if (vaultConfirmed) {
                onConfirmShare(selectedMode)
              }
            },
            enabled = vaultConfirmed,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier.weight(1.3f).testTag("btn_confirm_share_now")
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Now", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
