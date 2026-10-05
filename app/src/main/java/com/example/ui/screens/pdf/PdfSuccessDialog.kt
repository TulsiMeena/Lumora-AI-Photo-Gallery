package com.example.ui.screens.pdf

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.pdf.PdfHistoryItem
import com.example.ui.theme.LocalGalleryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSuccessSheet(
  item: PdfHistoryItem,
  onDismiss: () -> Unit,
  onOpen: () -> Unit,
  onShare: () -> Unit,
  onRename: () -> Unit,
  onCreateAnother: () -> Unit
) {
  val colors = LocalGalleryColors.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surfaceCard,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Success Icon Badge
      Box(
        modifier = Modifier
          .size(68.dp)
          .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = "Success",
          tint = Color(0xFF10B981),
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "PDF Created Successfully!",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
      )

      Text(
        text = "Saved offline to device storage • Ready to share",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(20.dp))

      // PDF Info Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = colors.cardBackground
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.PictureAsPdf,
              contentDescription = "PDF Icon",
              tint = Color(0xFFEF4444),
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.fileName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = colors.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${item.pageCount} ${if (item.pageCount == 1) "Page" else "Pages"}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
              )
              Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
              )
              Text(
                text = item.formattedSize,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
              )
              Text(
                text = item.pageSizeName,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Primary Action: Open PDF
      Button(
        onClick = onOpen,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("open_pdf_btn"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(
          imageVector = Icons.Rounded.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Open PDF",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Secondary Actions: Share & Rename
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        FilledTonalButton(
          onClick = onShare,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("share_pdf_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share")
        }

        FilledTonalButton(
          onClick = onRename,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("rename_pdf_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Rename")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Tertiary Action: Create Another PDF
      OutlinedButton(
        onClick = onCreateAnother,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Create Another PDF")
      }
    }
  }
}
