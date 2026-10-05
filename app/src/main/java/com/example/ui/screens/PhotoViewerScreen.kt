package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.ai.understanding.ImageUnderstandingTask
import com.example.ui.components.ProceduralPhotoArtwork
import com.example.ui.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
  photo: MediaItem,
  onClose: () -> Unit,
  onToggleFavorite: (MediaItem) -> Unit,
  onShare: (MediaItem) -> Unit,
  onEdit: (MediaItem, String) -> Unit,
  onAiAction: (MediaItem) -> Unit,
  onScanAsDoc: (MediaItem) -> Unit = {},
  onCreatePdf: ((MediaItem) -> Unit)? = null,
  onOpenPhotoEditor: ((MediaItem) -> Unit)? = null,
  onOpenAiPhotoEditor: ((MediaItem) -> Unit)? = null,
  onOpenAiUnderstanding: ((MediaItem, ImageUnderstandingTask) -> Unit)? = null,
  onOpenImageResize: ((MediaItem) -> Unit)? = null,
  onOpenPassportStudio: ((MediaItem) -> Unit)? = null,
  onOpenCompressor: ((MediaItem) -> Unit)? = null,
  onOpenFormatConverter: ((MediaItem) -> Unit)? = null,
  onMoveToVault: ((MediaItem) -> Unit)? = null,
  onDelete: (MediaItem) -> Unit,
  onSafeRemove: (MediaItem) -> Unit,
  onNextPhoto: (() -> Unit)? = null,
  onPrevPhoto: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = GalleryTheme.colors
  var showControls by remember { mutableStateOf(true) }
  var showDetailsSheet by remember { mutableStateOf(false) }
  var showEditSheet by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }

  // Pinch-to-zoom scale state
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .testTag("screen_photo_viewer")
  ) {
    // Zoomable Image / Video View Layer
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectTapGestures(
            onDoubleTap = {
              scale = if (scale > 1f) 1f else 2.5f
              offsetX = 0f
              offsetY = 0f
            },
            onTap = {
              showControls = !showControls
            }
          )
        }
        .pointerInput(Unit) {
          detectTransformGestures { _, pan, zoom, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            if (scale > 1f) {
              offsetX += pan.x
              offsetY += pan.y
            } else {
              offsetX = 0f
              offsetY = 0f
            }
          }
        },
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offsetX,
            translationY = offsetY
          ),
        contentAlignment = Alignment.Center
      ) {
        if (!photo.uri.isNullOrBlank()) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(photo.uri)
              .crossfade(true)
              .build(),
            contentDescription = photo.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          ProceduralPhotoArtwork(
            seed = photo.colorSeed,
            category = photo.categoryTag,
            modifier = Modifier.fillMaxSize()
          )
        }

        // Video Player Overlay & Indicator
        if (photo.mediaType == MediaType.VIDEO) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.55f))
              .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
              .clickable {
                if (!photo.uri.isNullOrBlank()) {
                  try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                      setDataAndType(Uri.parse(photo.uri), "video/*")
                      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    e.printStackTrace()
                  }
                }
              }
              .testTag("btn_play_video"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = "Play Video",
              tint = Color.White,
              modifier = Modifier.size(44.dp)
            )
          }
        }
      }
    }

    // Previous Navigation arrow
    if (showControls && onPrevPhoto != null) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp)
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.45f))
          .clickable { onPrevPhoto() },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.NavigateBefore,
          contentDescription = "Previous Photo",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }
    }

    // Next Navigation arrow
    if (showControls && onNextPhoto != null) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 12.dp)
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.45f))
          .clickable { onNextPhoto() },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.NavigateNext,
          contentDescription = "Next Photo",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }
    }

    // Top Controls Bar
    AnimatedVisibility(
      visible = showControls,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.Black.copy(alpha = 0.55f))
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = onClose,
              modifier = Modifier.testTag("btn_viewer_back")
            ) {
              Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = photo.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
              Text(
                text = "${photo.dateTaken} • ${photo.albumName}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { showDetailsSheet = true },
              modifier = Modifier.testTag("btn_viewer_info")
            ) {
              Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Details",
                tint = Color.White
              )
            }
            IconButton(
              onClick = { onToggleFavorite(photo) },
              modifier = Modifier.testTag("btn_viewer_favorite")
            ) {
              Icon(
                imageVector = if (photo.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (photo.isFavorite) Color(0xFFF43F5E) else Color.White
              )
            }
            Box {
              IconButton(
                onClick = { showMoreMenu = true },
                modifier = Modifier.testTag("btn_viewer_more")
              ) {
                Icon(
                  imageVector = Icons.Rounded.MoreVert,
                  contentDescription = "More",
                  tint = Color.White
                )
              }
              DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("AI Image Understanding & Chat") },
                  leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenAiUnderstanding?.invoke(photo, ImageUnderstandingTask.ASK_ANYTHING)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Solve Math Problem") },
                  leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenAiUnderstanding?.invoke(photo, ImageUnderstandingTask.SOLVE_MATH)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Translate Image (16+ Langs)") },
                  leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenAiUnderstanding?.invoke(photo, ImageUnderstandingTask.TRANSLATE)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Gemini AI Photo Editor") },
                  leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenAiPhotoEditor?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Ask AI Assistant") },
                  leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent)
                  },
                  onClick = {
                    showMoreMenu = false
                    onAiAction(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Move to Private Vault") },
                  leadingIcon = {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.accent)
                  },
                  onClick = {
                    showMoreMenu = false
                    onMoveToVault?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Remove from My Gallery") },
                  leadingIcon = {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = colors.warning)
                  },
                  onClick = {
                    showMoreMenu = false
                    onSafeRemove(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Scan / Extract Text (OCR)") },
                  leadingIcon = {
                    Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = colors.accent)
                  },
                  onClick = {
                    showMoreMenu = false
                    onScanAsDoc(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Resize, DPI & Dimensions") },
                  leadingIcon = {
                    Icon(Icons.Rounded.Crop, contentDescription = null, tint = colors.accent)
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenImageResize?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Compress & Target Size") },
                  leadingIcon = {
                    Icon(Icons.Rounded.Compress, contentDescription = null, tint = Color(0xFF0EA5E9))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenCompressor?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Convert Format (JPG/PNG/WebP)") },
                  leadingIcon = {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = Color(0xFF10B981))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenFormatConverter?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Passport & Visa Studio") },
                  leadingIcon = {
                    Icon(Icons.Rounded.AccountBox, contentDescription = null, tint = Color(0xFFF59E0B))
                  },
                  onClick = {
                    showMoreMenu = false
                    onOpenPassportStudio?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Create PDF from Photo") },
                  leadingIcon = {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                  },
                  onClick = {
                    showMoreMenu = false
                    onCreatePdf?.invoke(photo)
                  }
                )
                DropdownMenuItem(
                  text = { Text("Details & EXIF") },
                  leadingIcon = {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                  },
                  onClick = {
                    showMoreMenu = false
                    showDetailsSheet = true
                  }
                )
                DropdownMenuItem(
                  text = { Text("Delete from Device", color = colors.error) },
                  leadingIcon = {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error)
                  },
                  onClick = {
                    showMoreMenu = false
                    onDelete(photo)
                  }
                )
              }
            }
          }
        }
      }
    }

    // Bottom Action Bar
    AnimatedVisibility(
      visible = showControls,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.Black.copy(alpha = 0.65f))
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          ViewerBottomAction(
            title = "Share",
            icon = Icons.Rounded.Share,
            onClick = { onShare(photo) }
          )
          ViewerBottomAction(
            title = "Edit",
            icon = Icons.Rounded.Edit,
            onClick = {
              if (onOpenPhotoEditor != null) {
                onOpenPhotoEditor(photo)
              } else {
                showEditSheet = true
              }
            }
          )
          ViewerBottomAction(
            title = "AI Edit",
            icon = Icons.Rounded.AutoAwesome,
            accentTint = Color(0xFF38BDF8),
            onClick = {
              if (onOpenAiPhotoEditor != null) {
                onOpenAiPhotoEditor(photo)
              } else {
                onAiAction(photo)
              }
            }
          )
          ViewerBottomAction(
            title = "AI Vision",
            icon = Icons.Rounded.AutoAwesome,
            accentTint = Color(0xFF8B5CF6),
            onClick = {
              onOpenAiUnderstanding?.invoke(photo, ImageUnderstandingTask.ASK_ANYTHING)
            }
          )
          ViewerBottomAction(
            title = "Hide",
            icon = Icons.Rounded.VisibilityOff,
            accentTint = colors.warning,
            onClick = { onSafeRemove(photo) }
          )
          ViewerBottomAction(
            title = "Delete",
            icon = Icons.Rounded.Delete,
            accentTint = colors.error,
            onClick = { onDelete(photo) }
          )
        }
      }
    }
  }

  // EXIF / Details Sheet (Requirement 10)
  if (showDetailsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showDetailsSheet = false },
      sheetState = rememberModalBottomSheetState(),
      containerColor = colors.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp)
          .testTag("photo_details_sheet")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Details & EXIF Metadata",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = { showDetailsSheet = false }) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Close",
              tint = colors.secondaryText
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ExifDetailRow(label = "Title", value = photo.title)
        ExifDetailRow(label = "Type", value = if (photo.mediaType == MediaType.VIDEO) "Video (${photo.formattedDuration})" else "Photo")
        ExifDetailRow(label = "Date & Time", value = photo.dateTaken)
        ExifDetailRow(label = "Resolution", value = photo.resolution)
        ExifDetailRow(label = "File Size", value = photo.formattedSize)
        ExifDetailRow(label = "MIME Type", value = photo.mimeType)
        ExifDetailRow(label = "Folder", value = photo.albumName)
        ExifDetailRow(label = "Favorite", value = if (photo.isFavorite) "Yes (Starred)" else "No")
        if (!photo.locationName.isNullOrBlank()) {
          ExifDetailRow(label = "Location", value = photo.locationName)
        }
        if (photo.uri != null) {
          ExifDetailRow(label = "Storage", value = "Device MediaStore (Master Untouched)")
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                  setDataAndType(
                    android.net.Uri.parse(photo.uri),
                    if (photo.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                  )
                  addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
              } catch (_: Exception) {}
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Open in Device Location", fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(28.dp))
      }
    }
  }

  // Non-Destructive Quick Adjust Sheet (Requirement 9: Recently Edited)
  if (showEditSheet) {
    ModalBottomSheet(
      onDismissRequest = { showEditSheet = false },
      sheetState = rememberModalBottomSheetState(),
      containerColor = colors.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Non-Destructive Adjust",
              color = colors.primaryText,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Edits are tracked locally. Original photo is never overwritten.",
              color = colors.secondaryText,
              fontSize = 11.sp
            )
          }
          IconButton(onClick = { showEditSheet = false }) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.secondaryText)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val adjustments = listOf(
          "Warm Golden Hour" to "Enhances warm highlights and golden sunset tones",
          "Dramatic Monochrome" to "High-contrast black & white aesthetic",
          "Crisp Clarity & HDR" to "Sharpens textures and boosts shadow dynamics",
          "Vivid Vibrant Pop" to "Brings out rich saturation and nature greens",
          "Cinematic Teal & Orange" to "Hollywood color grading curve"
        )

        adjustments.forEach { (title, desc) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(colors.card)
              .clickable {
                showEditSheet = false
                onEdit(photo, title)
              }
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = title, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text(text = desc, color = colors.secondaryText, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun ViewerBottomAction(
  title: String,
  icon: ImageVector,
  accentTint: Color = Color.White,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag("viewer_action_${title.lowercase().replace(" ", "_")}")
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = accentTint,
      modifier = Modifier.size(22.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      color = accentTint,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun ExifDetailRow(
  label: String,
  value: String
) {
  val colors = GalleryTheme.colors

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      color = colors.secondaryText,
      fontSize = 13.sp
    )
    Text(
      text = value,
      color = colors.primaryText,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}
