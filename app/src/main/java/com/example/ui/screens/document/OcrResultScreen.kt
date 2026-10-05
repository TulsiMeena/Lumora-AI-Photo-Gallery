package com.example.ui.screens.document

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.document.DocumentOcrEngine
import com.example.model.DocumentItem
import com.example.model.OcrAnalysisResult
import com.example.ui.theme.GalleryTheme
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrResultScreen(
  document: DocumentItem,
  ocrResult: OcrAnalysisResult,
  onSaveEditedText: (String) -> Unit,
  onAskAi: (prompt: String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = GalleryTheme.colors

  var isEditing by remember { mutableStateOf(false) }
  var editableText by remember { mutableStateOf(ocrResult.fullText) }
  var searchQuery by remember { mutableStateOf("") }
  var isSearchVisible by remember { mutableStateOf(false) }
  var copyFeedback by remember { mutableStateOf(false) }

  val matches = remember(editableText, searchQuery) {
    if (searchQuery.isNotBlank()) {
      DocumentOcrEngine.searchInText(editableText, searchQuery)
    } else {
      emptyList()
    }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = colors.background
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Navigation Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(colors.surface)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onClose) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = colors.textPrimary
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Extracted OCR Text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
          )
          Text(
            text = "${document.title} • ${ocrResult.wordCount} words",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1
          )
        }

        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search text",
            tint = if (isSearchVisible) colors.accent else colors.textSecondary
          )
        }

        IconButton(onClick = { isEditing = !isEditing }) {
          Icon(
            imageVector = if (isEditing) Icons.Rounded.Check else Icons.Rounded.Edit,
            contentDescription = if (isEditing) "Done editing" else "Edit text",
            tint = if (isEditing) colors.accent else colors.textSecondary
          )
        }
      }

      // Search bar if open
      AnimatedVisibility(visible = isSearchVisible) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = colors.surfaceVariant,
          tonalElevation = 2.dp
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search within extracted text...") },
              singleLine = true,
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear")
                  }
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border
              )
            )

            if (searchQuery.isNotBlank()) {
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "${matches.size} found",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent
              )
            }
          }
        }
      }

      // Scrollable Text & Detected Entities Area
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
      ) {
        // Quick Action Chips Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AssistChip(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Document OCR Text", editableText)
              clipboard.setPrimaryClip(clip)
              copyFeedback = true
            },
            label = { Text(if (copyFeedback) "Copied!" else "Copy All") },
            leadingIcon = {
              Icon(
                imageVector = if (copyFeedback) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = if (copyFeedback) Color(0xFF10B981) else colors.accent,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = AssistChipDefaults.assistChipColors(
              containerColor = colors.surface,
              labelColor = colors.textPrimary
            )
          )

          AssistChip(
            onClick = {
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, editableText)
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Document Text"))
            },
            label = { Text("Share Text") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = AssistChipDefaults.assistChipColors(
              containerColor = colors.surface,
              labelColor = colors.textPrimary
            )
          )

          if (isEditing) {
            AssistChip(
              onClick = {
                onSaveEditedText(editableText)
                isEditing = false
              },
              label = { Text("Save Edits") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Save,
                  contentDescription = null,
                  tint = colors.accent,
                  modifier = Modifier.size(16.dp)
                )
              },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = colors.accent.copy(alpha = 0.15f),
                labelColor = colors.accent
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detected Entities Summary Card
        if (ocrResult.detectedDates.isNotEmpty() || ocrResult.detectedAmounts.isNotEmpty() || ocrResult.detectedIds.isNotEmpty()) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "DETECTED ENTITIES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
              )
              Spacer(modifier = Modifier.height(10.dp))

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                // Dates
                ocrResult.detectedDates.forEach { date ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6)
                      )
                    }
                  }
                }

                // Amounts
                ocrResult.detectedAmounts.forEach { amount ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = amount,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                      )
                    }
                  }
                }

                // IDs / Numbers
                ocrResult.detectedIds.forEach { idVal ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Tag,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = idVal,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8B5CF6)
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
        }

        // Ask AI About This Document Card
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = colors.accent.copy(alpha = 0.08f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Ask AI About This Document",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Tap a prompt to analyze, translate, or extract answers using offline intelligence:",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                "इस document में क्या लिखा है?",
                "इसका summary बनाओ।",
                "इसमें कौन-कौन सी dates हैं?",
                "महत्वपूर्ण numbers निकालो।",
                "Translate to Hindi"
              ).forEach { prompt ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = colors.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                  modifier = Modifier.clickable { onAskAi(prompt) }
                ) {
                  Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Extracted Text Display / Editor
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = colors.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isEditing) "EDITING MODE" else "FORMATTED DOCUMENT TEXT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
              )

              Text(
                text = "${ocrResult.lineCount} lines",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
              OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  lineHeight = 22.sp
                ),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = colors.accent,
                  unfocusedBorderColor = colors.border
                )
              )
            } else {
              SelectionContainer {
                val annotatedText = buildAnnotatedString {
                  val text = editableText
                  if (searchQuery.isNotBlank()) {
                    val lowerText = text.lowercase(Locale.getDefault())
                    val lowerQuery = searchQuery.lowercase(Locale.getDefault())
                    var lastIndex = 0
                    while (true) {
                      val start = lowerText.indexOf(lowerQuery, lastIndex)
                      if (start == -1) {
                        append(text.substring(lastIndex))
                        break
                      }
                      append(text.substring(lastIndex, start))
                      withStyle(
                        style = SpanStyle(
                          background = Color(0xFFFEF08A),
                          color = Color.Black,
                          fontWeight = FontWeight.Bold
                        )
                      ) {
                        append(text.substring(start, start + lowerQuery.length))
                      }
                      lastIndex = start + lowerQuery.length
                    }
                  } else {
                    append(text)
                  }
                }

                Text(
                  text = annotatedText,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 22.sp
                  ),
                  color = colors.textPrimary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy indicator
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Local On-Device OCR • Zero Cloud Upload",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
          )
        }
      }
    }
  }
}
