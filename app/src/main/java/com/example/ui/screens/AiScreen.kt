package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiChatMessage
import com.example.data.ai.ImageAnalysisResult
import com.example.data.local.SearchHistoryEntity
import com.example.model.AiActionItem
import com.example.model.MediaItem
import com.example.model.ai.understanding.ImageUnderstandingTask
import com.example.ui.components.MediaThumbnailCard
import com.example.ui.components.ProceduralPhotoArtwork
import com.example.ui.theme.GalleryTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiScreen(
  chatMessages: List<AiChatMessage>,
  isAiSearching: Boolean,
  suggestions: List<String>,
  recentActions: List<AiActionItem>,
  searchHistory: List<SearchHistoryEntity>,
  attachedMedia: MediaItem?,
  allPhotos: List<MediaItem>,
  onSubmitQuery: (String, MediaItem?) -> Unit,
  onClearConversation: () -> Unit,
  onAttachMedia: (MediaItem?) -> Unit,
  onRemoveAttachedMedia: () -> Unit,
  onDeleteHistoryItem: (String) -> Unit,
  onClearHistory: () -> Unit,
  onViewInMainGallery: (List<MediaItem>, String) -> Unit,
  onPhotoClick: (MediaItem) -> Unit,
  onOpenAiEditor: () -> Unit,
  onOpenAiEditorWithMedia: ((MediaItem) -> Unit)? = null,
  onOpenVisualAnalysis: () -> Unit,
  onOpenAiUnderstanding: ((MediaItem?, ImageUnderstandingTask) -> Unit)? = null,
  onOpenImageResize: ((MediaItem?, Int) -> Unit)? = null,
  onOpenCompressor: ((MediaItem?, Int) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  var currentInput by remember { mutableStateOf("") }
  var showHistorySheet by remember { mutableStateOf(false) }
  var showAttachPickerSheet by remember { mutableStateOf(false) }

  // Speech Recognition Activity Launcher
  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
      if (!spoken.isNullOrBlank()) {
        currentInput = spoken
        onSubmitQuery(spoken, attachedMedia)
      }
    }
  }

  fun launchVoiceInput() {
    try {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about your photos (Hindi or English)…")
      }
      speechLauncher.launch(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "Speech recognition not available on device", Toast.LENGTH_SHORT).show()
    }
  }

  // Auto-scroll to bottom when new messages arrive
  LaunchedEffect(chatMessages.size, isAiSearching) {
    if (chatMessages.isNotEmpty()) {
      listState.animateScrollToItem(chatMessages.size)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // 1. HEADER
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = colors.background
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(colors.accent, Color(0xFF8B5CF6))
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "AI Gallery Assistant",
              color = colors.primaryText,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "100% On-Device & Private",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (searchHistory.isNotEmpty()) {
            IconButton(
              onClick = { showHistorySheet = true },
              modifier = Modifier.testTag("btn_ai_history")
            ) {
              Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = "Search History",
                tint = colors.secondaryText
              )
            }
          }

          if (chatMessages.isNotEmpty()) {
            IconButton(
              onClick = onClearConversation,
              modifier = Modifier.testTag("btn_clear_ai_chat")
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = "Clear Conversation",
                tint = colors.secondaryText
              )
            }
          }
        }
      }
    }

    // 2. QUICK SMART FILTER CHIPS (Always accessible)
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("ai_smart_chips_row")
    ) {
      val quickChips = listOf(
        "September Photos" to "September की photos दिखाओ",
        "Red Shirt" to "लाल shirt वाली photos दिखाओ",
        "Aadhaar Card" to "मेरी Aadhaar वाली photo खोजो",
        "Yesterday" to "कल की photos दिखाओ",
        "Videos" to "इस महीने के videos दिखाओ",
        "Favorites" to "मेरी favorite photos दिखाओ",
        "Screenshots" to "मेरी screenshots दिखाओ",
        "Beach & Waves" to "Beach वाली photos दिखाओ",
        "Large Files" to "Large size वाली photos दिखाओ",
        "Most Recent" to "मेरी सबसे recent photos दिखाओ"
      )
      items(quickChips) { (label, query) ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable {
              currentInput = query
              onSubmitQuery(query, attachedMedia)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("chip_${label.lowercase().replace(" ", "_")}")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = label,
              color = colors.primaryText,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // 3. MAIN WORKSPACE / CHAT CONVERSATION AREA
    LazyColumn(
      state = listState,
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .testTag("ai_chat_scroll_area")
    ) {
      // IF NO MESSAGES YET: SHOW WELCOME & SUGGESTIONS & TOOLS
      if (chatMessages.isEmpty()) {
        item {
          WelcomeAssistantBanner()
        }

        // Suggested Queries List
        item {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Natural Language Vision Queries",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              suggestions.forEach { prompt ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable {
                      currentInput = prompt
                      onSubmitQuery(prompt, attachedMedia)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("prompt_chip_${prompt.hashCode()}")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Rounded.AutoAwesome,
                      contentDescription = null,
                      tint = colors.accent,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = prompt,
                      color = colors.primaryText,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            }
          }
        }

        // Recent Search History Preview
        if (searchHistory.isNotEmpty()) {
          item {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Recent Searches",
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Text(
                  text = "View All",
                  color = colors.accent,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.clickable { showHistorySheet = true }
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              searchHistory.take(4).forEach { item ->
                SearchHistoryRow(
                  item = item,
                  onSelect = { query ->
                    currentInput = query
                    onSubmitQuery(query, attachedMedia)
                  },
                  onDelete = { onDeleteHistoryItem(item.id) }
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
            }
          }
        }

        // AI Suite Tools (Editor & Visual Analysis)
        item {
          Column {
            Text(
              text = "AI Photo Intelligence Suite",
              color = colors.primaryText,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              AiToolCard(
                title = "AI Understanding",
                subtitle = "Ask, Math, OCR, Translate, Tables & Charts",
                badge = "Gemini",
                icon = Icons.Rounded.AutoAwesome,
                accentColor = Color(0xFF8B5CF6),
                onClick = {
                  onOpenAiUnderstanding?.invoke(attachedMedia, ImageUnderstandingTask.ASK_ANYTHING)
                },
                modifier = Modifier.weight(1f)
              )
              AiToolCard(
                title = "AI Photo Editor",
                subtitle = "Magic Eraser, Generative Fill & Unblur",
                badge = "Ready",
                icon = Icons.Rounded.AutoFixHigh,
                accentColor = colors.accent,
                onClick = onOpenAiEditor,
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              AiToolCard(
                title = "Resize & DPI Studio",
                subtitle = "Exact Dimensions, Print DPI, Units & Batch",
                badge = "Offline",
                icon = Icons.Rounded.Crop,
                accentColor = Color(0xFF0EA5E9),
                onClick = {
                  onOpenImageResize?.invoke(attachedMedia, 0)
                },
                modifier = Modifier.weight(1f)
              )
              AiToolCard(
                title = "Passport Studio",
                subtitle = "Biometric Sizes, Face Guide & Print Sheet",
                badge = "Print",
                icon = Icons.Rounded.AccountBox,
                accentColor = Color(0xFFF59E0B),
                onClick = {
                  onOpenImageResize?.invoke(attachedMedia, 1)
                },
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              AiToolCard(
                title = "Compress & Convert",
                subtitle = "Target File Size, Strict Max, Codecs & Batch",
                badge = "Offline",
                icon = Icons.Rounded.Compress,
                accentColor = Color(0xFF10B981),
                onClick = {
                  onOpenCompressor?.invoke(attachedMedia, 0)
                },
                modifier = Modifier.weight(1f)
              )
              AiToolCard(
                title = "Format Converter",
                subtitle = "JPG, PNG & WebP with Alpha Handling",
                badge = "Codecs",
                icon = Icons.Rounded.Crop,
                accentColor = Color(0xFF38BDF8),
                onClick = {
                  onOpenCompressor?.invoke(attachedMedia, 2)
                },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Recent Operations
        if (recentActions.isNotEmpty()) {
          item {
            Column {
              Text(
                text = "Recent AI Operations",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentActions.forEach { action ->
                  RecentActionRow(action = action)
                }
              }
            }
          }
        }
      } else {
        // DISPLAY CHAT MESSAGES
        items(chatMessages, key = { it.id }) { message ->
          if (message.isUser) {
            UserMessageBubble(message = message, onPhotoClick = onPhotoClick)
          } else {
            AssistantMessageBubble(
              message = message,
              onPhotoClick = onPhotoClick,
              onViewInMainGallery = onViewInMainGallery,
              onOpenAiEditor = {
                message.attachedMedia?.let { onOpenAiEditorWithMedia?.invoke(it) } ?: onOpenAiEditor()
              }
            )
          }
        }

        // Searching/Thinking Indicator
        if (isAiSearching) {
          item {
            AiSearchingBubble()
          }
        }
      }
    }

    // 4. ATTACHED PHOTO PREVIEW BAR (Above input)
    AnimatedVisibility(
      visible = attachedMedia != null,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      if (attachedMedia != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(RoundedCornerShape(6.dp))
              ) {
                ProceduralPhotoArtwork(
                  seed = attachedMedia.colorSeed,
                  category = attachedMedia.categoryTag,
                  modifier = Modifier.fillMaxSize()
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Attached: ${attachedMedia.title}",
                  color = colors.primaryText,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Ask questions, explain objects or extract text",
                  color = colors.secondaryText,
                  fontSize = 10.sp
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = colors.accentGlow,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .clickable {
                    if (attachedMedia != null) {
                      onOpenAiEditorWithMedia?.invoke(attachedMedia) ?: onOpenAiEditor()
                    } else {
                      onOpenAiEditor()
                    }
                  }
                  .padding(end = 4.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "AI Edit",
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              IconButton(
                onClick = onRemoveAttachedMedia,
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Detach image",
                  tint = colors.secondaryText,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }

    // 5. BOTTOM DOCKED INPUT BAR
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
      color = colors.surface,
      tonalElevation = 6.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Attachment Button
        IconButton(
          onClick = { showAttachPickerSheet = true },
          modifier = Modifier.testTag("btn_attach_media")
        ) {
          Icon(
            imageVector = Icons.Rounded.AddPhotoAlternate,
            contentDescription = "Attach Photo for AI",
            tint = if (attachedMedia != null) colors.accent else colors.secondaryText
          )
        }

        // Voice Input Button
        IconButton(
          onClick = { launchVoiceInput() },
          modifier = Modifier.testTag("btn_voice_input")
        ) {
          Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = "Voice Search",
            tint = colors.accent
          )
        }

        // Search Input Text Field
        OutlinedTextField(
          value = currentInput,
          onValueChange = { currentInput = it },
          placeholder = {
            Text(
              text = if (attachedMedia != null) "Ask about this photo…" else "Ask anything about your photos…",
              color = colors.secondaryText,
              fontSize = 13.sp
            )
          },
          singleLine = true,
          trailingIcon = {
            IconButton(
              onClick = {
                if (currentInput.isNotBlank() || attachedMedia != null) {
                  onSubmitQuery(currentInput, attachedMedia)
                  currentInput = ""
                }
              },
              modifier = Modifier.testTag("btn_send_ai_prompt")
            ) {
              Icon(
                imageVector = Icons.Rounded.Send,
                contentDescription = "Send",
                tint = if (currentInput.isNotBlank() || attachedMedia != null) colors.accent else colors.tertiaryText
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border.copy(alpha = 0.7f),
            focusedContainerColor = colors.surfaceVariant,
            unfocusedContainerColor = colors.surfaceVariant
          ),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("input_ai_query")
        )
      }
    }
  }

  // BOTTOM SHEET: PHOTO ATTACHMENT PICKER
  if (showAttachPickerSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAttachPickerSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = colors.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Select Photo for AI Inspection",
            color = colors.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = { showAttachPickerSheet = false }) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = colors.secondaryText)
          }
        }

        Text(
          text = "Choose any photo to inspect objects, extract text or ask questions.",
          color = colors.secondaryText,
          fontSize = 12.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
        ) {
          items(allPhotos, key = { it.id }) { photo ->
            MediaThumbnailCard(
              item = photo,
              isSelected = attachedMedia?.id == photo.id,
              onClick = {
                onAttachMedia(photo)
                showAttachPickerSheet = false
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // BOTTOM SHEET: SEARCH HISTORY
  if (showHistorySheet) {
    ModalBottomSheet(
      onDismissRequest = { showHistorySheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = colors.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Search History",
            color = colors.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          if (searchHistory.isNotEmpty()) {
            Text(
              text = "Clear All",
              color = Color(0xFFEF4444),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.clickable {
                onClearHistory()
                showHistorySheet = false
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchHistory.isEmpty()) {
          Text(
            text = "No previous searches yet.",
            color = colors.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 24.dp)
          )
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(380.dp)
          ) {
            items(searchHistory, key = { it.id }) { item ->
              SearchHistoryRow(
                item = item,
                onSelect = { query ->
                  showHistorySheet = false
                  currentInput = query
                  onSubmitQuery(query, attachedMedia)
                },
                onDelete = { onDeleteHistoryItem(item.id) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

// -----------------------------------------------------------------------------------------
// SUB-COMPONENTS
// -----------------------------------------------------------------------------------------

@Composable
private fun WelcomeAssistantBanner() {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.linearGradient(
          listOf(colors.surface, colors.surfaceVariant)
        )
      )
      .border(1.dp, colors.border, RoundedCornerShape(20.dp))
      .padding(18.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.accent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Ask anything about your photos…",
          color = colors.primaryText,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Find photos by what's in them (red shirt, beach, documents, aadhaar), dates (September, yesterday), albums or colors. Ask questions or attach an image to extract text and analyze details.",
        color = colors.secondaryText,
        fontSize = 12.sp,
        lineHeight = 17.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = Color(0xFF10B981),
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Local offline index • No photos sent to external servers",
          color = Color(0xFF10B981),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun UserMessageBubble(
  message: AiChatMessage,
  onPhotoClick: (MediaItem) -> Unit
) {
  val colors = GalleryTheme.colors
  val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.End
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .background(
          Brush.linearGradient(
            listOf(colors.accent, Color(0xFF8B5CF6))
          )
        )
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Column {
        // Attached Photo Preview inside user bubble
        if (message.attachedMedia != null) {
          Box(
            modifier = Modifier
              .size(120.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable { onPhotoClick(message.attachedMedia) }
              .padding(bottom = 6.dp)
          ) {
            ProceduralPhotoArtwork(
              seed = message.attachedMedia.colorSeed,
              category = message.attachedMedia.categoryTag,
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        Text(
          text = message.text,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = timeFormat.format(Date(message.timestamp)),
      color = colors.tertiaryText,
      fontSize = 10.sp,
      modifier = Modifier.padding(end = 4.dp)
    )
  }
}

@Composable
private fun AssistantMessageBubble(
  message: AiChatMessage,
  onPhotoClick: (MediaItem) -> Unit,
  onViewInMainGallery: (List<MediaItem>, String) -> Unit,
  onOpenAiEditor: (() -> Unit)? = null
) {
  val colors = GalleryTheme.colors
  val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .background(colors.surface)
        .border(1.dp, colors.border, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .padding(14.dp)
    ) {
      Column {
        // Assistant Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Gallery AI",
              color = colors.primaryText,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF10B981).copy(alpha = 0.12f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "On-Device",
              color = Color(0xFF10B981),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Explanation / conversational text
        Text(
          text = message.text,
          color = colors.primaryText,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )

        // Recognized Filter Badges (e.g. September, Red Shirt, Favorites)
        if (message.recognizedBadges.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(message.recognizedBadges) { badge ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(colors.accentGlow)
                  .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(
                  text = badge.label,
                  color = colors.accent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        // Visual Image Analysis Card (if photo was attached & analyzed)
        if (message.analysisResult != null) {
          Spacer(modifier = Modifier.height(10.dp))
          ImageAnalysisDetailsCard(
            result = message.analysisResult,
            onOpenAiEditor = onOpenAiEditor
          )
        }

        // Matching Items Gallery Grid
        if (message.matchingItems.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${message.matchingItems.size} Matching Items",
              color = colors.primaryText,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceVariant)
                .clickable { onViewInMainGallery(message.matchingItems, message.text) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "View in Gallery",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(12.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2-Column Grid of matched items directly in conversation
          val itemsToShow = message.matchingItems.take(6)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsToShow.chunked(2).forEach { rowPair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowPair.forEach { item ->
                  Box(modifier = Modifier.weight(1f)) {
                    MediaThumbnailCard(
                      item = item,
                      aspectRatio = 1.2f,
                      onClick = { onPhotoClick(item) }
                    )
                  }
                }
                if (rowPair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          if (message.matchingItems.size > 6) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "+${message.matchingItems.size - 6} more photos available in main gallery",
              color = colors.secondaryText,
              fontSize = 11.sp,
              modifier = Modifier.clickable { onViewInMainGallery(message.matchingItems, message.text) }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = timeFormat.format(Date(message.timestamp)),
      color = colors.tertiaryText,
      fontSize = 10.sp,
      modifier = Modifier.padding(start = 4.dp)
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageAnalysisDetailsCard(
  result: ImageAnalysisResult,
  onOpenAiEditor: (() -> Unit)? = null
) {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surfaceVariant)
      .border(1.dp, colors.border, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Visual Inspection Details",
          color = colors.primaryText,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )

        if (onOpenAiEditor != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(colors.accentGlow)
              .clickable { onOpenAiEditor() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Edit with AI",
              color = colors.accent,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Detected Objects Chips
      if (result.detectedObjects.isNotEmpty()) {
        Text(
          text = "Detected Objects:",
          color = colors.secondaryText,
          fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          result.detectedObjects.forEach { obj ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(text = obj, color = colors.primaryText, fontSize = 11.sp)
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // OCR / Extracted text
      if (result.extractedText.isNotBlank()) {
        Text(
          text = "Extracted Text (OCR):",
          color = colors.secondaryText,
          fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
        ) {
          Text(
            text = result.extractedText,
            color = colors.accent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Technical properties
      result.technicalDetails["Resolution"]?.let {
        Text(text = "• Resolution: $it", color = colors.secondaryText, fontSize = 11.sp)
      }
      result.technicalDetails["File Size"]?.let {
        Text(text = "• Size: $it", color = colors.secondaryText, fontSize = 11.sp)
      }
      result.technicalDetails["Camera"]?.let {
        Text(text = "• Camera: $it", color = colors.secondaryText, fontSize = 11.sp)
      }
    }
  }
}

@Composable
private fun AiSearchingBubble() {
  val colors = GalleryTheme.colors
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surface)
      .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = colors.accent,
        strokeWidth = 2.dp
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "Searching gallery locally with AI vision…",
        color = colors.secondaryText,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun SearchHistoryRow(
  item: SearchHistoryEntity,
  onSelect: (String) -> Unit,
  onDelete: () -> Unit
) {
  val colors = GalleryTheme.colors
  val dateFormat = remember { SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(colors.surface)
      .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
      .clickable { onSelect(item.query) }
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("history_item_${item.id}")
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
        Icon(
          imageVector = Icons.Rounded.History,
          contentDescription = null,
          tint = colors.secondaryText,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = item.query,
            color = colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${dateFormat.format(Date(item.timestamp))} • ${item.resultCount} found",
            color = colors.secondaryText,
            fontSize = 10.sp
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(24.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Delete history item",
          tint = colors.secondaryText,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
private fun AiToolCard(
  title: String,
  subtitle: String,
  badge: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surface)
      .border(1.dp, colors.border, RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("ai_tool_${title.lowercase().replace(" ", "_")}")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = badge,
            color = colors.secondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = title,
        color = colors.primaryText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = subtitle,
        color = colors.secondaryText,
        fontSize = 11.sp,
        lineHeight = 15.sp
      )
    }
  }
}

@Composable
private fun RecentActionRow(
  action: AiActionItem
) {
  val colors = GalleryTheme.colors

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surface)
      .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(colors.accentGlow),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(16.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = action.title,
            color = colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = action.timestamp,
            color = colors.tertiaryText,
            fontSize = 10.sp
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = action.subtitle,
          color = colors.secondaryText,
          fontSize = 11.sp
        )
      }
    }
  }
}
