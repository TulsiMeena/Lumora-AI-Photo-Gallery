package com.example.ui.screens.advanced

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.advanced.CleanerCategory
import com.example.model.advanced.DuplicateGroup
import com.example.model.advanced.StorageDoctorScanResult
import com.example.ui.theme.GalleryTheme

@Composable
fun StorageDoctorScreen(
  scanResult: StorageDoctorScanResult,
  onRescan: () -> Unit,
  onCleanGroup: (DuplicateGroup, Set<String>) -> Unit,
  onCleanAllRecommended: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  var selectedCategory by remember { mutableStateOf<CleanerCategory?>(null) }

  val filteredGroups = if (selectedCategory == null) {
    scanResult.duplicateGroups
  } else {
    scanResult.duplicateGroups.filter { it.category == selectedCategory }
  }

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
          modifier = Modifier.testTag("btn_doctor_back")
        ) {
          Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Storage Doctor",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "AI Duplicates & Junk Cleaner",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }

        IconButton(onClick = onRescan) {
          Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Rescan", tint = colors.accent)
        }
      }
    }

    if (scanResult.isScanning) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = colors.accent)
          Spacer(modifier = Modifier.height(14.dp))
          Text("Analyzing gallery for duplicates & junk...", color = colors.secondaryText, fontSize = 13.sp)
        }
      }
      return
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 14.dp)
    ) {
      // Savings Hero Card
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Potential Space to Free", color = colors.secondaryText, fontSize = 12.sp)
                Text(
                  text = scanResult.formattedSavings(),
                  color = colors.accent,
                  fontSize = 32.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Rounded.CleaningServices,
                  contentDescription = null,
                  tint = colors.accent,
                  modifier = Modifier.size(28.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Found ${scanResult.duplicateGroups.size} clutter groups across ${scanResult.scannedCount} photos. Original best-quality photos are kept safe.",
              color = colors.secondaryText,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )

            if (scanResult.totalSavingsBytes > 0) {
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = onCleanAllRecommended,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(imageVector = Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("1-Tap Clean Recommended (${scanResult.formattedSavings()})", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Filter chips
      item {
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChipItem(
              title = "All Issues",
              isSelected = selectedCategory == null,
              onClick = { selectedCategory = null }
            )
          }
          items(CleanerCategory.values()) { cat ->
            FilterChipItem(
              title = cat.label,
              isSelected = selectedCategory == cat,
              onClick = { selectedCategory = cat }
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Group cards
      if (filteredGroups.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("Your gallery is clean & organized!", color = colors.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("No redundant duplicates found.", color = colors.secondaryText, fontSize = 12.sp)
            }
          }
        }
      } else {
        items(filteredGroups) { group ->
          DuplicateGroupCard(
            group = group,
            onClean = { toDeleteIds -> onCleanGroup(group, toDeleteIds) }
          )
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
private fun DuplicateGroupCard(
  group: DuplicateGroup,
  onClean: (Set<String>) -> Unit
) {
  val colors = GalleryTheme.colors
  val keepId = group.recommendedKeepId ?: group.items.first().id
  var selectedForDelete by remember {
    mutableStateOf(group.items.filter { it.id != keepId }.map { it.id }.toSet())
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surface)
      .border(1.dp, colors.border, RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(group.category.label, color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("${group.items.size} photos • Savings: %.1f MB".format(group.potentialSavingsBytes / (1024.0 * 1024.0)), color = colors.secondaryText, fontSize = 11.sp)
        }

        if (selectedForDelete.isNotEmpty()) {
          Button(
            onClick = { onClean(selectedForDelete) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete ${selectedForDelete.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(group.items) { item ->
          val isBest = item.id == keepId
          val isSelected = selectedForDelete.contains(item.id)

          Box(
            modifier = Modifier
              .size(105.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(
                width = if (isBest) 2.dp else if (isSelected) 2.dp else 1.dp,
                color = if (isBest) Color(0xFF10B981) else if (isSelected) Color(0xFFEF4444) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
              )
              .clickable {
                if (!isBest) {
                  selectedForDelete = if (isSelected) selectedForDelete - item.id else selectedForDelete + item.id
                }
              }
          ) {
            AsyncImage(
              model = item.uri,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )

            if (isBest) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .padding(4.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF10B981))
                  .padding(horizontal = 5.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                  Spacer(modifier = Modifier.width(2.dp))
                  Text("KEEP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              }
            } else if (isSelected) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp)
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FilterChipItem(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(if (isSelected) colors.accent else colors.surface)
      .border(1.dp, if (isSelected) colors.accent else colors.border, RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(
      text = title,
      color = if (isSelected) Color.White else colors.primaryText,
      fontSize = 12.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )
  }
}
