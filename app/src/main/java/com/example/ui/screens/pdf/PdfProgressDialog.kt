package com.example.ui.screens.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.pdf.PdfGenerationProgress
import com.example.ui.theme.LocalGalleryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfProgressDialog(
  progress: PdfGenerationProgress,
  onCancel: () -> Unit
) {
  val colors = LocalGalleryColors.current

  BasicAlertDialog(
    onDismissRequest = { /* Prevent dismiss while processing */ }
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = colors.surfaceCard)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.PictureAsPdf,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Creating PDF...",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = progress.statusMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        LinearProgressIndicator(
          progress = { (progress.percent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = colors.divider
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "${progress.percent}% Completed • Page ${progress.currentPage} of ${progress.totalPages}",
          style = MaterialTheme.typography.labelMedium,
          color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        TextButton(
          onClick = onCancel,
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Cancel")
        }
      }
    }
  }
}
