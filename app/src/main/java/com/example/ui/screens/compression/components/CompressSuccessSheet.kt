package com.example.ui.screens.compression.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.compression.CompressionExecutionResult
import com.example.model.compression.formatFileSize
import com.example.ui.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressSuccessSheet(
  result: CompressionExecutionResult,
  onDismiss: () -> Unit,
  onOpenInViewer: (Uri) -> Unit,
  onOpenInEditor: (Uri) -> Unit,
  onOpenInResize: (Uri) -> Unit,
  onCreatePdf: (Uri) -> Unit,
  onCompressAgain: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface,
    modifier = Modifier.testTag("sheet_compress_success")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Success Header Icon
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

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Compression Complete!",
        color = colors.primaryText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = result.statusMessage,
        color = if (result.targetAchieved) Color(0xFF10B981) else colors.warning,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 2.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Results Comparison Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Original", color = colors.secondaryText, fontSize = 11.sp)
              Text(
                text = formatFileSize(result.originalSizeBytes),
                color = colors.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${result.originalWidth} × ${result.originalHeight} px",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }

            // Big Savings Badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.18f))
                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "-${result.reductionPercent}%",
                  color = Color(0xFF10B981),
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Black
                )
                Text(
                  text = "SAVED",
                  color = Color(0xFF10B981),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text("Output (${result.outputFormat.displayName})", color = colors.secondaryText, fontSize = 11.sp)
              Text(
                text = formatFileSize(result.outputSizeBytes),
                color = Color(0xFF0EA5E9),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${result.outputWidth} × ${result.outputHeight} px",
                color = colors.secondaryText,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Saved path disclaimer
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(colors.background)
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "Saved as new file in: Pictures/My Gallery AI/Compressed\nOriginal remains untouched.",
              color = colors.secondaryText,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Primary Actions Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ActionButton(
          label = "Open",
          icon = Icons.Rounded.OpenInNew,
          onClick = {
            onDismiss()
            onOpenInViewer(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f)
        )
        ActionButton(
          label = "Share",
          icon = Icons.Rounded.Share,
          onClick = {
            try {
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = result.outputFormat.mimeType
                putExtra(Intent.EXTRA_STREAM, Uri.parse(result.outputUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(intent, "Share Compressed Image"))
            } catch (e: Exception) {
              e.printStackTrace()
            }
          },
          modifier = Modifier.weight(1f)
        )
        ActionButton(
          label = "Create PDF",
          icon = Icons.Rounded.PictureAsPdf,
          onClick = {
            onDismiss()
            onCreatePdf(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ActionButton(
          label = "Pro Editor",
          icon = Icons.Rounded.Tune,
          onClick = {
            onDismiss()
            onOpenInEditor(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f)
        )
        ActionButton(
          label = "Resize & DPI",
          icon = Icons.Rounded.Crop,
          onClick = {
            onDismiss()
            onOpenInResize(Uri.parse(result.outputUri))
          },
          modifier = Modifier.weight(1f)
        )
        ActionButton(
          label = "Compress More",
          icon = Icons.Rounded.Refresh,
          onClick = {
            onDismiss()
            onCompressAgain()
          },
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_dismiss_success")
      ) {
        Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
      }
    }
  }
}

@Composable
private fun ActionButton(
  label: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surfaceVariant)
      .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(imageVector = icon, contentDescription = label, tint = colors.accent, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = label, color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}
