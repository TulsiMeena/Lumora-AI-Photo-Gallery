package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GalleryTheme

enum class GalleryNavDestination(
  val route: String,
  val label: String,
  val icon: ImageVector
) {
  HOME("home", "Home", Icons.Rounded.PhotoLibrary),
  ALBUMS("albums", "Albums", Icons.Rounded.FolderCopy),
  AI("ai", "AI", Icons.Rounded.AutoAwesome),
  DOCUMENTS("documents", "Documents", Icons.Rounded.PictureAsPdf),
  VAULT("vault", "Vault", Icons.Rounded.Lock)
}

@Composable
fun PremiumBottomNav(
  currentDestination: GalleryNavDestination,
  onDestinationSelected: (GalleryNavDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = colors.border.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
      ),
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    color = colors.surface,
    tonalElevation = 8.dp,
    shadowElevation = 12.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .height(64.dp)
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      GalleryNavDestination.values().forEach { destination ->
        val isSelected = destination == currentDestination
        val iconColor by animateColorAsState(
          targetValue = if (isSelected) colors.accent else colors.secondaryText,
          label = "nav_icon_color"
        )
        val pillBgColor by animateColorAsState(
          targetValue = if (isSelected) colors.accentGlow else Color.Transparent,
          label = "nav_pill_bg"
        )
        val scale by animateFloatAsState(
          targetValue = if (isSelected) 1.05f else 1.0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
          ),
          label = "nav_item_scale"
        )

        val interactionSource = remember { MutableInteractionSource() }

        Column(
          modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
              interactionSource = interactionSource,
              indication = null
            ) {
              onDestinationSelected(destination)
            }
            .padding(vertical = 4.dp)
            .testTag("nav_${destination.route}"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(pillBgColor)
              .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = destination.icon,
              contentDescription = destination.label,
              tint = iconColor,
              modifier = Modifier.size(22.dp)
            )
          }

          Text(
            text = destination.label,
            color = iconColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    }
  }
}
