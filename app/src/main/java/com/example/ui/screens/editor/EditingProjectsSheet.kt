package com.example.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.EditingProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditingProjectsSheet(
  projects: List<EditingProjectEntity>,
  onSelectProject: (EditingProjectEntity) -> Unit,
  onDeleteProject: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF161922),
    modifier = modifier.testTag("sheet_editing_projects")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.HistoryEdu,
            contentDescription = null,
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Editing Projects",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
        }
      }

      Text(
        text = "Resume unfinished non-destructive edits with all history and layers preserved.",
        color = Color(0xFF94A3B8),
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 16.dp)
      )

      if (projects.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No active editing projects", color = Color(0xFF64748B), fontSize = 14.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(projects, key = { it.id }) { project ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF202533))
                .clickable { onSelectProject(project) }
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Thumbnail
              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data(project.thumbnailPath ?: project.originalUri)
                  .crossfade(true)
                  .build(),
                contentDescription = project.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(60.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF334155))
              )

              Spacer(modifier = Modifier.width(14.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = project.title,
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = dateFormat.format(Date(project.lastModifiedMillis)),
                  color = Color(0xFF94A3B8),
                  fontSize = 12.sp
                )
              }

              // Actions
              IconButton(
                onClick = { onSelectProject(project) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF60A5FA))
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Continue Editing")
              }

              IconButton(
                onClick = { onDeleteProject(project.id) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFEF4444))
              ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete Project")
              }
            }
          }
        }
      }
    }
  }
}
