package com.example.ui.screens.resize.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.resize.ResizeExecutionResult
import com.example.ui.theme.GalleryTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResizeSuccessSheet(
  result: ResizeExecutionResult,
  onDismiss: () -> Unit,
  onOpenInViewer: (Uri) -> Unit,
  onOpenInProEditor: (Uri) -> Unit,
  onCreatePdf: (Uri) -> Unit,
  onResizeAgain: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(Color(0xFF10B981).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = Color(0xFF10B981),
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Resized & Saved Successfully!",
        color = colors.primaryText,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Original image was kept 100% untouched. Saved as new file.",
        color = colors.secondaryText,
        fontSize = 12.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Specs summary card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(14.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dimensions:", color = colors.secondaryText, fontSize = 12.sp)
            Text("${result.width} × ${result.height} px", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Resolution:", color = colors.secondaryText, fontSize = 12.sp)
            Text("${result.dpi} DPI", color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Format:", color = colors.secondaryText, fontSize = 12.sp)
            Text(result.outputFormat.displayName, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("File Size:", color = colors.secondaryText, fontSize = 12.sp)
            val kb = result.fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            val sz = if (mb >= 1.0) "%.2f MB".format(mb) else "%.1f KB".format(kb)
            Text(sz, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Location:", color = colors.secondaryText, fontSize = 12.sp)
            Text("Pictures/My Gallery AI/Resized", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = {
            val file = File(result.outputFilePath)
            val contentUri = try {
              FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
              Uri.parse(result.outputUri)
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = result.outputFormat.mimeType
              putExtra(Intent.EXTRA_STREAM, contentUri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Resized Image"))
          },
          modifier = Modifier.weight(1f).testTag("btn_share_resized"),
          colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share")
        }

        FilledTonalButton(
          onClick = {
            onDismiss()
            onOpenInViewer(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f).testTag("btn_open_resized"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Open")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenInProEditor(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pro Edit")
        }

        OutlinedButton(
          onClick = {
            onDismiss()
            onCreatePdf(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Make PDF")
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = {
          onDismiss()
          onResizeAgain()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Resize Another / Adjust")
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
