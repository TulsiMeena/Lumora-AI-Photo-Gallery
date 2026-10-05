package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.ui.theme.GalleryTheme
import com.example.ui.theme.ThumbnailShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnailCard(
  item: MediaItem,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  aspectRatio: Float = 1f,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  val cornerRadius = 12.dp

  Box(
    modifier = modifier
      .aspectRatio(aspectRatio)
      .clip(RoundedCornerShape(cornerRadius))
      .background(colors.card)
      .border(
        width = if (isSelected) 2.5.dp else 1.dp,
        color = if (isSelected) colors.accent else colors.border.copy(alpha = 0.4f),
        shape = RoundedCornerShape(cornerRadius)
      )
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .testTag("media_thumbnail_${item.id}")
  ) {
    if (!item.uri.isNullOrBlank()) {
      // Memory-efficient thumbnail loading via Coil
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(item.uri)
          .size(360, 360)
          .scale(Scale.FILL)
          .crossfade(true)
          .build(),
        contentDescription = item.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      // Procedural Photo Artwork Canvas fallback
      ProceduralPhotoArtwork(
        seed = item.colorSeed,
        category = item.categoryTag,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Bottom gradient scrim for high-contrast badge visibility
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0.5f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.65f)
          )
        )
    )

    // Video indicator & duration badge
    if (item.mediaType == MediaType.VIDEO) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.65f))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        androidx.compose.foundation.layout.Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Video",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
          )
          androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(3.dp))
          Text(
            text = item.formattedDuration,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    // Favorite heart badge
    if (item.isFavorite) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(6.dp)
          .size(22.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.Favorite,
          contentDescription = "Favorite",
          tint = Color(0xFFF43F5E),
          modifier = Modifier.size(13.dp)
        )
      }
    }

    // Selection mode checkmark
    if (isSelectionMode) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(6.dp)
          .size(22.dp)
          .clip(CircleShape)
          .background(
            if (isSelected) colors.accent else Color.Black.copy(alpha = 0.5f)
          )
          .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}

/**
 * Draws rich procedural photo gradients and geometric landscape artwork
 * inspired by real photography (sunsets, mountains, desert dunes, astro night skies, architecture).
 */
@Composable
fun ProceduralPhotoArtwork(
  seed: Int,
  category: String,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height

    when (seed % 6) {
      1 -> drawSunsetLandscape(width, height)
      2 -> drawMistyMountains(width, height)
      3 -> drawGoldenDunes(width, height)
      4 -> drawNightSkyAstro(width, height)
      5 -> drawModernArchitecture(width, height)
      else -> drawEmeraldNature(width, height)
    }
  }
}

private fun DrawScope.drawSunsetLandscape(w: Float, h: Float) {
  // Sky gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF311042),
        Color(0xFF8B2653),
        Color(0xFFD94638),
        Color(0xFFF59E0B),
        Color(0xFFFDE68A)
      )
    ),
    size = Size(w, h)
  )

  // Sun orb
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color.Transparent),
      center = Offset(w * 0.45f, h * 0.42f),
      radius = w * 0.28f
    ),
    center = Offset(w * 0.45f, h * 0.42f),
    radius = w * 0.22f
  )

  // Silhouettes of hill ridges
  drawOval(
    color = Color(0xFF450A0A).copy(alpha = 0.85f),
    topLeft = Offset(-w * 0.2f, h * 0.65f),
    size = Size(w * 0.8f, h * 0.6f)
  )
  drawOval(
    color = Color(0xFF1E1014),
    topLeft = Offset(w * 0.35f, h * 0.62f),
    size = Size(w * 0.9f, h * 0.65f)
  )
}

private fun DrawScope.drawMistyMountains(w: Float, h: Float) {
  // Cool atmospheric sky
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF1E293B),
        Color(0xFF334155),
        Color(0xFF64748B),
        Color(0xFF94A3B8)
      )
    ),
    size = Size(w, h)
  )

  // Mountain layer 1
  drawOval(
    color = Color(0xFF475569).copy(alpha = 0.9f),
    topLeft = Offset(-w * 0.1f, h * 0.45f),
    size = Size(w * 1.2f, h * 0.8f)
  )
  // Mountain layer 2
  drawOval(
    color = Color(0xFF1E293B),
    topLeft = Offset(w * 0.2f, h * 0.58f),
    size = Size(w * 1.0f, h * 0.7f)
  )
  // Misty veil
  drawRect(
    brush = Brush.verticalGradient(
      0.5f to Color.Transparent,
      0.8f to Color(0xFFCBD5E1).copy(alpha = 0.35f),
      1.0f to Color.Transparent
    ),
    size = Size(w, h)
  )
}

private fun DrawScope.drawGoldenDunes(w: Float, h: Float) {
  // Warm twilight
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF4C1D95),
        Color(0xFF9D174D),
        Color(0xFFEA580C),
        Color(0xFFFBBF24)
      )
    ),
    size = Size(w, h)
  )
  // Dune wave 1
  drawOval(
    color = Color(0xFFB45309),
    topLeft = Offset(-w * 0.3f, h * 0.55f),
    size = Size(w * 1.1f, h * 0.7f)
  )
  // Dune wave 2
  drawOval(
    color = Color(0xFF78350F),
    topLeft = Offset(w * 0.25f, h * 0.68f),
    size = Size(w * 1.0f, h * 0.6f)
  )
}

private fun DrawScope.drawNightSkyAstro(w: Float, h: Float) {
  // Deep cosmic gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF030712),
        Color(0xFF0B132B),
        Color(0xFF1C2541),
        Color(0xFF3A0CA3).copy(alpha = 0.7f)
      )
    ),
    size = Size(w, h)
  )

  // Star cluster dots
  val stars = listOf(
    Offset(w * 0.2f, h * 0.15f) to 1.8f,
    Offset(w * 0.35f, h * 0.28f) to 2.2f,
    Offset(w * 0.75f, h * 0.18f) to 1.5f,
    Offset(w * 0.6f, h * 0.35f) to 2.5f,
    Offset(w * 0.85f, h * 0.42f) to 1.4f,
    Offset(w * 0.15f, h * 0.45f) to 2.0f,
    Offset(w * 0.45f, h * 0.12f) to 1.6f
  )
  for ((offset, radius) in stars) {
    drawCircle(Color.White, radius = radius, center = offset)
  }

  // Horizon silhouette
  drawOval(
    color = Color(0xFF020617),
    topLeft = Offset(-w * 0.1f, h * 0.75f),
    size = Size(w * 1.2f, h * 0.5f)
  )
}

private fun DrawScope.drawModernArchitecture(w: Float, h: Float) {
  drawRect(
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155)),
      start = Offset.Zero,
      end = Offset(w, h)
    ),
    size = Size(w, h)
  )
  // Geometric building lines
  drawRect(
    color = Color(0xFF64748B).copy(alpha = 0.35f),
    topLeft = Offset(w * 0.15f, h * 0.2f),
    size = Size(w * 0.35f, h * 0.8f)
  )
  drawRect(
    color = Color(0xFF475569).copy(alpha = 0.6f),
    topLeft = Offset(w * 0.45f, h * 0.35f),
    size = Size(w * 0.4f, h * 0.65f)
  )
}

private fun DrawScope.drawEmeraldNature(w: Float, h: Float) {
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF064E3B),
        Color(0xFF047857),
        Color(0xFF059669),
        Color(0xFF10B981)
      )
    ),
    size = Size(w, h)
  )
  drawCircle(
    color = Color(0xFF34D399).copy(alpha = 0.25f),
    center = Offset(w * 0.5f, h * 0.5f),
    radius = w * 0.45f
  )
}
