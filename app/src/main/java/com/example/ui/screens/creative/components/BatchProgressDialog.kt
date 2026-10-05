package com.example.ui.screens.creative.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.creative.BatchJob
import com.example.model.creative.BatchJobStatus
import com.example.ui.theme.GalleryTheme

@Composable
fun BatchProgressDialog(
  job: BatchJob,
  onOpenResultsInGallery: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val isCompleted = job.status == BatchJobStatus.COMPLETED
  val isFailed = job.status == BatchJobStatus.FAILED
  val isRunning = job.status == BatchJobStatus.RUNNING

  val progress = if (job.totalItems > 0) {
    (job.completedItems + job.failedItems).toFloat() / job.totalItems.toFloat()
  } else 0f

  Dialog(
    onDismissRequest = { if (!isRunning) onDismiss() },
    properties = DialogProperties(dismissOnBackPress = !isRunning, dismissOnClickOutside = !isRunning)
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = colors.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .testTag("dialog_batch_progress")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon Status Indicator
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(
              when {
                isCompleted -> colors.success.copy(alpha = 0.15f)
                isFailed -> colors.error.copy(alpha = 0.15f)
                else -> colors.accent.copy(alpha = 0.15f)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          when {
            isCompleted -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(32.dp))
            isFailed -> Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(32.dp))
            else -> CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp, color = colors.accent)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = when {
            isCompleted -> "${job.operation.title} Completed!"
            isFailed -> "${job.operation.title} Failed"
            else -> "Processing ${job.operation.title}..."
          },
          color = colors.primaryText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = when {
            isCompleted -> "Successfully processed ${job.completedItems} of ${job.totalItems} files."
            isFailed -> "Encountered issues during batch processing."
            else -> "Processed ${job.completedItems} of ${job.totalItems} files (${(progress * 100).toInt()}%)"
          },
          color = colors.secondaryText,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Bar
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = if (isCompleted) colors.success else colors.accent,
          trackColor = colors.surfaceVariant
        )

        if (isRunning && job.currentFileName.isNotBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Current: ${job.currentFileName}",
            color = colors.secondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Error log summary if any items failed
        if (job.errorDetails.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(colors.error.copy(alpha = 0.1f))
              .padding(10.dp)
          ) {
            Column {
              Text("${job.failedItems} items had warnings/errors:", color = colors.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              job.errorDetails.take(3).forEach { err ->
                Text("• $err", color = colors.secondaryText, fontSize = 10.sp, maxLines = 1)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Actions
        if (!isRunning) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Close", color = colors.secondaryText)
            }

            if (isCompleted && job.outputUris.isNotEmpty()) {
              Button(
                onClick = {
                  onDismiss()
                  onOpenResultsInGallery()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.weight(1.3f).testTag("btn_view_batch_results")
              ) {
                Text("View Results", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
