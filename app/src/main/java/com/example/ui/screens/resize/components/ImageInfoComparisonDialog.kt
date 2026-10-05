package com.example.ui.screens.resize.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.resize.ImageMetadataInfo
import com.example.model.resize.OutputFormat
import com.example.ui.theme.GalleryTheme

@Composable
fun ImageInfoComparisonDialog(
  originalInfo: ImageMetadataInfo,
  outputWidth: Int,
  outputHeight: Int,
  outputDpi: Int,
  outputFormat: OutputFormat,
  estimatedSizeBytes: Long,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  var selectedTab by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.CompareArrows,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Image Specifications",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = colors.primaryText
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Color.Transparent,
          contentColor = colors.accent
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Comparison", fontSize = 13.sp) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Original Info", fontSize = 13.sp) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("Output Info", fontSize = 13.sp) }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
          0 -> {
            // Side-by-Side Comparison
            Card(
              colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComparisonRow(
                  label = "Dimensions",
                  orig = "${originalInfo.widthPx} × ${originalInfo.heightPx} px",
                  out = "$outputWidth × $outputHeight px"
                )
                Divider(color = colors.divider.copy(alpha = 0.5f))
                ComparisonRow(
                  label = "Format",
                  orig = originalInfo.formatName,
                  out = outputFormat.displayName
                )
                Divider(color = colors.divider.copy(alpha = 0.5f))
                ComparisonRow(
                  label = "DPI / PPI",
                  orig = "${originalInfo.existingDpi} DPI",
                  out = "$outputDpi DPI"
                )
                Divider(color = colors.divider.copy(alpha = 0.5f))
                val estKb = estimatedSizeBytes / 1024.0
                val estMb = estKb / 1024.0
                val estStr = if (estMb >= 1.0) "≈ %.2f MB".format(estMb) else "≈ %.1f KB".format(estKb)
                ComparisonRow(
                  label = "File Size",
                  orig = originalInfo.formattedSize,
                  out = estStr
                )
              }
            }
          }

          1 -> {
            // Original Details
            Card(
              colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoItem("File Name", originalInfo.fileName)
                InfoItem("Format", originalInfo.formatName)
                InfoItem("Pixel Dimensions", "${originalInfo.widthPx} × ${originalInfo.heightPx} px")
                InfoItem("Aspect Ratio", originalInfo.aspectRatioStr)
                InfoItem("File Size", originalInfo.formattedSize)
                InfoItem("DPI Metadata", "${originalInfo.existingDpi} DPI")
                InfoItem("Physical Print Size", originalInfo.getPhysicalDimensions(originalInfo.existingDpi))
                InfoItem("Alpha / Transparency", if (originalInfo.hasAlpha) "Yes (Transparent supported)" else "No (Opaque RGB)")
                InfoItem("Color Space", originalInfo.colorSpace)
              }
            }
          }

          2 -> {
            // Output Details
            Card(
              colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoItem("Output Format", "${outputFormat.displayName} (${outputFormat.extension})")
                InfoItem("Target Dimensions", "$outputWidth × $outputHeight px")
                val widthIn = outputWidth.toDouble() / outputDpi
                val heightIn = outputHeight.toDouble() / outputDpi
                val widthCm = widthIn * 2.54
                val heightCm = heightIn * 2.54
                InfoItem("Print Size (cm)", "%.2f × %.2f cm".format(widthCm, heightCm))
                InfoItem("Print Size (inches)", "%.2f × %.2f in".format(widthIn, heightIn))
                InfoItem("Print Size (mm)", "%.1f × %.1f mm".format(widthCm * 10, heightCm * 10))
                InfoItem("Print Resolution", "$outputDpi DPI")
                val estKb = estimatedSizeBytes / 1024.0
                val estMb = estKb / 1024.0
                val estStr = if (estMb >= 1.0) "≈ %.2f MB".format(estMb) else "≈ %.1f KB".format(estKb)
                InfoItem("Estimated Size", "$estStr (Approximation)")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Done")
      }
    },
    containerColor = colors.surface
  )
}

@Composable
private fun ComparisonRow(label: String, orig: String, out: String) {
  val colors = GalleryTheme.colors
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(text = label, color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(2.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(text = "Orig: $orig", color = colors.primaryText, fontSize = 12.sp)
      Text(text = "New: $out", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun InfoItem(label: String, value: String) {
  val colors = GalleryTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = colors.secondaryText, fontSize = 12.sp)
    Text(text = value, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
  }
}
