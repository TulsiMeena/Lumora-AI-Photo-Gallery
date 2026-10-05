package com.example.ui.screens.compression.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.compression.formatFileSize
import com.example.ui.theme.GalleryTheme

@Composable
fun CompareBeforeAfterDialog(
  originalUri: Uri?,
  originalSize: Long,
  originalDims: String,
  compressedBitmap: Bitmap?,
  compressedSize: Long,
  compressedDims: String,
  qualityUsed: Int,
  formatName: String,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  var isSplitView by remember { mutableStateOf(false) }
  var showCompressedInToggle by remember { mutableStateOf(true) }
  var splitSliderPos by remember { mutableFloatStateOf(0.5f) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.95f)),
      color = Color.Transparent
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.Compare,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Before & After Comparison",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Visual quality check vs file reduction",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("btn_close_comparison")
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        // Stats Comparison Bar
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Before Info
            Column {
              Text("BEFORE (Original)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text(formatFileSize(originalSize), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Text(originalDims, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            // Arrow & Savings
            val savedBytes = originalSize - compressedSize
            val percent = if (originalSize > 0 && savedBytes > 0) {
              ((savedBytes.toDouble() / originalSize.toDouble()) * 100).toInt()
            } else 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
              Text(
                text = "-$percent% SAVED",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }

            // After Info
            Column(horizontalAlignment = Alignment.End) {
              Text("AFTER ($formatName)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text(formatFileSize(compressedSize), color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Text("$compressedDims @ $qualityUsed% Q", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
          }
        }

        // Comparison Image Canvas Area
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (isSplitView) {
            // Side-by-side view
            Row(modifier = Modifier.fillMaxSize()) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxSize()
                  .border(0.5.dp, Color(0xFF334155)),
                contentAlignment = Alignment.Center
              ) {
                if (originalUri != null) {
                  AsyncImage(
                    model = originalUri,
                    contentDescription = "Original",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                  )
                }
                Box(
                  modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text("Original", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                if (compressedBitmap != null) {
                  androidx.compose.foundation.Image(
                    bitmap = compressedBitmap.asImageBitmap(),
                    contentDescription = "Compressed",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                  )
                }
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xFF0284C7).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text("Compressed", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          } else {
            // Toggle view (Click to switch between original and compressed)
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clickable { showCompressedInToggle = !showCompressedInToggle },
              contentAlignment = Alignment.Center
            ) {
              if (showCompressedInToggle && compressedBitmap != null) {
                androidx.compose.foundation.Image(
                  bitmap = compressedBitmap.asImageBitmap(),
                  contentDescription = "Compressed",
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxSize()
                )
              } else if (originalUri != null) {
                AsyncImage(
                  model = originalUri,
                  contentDescription = "Original",
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxSize()
                )
              }

              // Mode badge
              Box(
                modifier = Modifier
                  .align(Alignment.TopCenter)
                  .padding(12.dp)
                  .background(
                    if (showCompressedInToggle) Color(0xFF0284C7) else Color(0xFF475569),
                    RoundedCornerShape(20.dp)
                  )
                  .padding(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Text(
                  text = if (showCompressedInToggle) "Showing Compressed (Tap to toggle)" else "Showing Original (Tap to toggle)",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Toggle View Mode Button
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (!isSplitView) colors.accent else Color(0xFF1E293B))
              .clickable { isSplitView = false }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Tap-to-Toggle",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSplitView) colors.accent else Color(0xFF1E293B))
              .clickable { isSplitView = true }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Side-by-Side",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}
