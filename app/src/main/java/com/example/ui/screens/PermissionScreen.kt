package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GalleryTheme

@Composable
fun PermissionScreen(
  onRequestPermission: () -> Unit,
  onExploreWithSampleData: () -> Unit,
  isPermanentlyDenied: Boolean = false,
  isPartialAccessGranted: Boolean = false,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = GalleryTheme.colors
  val isAndroid14Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Brand Section
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(top = 24.dp)
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(colors.accentGlow)
          .border(2.dp, colors.accent.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.PhotoLibrary,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Access Your Photos & Videos",
        color = colors.primaryText,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "To organize your memories with lightning speed, My Gallery AI requires permission to read media stored on this device.",
        color = colors.secondaryText,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 19.sp
      )
    }

    // Privacy & Feature Highlights
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      PermissionHighlightCard(
        icon = Icons.Rounded.CloudOff,
        title = "100% Offline & Private",
        description = "Your photos never leave your device. Zero cloud sync, zero telemetry."
      )

      PermissionHighlightCard(
        icon = Icons.Rounded.Lock,
        title = "Original Files Untouched",
        description = "Read-only access protects your master files. Favorites are stored locally."
      )

      if (isAndroid14Plus) {
        PermissionHighlightCard(
          icon = Icons.Rounded.Collections,
          title = "Full or Selected Photos Access",
          description = "Android 14+ allows choosing between granting full library or selected photos."
        )
      } else {
        PermissionHighlightCard(
          icon = Icons.Rounded.Security,
          title = "Standard Storage Access",
          description = "Scans camera, screenshots, videos, and downloads directly from MediaStore."
        )
      }

      if (isPartialAccessGranted) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.warning.copy(alpha = 0.12f))
            .border(1.dp, colors.warning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = colors.warning,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Partial access active. You can grant full access for complete album indexing.",
              color = colors.primaryText,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    // Bottom Action Buttons
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (isPermanentlyDenied) {
        Button(
          onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_open_settings")
        ) {
          Icon(imageVector = Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Open App Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      } else {
        Button(
          onClick = onRequestPermission,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_grant_permission")
        ) {
          Icon(imageVector = Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isPartialAccessGranted) "Manage / Upgrade Photo Access" else "Grant Photo Access",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      OutlinedButton(
        onClick = onExploreWithSampleData,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.secondaryText),
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("btn_explore_sample_data")
      ) {
        Text(text = "Explore App with Sample Data", fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun PermissionHighlightCard(
  icon: ImageVector,
  title: String,
  description: String
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(colors.surface)
      .border(1.dp, colors.border, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(colors.accentGlow),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          color = colors.primaryText,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          color = colors.secondaryText,
          fontSize = 11.sp,
          lineHeight = 15.sp
        )
      }
    }
  }
}
