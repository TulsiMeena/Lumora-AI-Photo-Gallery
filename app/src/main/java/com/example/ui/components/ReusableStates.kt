package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GalleryTheme

@Composable
fun LoadingStateView(
  message: String = "Loading your library...",
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("state_loading"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator(
      color = colors.accent,
      strokeWidth = 3.dp,
      modifier = Modifier.size(44.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = message,
      color = colors.secondaryText,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  description: String,
  actionButtonText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp)
      .testTag("state_empty_${title.lowercase().replace(" ", "_")}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(colors.surfaceVariant)
        .border(1.dp, colors.border, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = colors.accent,
        modifier = Modifier.size(36.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = title,
      color = colors.primaryText,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = description,
      color = colors.secondaryText,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      textAlign = TextAlign.Center
    )

    if (actionButtonText != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onActionClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = colors.accent,
          contentColor = Color.White
        ),
        modifier = Modifier
          .height(44.dp)
          .testTag("empty_state_action_button")
      ) {
        Text(
          text = actionButtonText,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun PermissionCardView(
  title: String = "Media Access Required",
  description: String = "Grant permission to browse, organize and edit photos stored on your device.",
  onRequestPermission: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surfaceVariant)
      .border(1.dp, colors.border, RoundedCornerShape(16.dp))
      .padding(20.dp)
      .testTag("permission_card")
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Rounded.Security,
        contentDescription = null,
        tint = colors.accent,
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        color = colors.primaryText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = description,
        color = colors.secondaryText,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onRequestPermission,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = colors.accent,
          contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Grant Storage Access", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
