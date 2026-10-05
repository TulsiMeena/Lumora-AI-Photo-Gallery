package com.example.ui.screens.vault.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MediaType
import com.example.model.vault.VaultMediaItem
import com.example.ui.theme.GalleryTheme
import com.example.utils.vault.VaultCryptoManager
import com.example.utils.vault.VaultStorageManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VaultMediaViewerDialog(
  item: VaultMediaItem,
  askBeforeAiUpload: Boolean,
  exportWarningEnabled: Boolean,
  onToggleFavorite: (VaultMediaItem) -> Unit,
  onExportToGallery: (VaultMediaItem) -> Unit,
  onDeleteFromVault: (VaultMediaItem) -> Unit,
  onOpenInAiEdit: ((Bitmap) -> Unit)? = null,
  onOpenInAiChat: ((Bitmap) -> Unit)? = null,
  onOpenInOcr: ((Bitmap) -> Unit)? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val colors = GalleryTheme.colors
  val scope = rememberCoroutineScope()

  var decodedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var tempVideoFile by remember { mutableStateOf<File?>(null) }

  // Details dialog
  var showDetailsDialog by remember { mutableStateOf(false) }

  // Confirm Export / Move Out Dialog
  var showExportConfirmDialog by remember { mutableStateOf(false) }

  // Confirm Delete Dialog
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  // Confirm Share Dialog
  var showShareWarningDialog by remember { mutableStateOf(false) }

  // Confirm AI Upload Dialog
  var pendingAiAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showAiUploadConfirmDialog by remember { mutableStateOf(false) }

  // Zoom / Pan state
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  // Load media safely
  LaunchedEffect(item) {
    isLoading = true
    val mediaDir = VaultStorageManager.getVaultMediaDir(context)
    val encFile = File(mediaDir, item.encryptedFileName)
    val iv = Base64.decode(item.ivBase64, Base64.NO_WRAP)

    if (item.mediaType == MediaType.PHOTO) {
      val bmp = VaultCryptoManager.decodeEncryptedBitmap(encFile, iv, maxDim = 3840)
      decodedBitmap = bmp
    } else {
      val videoFile = VaultStorageManager.prepareDecryptedTempVideo(context, item)
      tempVideoFile = videoFile
    }
    isLoading = false
  }

  // Ensure temp video file is deleted when viewer closes
  DisposableEffect(Unit) {
    onDispose {
      tempVideoFile?.let { VaultStorageManager.cleanupTempVideo(it) }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black),
      color = Color.Black
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        // Media Canvas
        if (isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = Color.White)
              Spacer(modifier = Modifier.height(10.dp))
              Text("Decrypting secure media…", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
          }
        } else if (item.mediaType == MediaType.PHOTO && decodedBitmap != null) {
          Image(
            bitmap = decodedBitmap!!.asImageBitmap(),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
              )
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
              }
          )
        } else if (item.mediaType == MediaType.VIDEO && tempVideoFile != null) {
          AndroidView(
            factory = { ctx ->
              VideoView(ctx).apply {
                setVideoURI(Uri.fromFile(tempVideoFile))
                setOnPreparedListener { mp ->
                  mp.isLooping = true
                  start()
                }
              }
            },
            modifier = Modifier.fillMaxSize()
          )
        }

        // Top Navigation Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .background(Color.Black.copy(alpha = 0.5f), CircleShape)
              .testTag("btn_close_vault_viewer")
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Details
            IconButton(
              onClick = { showDetailsDialog = true },
              modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
              Icon(Icons.Rounded.Info, contentDescription = "Details", tint = Color.White)
            }

            // Favorite Toggle
            IconButton(
              onClick = { onToggleFavorite(item) },
              modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
              Icon(
                imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (item.isFavorite) Color(0xFFEF4444) else Color.White
              )
            }
          }
        }

        // Bottom Action Bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // 1. Export / Move Out of Vault
            ViewerActionButton(
              icon = Icons.Rounded.DriveFileMove,
              label = "Move Out",
              onClick = {
                if (exportWarningEnabled) {
                  showExportConfirmDialog = true
                } else {
                  onExportToGallery(item)
                  onDismiss()
                }
              }
            )

            // 2. Share
            ViewerActionButton(
              icon = Icons.Rounded.Share,
              label = "Share",
              onClick = {
                showShareWarningDialog = true
              }
            )

            // 3. AI Features (Single-photo protected upload)
            if (item.mediaType == MediaType.PHOTO && decodedBitmap != null) {
              ViewerActionButton(
                icon = Icons.Rounded.AutoAwesome,
                label = "AI Studio",
                onClick = {
                  val action = {
                    onOpenInAiEdit?.invoke(decodedBitmap!!)
                    onDismiss()
                  }
                  if (askBeforeAiUpload) {
                    pendingAiAction = action
                    showAiUploadConfirmDialog = true
                  } else {
                    action()
                  }
                }
              )

              ViewerActionButton(
                icon = Icons.Rounded.DocumentScanner,
                label = "OCR & Math",
                onClick = {
                  val action = {
                    onOpenInOcr?.invoke(decodedBitmap!!)
                    onDismiss()
                  }
                  if (askBeforeAiUpload) {
                    pendingAiAction = action
                    showAiUploadConfirmDialog = true
                  } else {
                    action()
                  }
                }
              )
            }

            // 4. Delete
            ViewerActionButton(
              icon = Icons.Rounded.DeleteForever,
              label = "Delete",
              tint = Color(0xFFEF4444),
              onClick = {
                showDeleteConfirmDialog = true
              }
            )
          }
        }
      }
    }
  }

  // --- SUB-DIALOGS ---

  // 1. Details Dialog
  if (showDetailsDialog) {
    AlertDialog(
      onDismissRequest = { showDetailsDialog = false },
      title = { Text("Encrypted Media Details") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("File Name: ${item.originalFileName}", fontSize = 12.sp)
          Text("Size: ${item.formattedSize}", fontSize = 12.sp)
          Text("Type: ${item.mimeType}", fontSize = 12.sp)
          if (item.width > 0 && item.height > 0) {
            Text("Dimensions: ${item.width} × ${item.height} px", fontSize = 12.sp)
          }
          Text("Encryption: Hardware AES-256 GCM", fontSize = 12.sp, color = Color(0xFF10B981))
          Text("Storage: App-Private (Zero MediaStore Indexing)", fontSize = 11.sp, color = colors.secondaryText)
        }
      },
      confirmButton = {
        Button(onClick = { showDetailsDialog = false }) { Text("Close") }
      }
    )
  }

  // 2. Export / Move Out Confirmation Dialog
  if (showExportConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showExportConfirmDialog = false },
      title = { Text("Move Out of Private Vault?") },
      text = {
        Text("This will decrypt the photo and save an unprotected copy in your public Gallery (Pictures/My Gallery AI/Exported/).")
      },
      confirmButton = {
        Button(
          onClick = {
            showExportConfirmDialog = false
            onExportToGallery(item)
            onDismiss()
          }
        ) {
          Text("Export & Unprotect")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showExportConfirmDialog = false }) { Text("Cancel") }
      }
    )
  }

  // 3. Share Warning Dialog
  if (showShareWarningDialog) {
    AlertDialog(
      onDismissRequest = { showShareWarningDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.warning)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Share Private Media?")
        }
      },
      text = {
        Text("This will create a temporary decrypted copy to send outside the Private Vault via Android's share menu. Normal vault isolation will not apply to shared recipients.")
      },
      confirmButton = {
        Button(
          onClick = {
            showShareWarningDialog = false
            scope.launch {
              val exported = VaultStorageManager.exportMedia(context, item, deleteFromVaultAfterExport = false)
              exported.getOrNull()?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = item.mimeType
                  putExtra(Intent.EXTRA_STREAM, uri)
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Private Photo"))
              }
            }
          }
        ) {
          Text("Share Unprotected Copy")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showShareWarningDialog = false }) { Text("Cancel") }
      }
    )
  }

  // 4. Delete Confirmation Dialog
  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Delete from Vault permanently?")
        }
      },
      text = {
        Text(
          text = "This will permanently remove the protected encrypted copy. This action cannot be undone.",
          color = Color(0xFFEF4444)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmDialog = false
            onDeleteFromVault(item)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Delete Permanently", color = Color.White)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
      }
    )
  }

  // 5. AI Upload Confirmation Dialog (Mandatory Privacy Guard)
  if (showAiUploadConfirmDialog) {
    AlertDialog(
      onDismissRequest = {
        showAiUploadConfirmDialog = false
        pendingAiAction = null
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.accent)
          Spacer(modifier = Modifier.width(8.dp))
          Text("AI Processing Consent")
        }
      },
      text = {
        Column {
          Text(
            text = "Do you want to send this single private photo for AI analysis/editing?",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "• Only this individual selected image will be sent.\n• No other Vault media or metadata is shared.\n• Your Private Vault remains otherwise completely isolated.",
            fontSize = 11.sp,
            color = colors.secondaryText,
            lineHeight = 15.sp
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showAiUploadConfirmDialog = false
            pendingAiAction?.invoke()
            pendingAiAction = null
          }
        ) {
          Text("Send to AI")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            showAiUploadConfirmDialog = false
            pendingAiAction = null
          }
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun ViewerActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color = Color.White,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(8.dp)
  ) {
    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    Spacer(modifier = Modifier.height(4.dp))
    Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
  }
}
