package com.example.ui.screens.creative.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.ui.theme.GalleryTheme
import kotlinx.coroutines.delay

@Composable
fun SlideshowDialog(
  items: List<MediaItem>,
  initialDurationSeconds: Int = 3,
  onDismiss: () -> Unit
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  var currentIndex by remember { mutableIntStateOf(0) }
  var isPlaying by remember { mutableStateOf(true) }
  var durationSec by remember { mutableFloatStateOf(initialDurationSeconds.toFloat()) }
  var showControls by remember { mutableStateOf(true) }

  // Auto-advance slideshow timer
  LaunchedEffect(isPlaying, currentIndex, durationSec) {
    if (isPlaying && items.isNotEmpty()) {
      delay((durationSec * 1000).toLong())
      currentIndex = (currentIndex + 1) % items.size
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable { showControls = !showControls }
        .testTag("dialog_slideshow")
    ) {
      if (items.isNotEmpty()) {
        val currentPhoto = items[currentIndex]

        AnimatedContent(
          targetState = currentPhoto,
          transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
          },
          label = "slideshow_crossfade"
        ) { photo ->
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(photo.uri)
              .crossfade(true)
              .build(),
            contentDescription = photo.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // Top Overlay Bar
      if (showControls) {
        Surface(
          color = Color.Black.copy(alpha = 0.6f),
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = if (items.isNotEmpty()) items[currentIndex].title else "Slideshow",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Slide ${currentIndex + 1} of ${items.size}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
              )
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(36.dp).testTag("btn_close_slideshow")
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
          }
        }

        // Bottom Controls Overlay
        Surface(
          color = Color.Black.copy(alpha = 0.65f),
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Playback Buttons
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
              IconButton(
                onClick = {
                  currentIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
                }
              ) {
                Icon(Icons.Rounded.FastRewind, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
              }

              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(colors.accent)
                  .clickable { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = if (isPlaying) "Pause" else "Play",
                  tint = Color.White,
                  modifier = Modifier.size(30.dp)
                )
              }

              IconButton(
                onClick = {
                  currentIndex = (currentIndex + 1) % items.size
                }
              ) {
                Icon(Icons.Rounded.FastForward, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("${durationSec.toInt()}s per slide", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Slider(
                value = durationSec,
                onValueChange = { durationSec = it },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                  thumbColor = colors.accent,
                  activeTrackColor = colors.accent,
                  inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }
}
