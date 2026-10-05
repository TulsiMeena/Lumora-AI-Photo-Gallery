package com.example.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.editor.ExportFormat
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
  isExporting: Boolean,
  onDismiss: () -> Unit,
  onConfirmExport: (ExportFormat, Int) -> Unit
) {
  var selectedFormat by remember { mutableStateOf(ExportFormat.JPEG) }
  var quality by remember { mutableFloatStateOf(92f) }

  Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF1E222D),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("dialog_export_image")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Rounded.SaveAlt,
            contentDescription = null,
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Save as New Image",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Format selection
        Text(
          text = "IMAGE FORMAT",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ExportFormat.values().forEach { format ->
            val isSelected = format == selectedFormat
            FilterChip(
              selected = isSelected,
              onClick = { selectedFormat = format },
              label = { Text(format.displayName, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF3B82F6),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF2B3242),
                labelColor = Color(0xFFCBD5E1)
              ),
              border = null,
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quality slider (JPEG/WebP)
        if (selectedFormat != ExportFormat.PNG) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Export Quality",
              color = Color(0xFFCBD5E1),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "${quality.roundToInt()}%",
              color = Color(0xFF60A5FA),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Slider(
            value = quality,
            onValueChange = { quality = it },
            valueRange = 50f..100f,
            colors = SliderDefaults.colors(
              thumbColor = Color.White,
              activeTrackColor = Color(0xFF3B82F6),
              inactiveTrackColor = Color(0xFF334155)
            )
          )
        } else {
          Text(
            text = "PNG uses 100% lossless compression",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Non-destructive notice
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10B981).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Text(
            text = "✓ Non-destructive: Your original photo remains untouched. A new high-res image will be saved to Pictures/MyGalleryAI.",
            color = Color(0xFF6EE7B7),
            fontSize = 12.sp
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onDismiss,
            enabled = !isExporting
          ) {
            Text("Cancel", color = Color(0xFF94A3B8))
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = { onConfirmExport(selectedFormat, quality.roundToInt()) },
            enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            modifier = Modifier.testTag("btn_confirm_export")
          ) {
            if (isExporting) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Exporting...", fontSize = 13.sp)
            } else {
              Text("Export & Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}
