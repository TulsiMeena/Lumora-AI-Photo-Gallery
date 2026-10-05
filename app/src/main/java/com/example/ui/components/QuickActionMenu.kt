package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GalleryTheme

sealed class QuickAction(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val tag: String
) {
  object Camera : QuickAction("Camera", "Take photo or video", Icons.Rounded.CameraAlt, "quick_action_camera")
  object ScanDocument : QuickAction("Scan Document", "Perspective crop & OCR", Icons.Rounded.DocumentScanner, "quick_action_scan")
  object Import : QuickAction("Import Media", "From device or storage", Icons.Rounded.FileDownload, "quick_action_import")
  object CreatePdf : QuickAction("Create PDF", "Compile images into PDF", Icons.Rounded.PictureAsPdf, "quick_action_pdf")
  object PhotoEditor : QuickAction("Photo Editor Pro", "Adjust, crop, curves, HSL", Icons.Rounded.Tune, "quick_action_editor")
  object EditingProjects : QuickAction("Edit Projects", "Resume unfinished sessions", Icons.Rounded.HistoryEdu, "quick_action_projects")
  object ResizeImage : QuickAction("Resize & DPI Studio", "Dimensions, DPI, units & presets", Icons.Rounded.Crop, "quick_action_resize")
  object CompressImage : QuickAction("Compress & Convert", "Target size, quality & formats", Icons.Rounded.Compress, "quick_action_compress")
  object PassportStudio : QuickAction("Passport Photo Studio", "Exact sizes, guides & print sheets", Icons.Rounded.AccountBox, "quick_action_passport")
  object AiEdit : QuickAction("AI Edit", "Smart enhance & erase", Icons.Rounded.AutoFixHigh, "quick_action_ai_edit")
  object CreativeStudio : QuickAction("Creative Studio", "Collage, poster, watermark, GIF", Icons.Rounded.AutoAwesome, "quick_action_creative")
  object BatchTools : QuickAction("Batch Studio", "Process multiple photos at once", Icons.Rounded.Layers, "quick_action_batch")
}

@Composable
fun QuickActionOverlay(
  isExpanded: Boolean,
  onDismiss: () -> Unit,
  onActionClick: (QuickAction) -> Unit
) {
  if (!isExpanded) return

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f))
      .clickable { onDismiss() }
      .testTag("quick_action_backdrop")
  )
}

@Composable
fun QuickActionMenu(
  isExpanded: Boolean,
  onToggle: () -> Unit,
  onActionClick: (QuickAction) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  val rotation by animateFloatAsState(
    targetValue = if (isExpanded) 45f else 0f,
    label = "fab_rotation"
  )

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AnimatedVisibility(
      visible = isExpanded,
      enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
      exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionItemRow(
          action = QuickAction.ScanDocument,
          onClick = { onActionClick(QuickAction.ScanDocument) }
        )
        QuickActionItemRow(
          action = QuickAction.CreativeStudio,
          onClick = { onActionClick(QuickAction.CreativeStudio) }
        )
        QuickActionItemRow(
          action = QuickAction.BatchTools,
          onClick = { onActionClick(QuickAction.BatchTools) }
        )
        QuickActionItemRow(
          action = QuickAction.Camera,
          onClick = { onActionClick(QuickAction.Camera) }
        )
        QuickActionItemRow(
          action = QuickAction.Import,
          onClick = { onActionClick(QuickAction.Import) }
        )
        QuickActionItemRow(
          action = QuickAction.PhotoEditor,
          onClick = { onActionClick(QuickAction.PhotoEditor) }
        )
        QuickActionItemRow(
          action = QuickAction.ResizeImage,
          onClick = { onActionClick(QuickAction.ResizeImage) }
        )
        QuickActionItemRow(
          action = QuickAction.CompressImage,
          onClick = { onActionClick(QuickAction.CompressImage) }
        )
        QuickActionItemRow(
          action = QuickAction.PassportStudio,
          onClick = { onActionClick(QuickAction.PassportStudio) }
        )
        QuickActionItemRow(
          action = QuickAction.EditingProjects,
          onClick = { onActionClick(QuickAction.EditingProjects) }
        )
        QuickActionItemRow(
          action = QuickAction.CreatePdf,
          onClick = { onActionClick(QuickAction.CreatePdf) }
        )
        QuickActionItemRow(
          action = QuickAction.AiEdit,
          onClick = { onActionClick(QuickAction.AiEdit) }
        )
      }
    }

    FloatingActionButton(
      onClick = onToggle,
      shape = CircleShape,
      containerColor = colors.accent,
      contentColor = Color.White,
      elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
      modifier = Modifier
        .size(56.dp)
        .testTag("fab_quick_actions")
    ) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = "Quick Actions",
        modifier = Modifier
          .size(28.dp)
          .rotate(rotation)
      )
    }
  }
}

@Composable
private fun QuickActionItemRow(
  action: QuickAction,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.End,
    modifier = Modifier
      .clickable { onClick() }
      .testTag(action.tag)
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(colors.surface)
        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = action.title,
          color = colors.primaryText,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = action.subtitle,
          color = colors.secondaryText,
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(colors.surface)
        .border(1.dp, colors.border, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = action.icon,
        contentDescription = action.title,
        tint = colors.accent,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
