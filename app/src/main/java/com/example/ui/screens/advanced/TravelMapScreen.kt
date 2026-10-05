package com.example.ui.screens.advanced

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.advanced.LocationCluster
import com.example.ui.theme.GalleryTheme

@Composable
fun TravelMapScreen(
  clusters: List<LocationCluster>,
  onOpenPhoto: (MediaItem) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  var selectedCluster by remember { mutableStateOf(clusters.firstOrNull()) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // Header
    Surface(
      color = colors.surface,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.testTag("btn_map_back")
        ) {
          Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Travel & Journey Map", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          Text("${clusters.sumOf { it.mediaItems.size }} geotagged photos across ${clusters.size} destinations", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
      }
    }

    // Interactive Map Canvas Area
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(Color(0xFF0F172A))
    ) {
      // Stylized Tactical Map Graphic Plane
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(clusters) {
            detectTapGestures { offset ->
              // Hit test pins on canvas
              val width = size.width
              val height = size.height
              clusters.forEach { cluster ->
                val px = ((cluster.centerLng + 180.0) / 360.0 * width).toFloat()
                val py = ((90.0 - cluster.centerLat) / 180.0 * height).toFloat()
                val dx = offset.x - px
                val dy = offset.y - py
                if (dx * dx + dy * dy <= 45 * 45) {
                  selectedCluster = cluster
                }
              }
            }
          }
      ) {
        val width = size.width
        val height = size.height

        // Draw longitude / latitude grid lines
        val gridColor = Color(0xFF1E293B)
        for (i in 1..8) {
          val y = height * (i / 9f)
          drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
        }
        for (i in 1..8) {
          val x = width * (i / 9f)
          drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1.dp.toPx())
        }

        // Draw connecting flight / route arches between destinations
        val sortedClusters = clusters.take(6)
        if (sortedClusters.size >= 2) {
          val pathPoints = sortedClusters.map {
            Offset(
              ((it.centerLng + 180.0) / 360.0 * width).toFloat(),
              ((90.0 - it.centerLat) / 180.0 * height).toFloat()
            )
          }
          for (i in 0 until pathPoints.size - 1) {
            drawLine(
              color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
              start = pathPoints[i],
              end = pathPoints[i + 1],
              strokeWidth = 2.dp.toPx(),
              pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
          }
        }

        // Draw Location Cluster Radar Pins
        clusters.forEach { cluster ->
          val px = ((cluster.centerLng + 180.0) / 360.0 * width).toFloat()
          val py = ((90.0 - cluster.centerLat) / 180.0 * height).toFloat()
          val isSelected = selectedCluster?.id == cluster.id

          // Outer radar glow
          drawCircle(
            color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color(0xFF3B82F6).copy(alpha = 0.2f),
            radius = if (isSelected) 36.dp.toPx() else 24.dp.toPx(),
            center = Offset(px, py)
          )

          // Pin circle
          drawCircle(
            color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF3B82F6),
            radius = if (isSelected) 14.dp.toPx() else 10.dp.toPx(),
            center = Offset(px, py)
          )

          // Inner white dot
          drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(px, py)
          )
        }
      }

      // City chips at top of map
      LazyRow(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(clusters) { cluster ->
          val isSelected = selectedCluster?.id == cluster.id
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(if (isSelected) Color(0xFF8B5CF6) else Color(0xFF1E293B).copy(alpha = 0.85f))
              .clickable { selectedCluster = cluster }
              .padding(horizontal = 12.dp, vertical = 7.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("${cluster.title} (${cluster.mediaItems.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Compass badge overlay
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(12.dp)
          .size(40.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E293B).copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = Icons.Rounded.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
      }
    }

    // Bottom Selected Location Media Carousel
    selectedCluster?.let { cluster ->
      Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(cluster.title, color = colors.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("Coordinates: %.2f°, %.2f° • %d captures".format(cluster.centerLat, cluster.centerLng, cluster.mediaItems.size), color = colors.secondaryText, fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cluster.mediaItems) { media ->
              Box(
                modifier = Modifier
                  .size(110.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                  .clickable { onOpenPhoto(media) }
              ) {
                AsyncImage(
                  model = media.uri,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              }
            }
          }
        }
      }
    }
  }
}
