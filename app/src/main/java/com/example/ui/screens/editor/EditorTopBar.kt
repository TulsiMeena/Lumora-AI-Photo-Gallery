package com.example.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.editor.CompareMode

@Composable
fun EditorTopBar(
  title: String,
  canUndo: Boolean,
  canRedo: Boolean,
  compareMode: CompareMode,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onCompareHold: (Boolean) -> Unit,
  onCompareModeCycle: () -> Unit,
  onSaveExport: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color(0xFF14171F).copy(alpha = 0.95f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Back button
      IconButton(
        onClick = onClose,
        modifier = Modifier.testTag("btn_editor_close")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Close Editor",
          tint = Color.White
        )
      }

      // Title & Undo / Redo group
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onUndo,
          enabled = canUndo,
          modifier = Modifier.testTag("btn_editor_undo")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = "Undo",
            tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f)
          )
        }

        IconButton(
          onClick = onRedo,
          enabled = canRedo,
          modifier = Modifier.testTag("btn_editor_redo")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Redo,
            contentDescription = "Redo",
            tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Compare Button with press-and-hold AND tap to cycle split mode
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (compareMode != CompareMode.NONE) Color(0xFF3B82F6).copy(alpha = 0.35f)
              else Color.White.copy(alpha = 0.08f)
            )
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  onCompareHold(true)
                  tryAwaitRelease()
                  onCompareHold(false)
                },
                onDoubleTap = {
                  onCompareModeCycle()
                }
              )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("btn_editor_compare"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Compare,
              contentDescription = "Compare Before/After",
              tint = if (compareMode != CompareMode.NONE) Color(0xFF60A5FA) else Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (compareMode == CompareMode.HOLD_BEFORE) "Before"
              else if (compareMode == CompareMode.SPLIT_SLIDER) "Split"
              else "Compare",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Actions group
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Save / Export button
        Button(
          onClick = onSaveExport,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3B82F6),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.testTag("btn_editor_export")
        ) {
          Icon(
            imageVector = Icons.Rounded.SaveAlt,
            contentDescription = "Save",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Save",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
