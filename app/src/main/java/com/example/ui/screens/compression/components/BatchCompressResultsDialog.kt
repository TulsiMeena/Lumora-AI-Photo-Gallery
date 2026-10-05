package com.example.ui.screens.compression.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.compression.BatchCompressionItem
import com.example.model.compression.BatchItemStatus
import com.example.model.compression.formatFileSize
import com.example.ui.theme.GalleryTheme

@Composable
fun BatchCompressResultsDialog(
  results: List<BatchCompressionItem>,
  onRetryFailed: () -> Unit,
  onCreatePdfFromBatch: (List<Uri>) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current

  val successCount = results.count { it.status == BatchItemStatus.SUCCESS }
  val failedCount = results.count { it.status == BatchItemStatus.FAILED }
  val totalOrig = results.sumOf { it.originalSize }
  val totalOutput = results.filter { it.status == BatchItemStatus.SUCCESS }.sumOf { it.outputSize }
  val totalSavedBytes = if (totalOrig > totalOutput) totalOrig - totalOutput else 0L

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(colors.background),
      color = colors.background
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Batch Compression Report",
              color = colors.primaryText,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "$successCount succeeded • $failedCount failed",
              color = if (failedCount == 0) Color(0xFF10B981) else colors.warning,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("btn_close_batch_results")
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.primaryText)
          }
        }

        // Summary Card
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Total Original", color = colors.secondaryText, fontSize = 11.sp)
              Text(formatFileSize(totalOrig), color = colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.18f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = formatFileSize(totalSavedBytes),
                  color = Color(0xFF10B981),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black
                )
                Text(
                  text = "TOTAL SAVED",
                  color = Color(0xFF10B981),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text("New Total", color = colors.secondaryText, fontSize = 11.sp)
              Text(formatFileSize(totalOutput), color = Color(0xFF0EA5E9), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Image", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Original", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Output", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Saved", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        // List of items
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(results) { item ->
            BatchItemReportRow(item = item)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (failedCount > 0) {
            OutlinedButton(
              onClick = onRetryFailed,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Retry Failed ($failedCount)", fontSize = 12.sp)
            }
          }

          val successfulUris = results.filter { it.status == BatchItemStatus.SUCCESS && it.outputUri != null }
            .map { Uri.parse(it.outputUri) }

          if (successfulUris.isNotEmpty()) {
            OutlinedButton(
              onClick = {
                try {
                  val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(successfulUris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  }
                  context.startActivity(Intent.createChooser(shareIntent, "Share All Compressed Images"))
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Share All", fontSize = 12.sp)
            }

            Button(
              onClick = {
                onDismiss()
                onCreatePdfFromBatch(successfulUris)
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Create PDF", fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text("Done", color = colors.primaryText, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun BatchItemReportRow(item: BatchCompressionItem) {
  val colors = GalleryTheme.colors

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surface),
    modifier = Modifier
      .fillMaxWidth()
      .border(0.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        AsyncImage(
          model = item.outputUri ?: item.uri,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (item.status == BatchItemStatus.FAILED) {
            Text(
              text = item.errorMessage ?: "Failed",
              color = colors.warning,
              fontSize = 10.sp,
              maxLines = 1
            )
          } else {
            Text(
              text = "${item.outputWidth}×${item.outputHeight} • ${item.outputFormat}",
              color = colors.secondaryText,
              fontSize = 10.sp
            )
          }
        }
      }

      // Sizes & status
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = formatFileSize(item.originalSize),
          color = colors.secondaryText,
          fontSize = 11.sp
        )

        if (item.status == BatchItemStatus.SUCCESS) {
          Text(
            text = formatFileSize(item.outputSize),
            color = Color(0xFF0EA5E9),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF10B981).copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "-${item.reductionPercent}%",
              color = Color(0xFF10B981),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = "Failed",
            tint = colors.warning,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
