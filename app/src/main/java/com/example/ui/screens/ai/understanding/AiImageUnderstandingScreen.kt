package com.example.ui.screens.ai.understanding

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.understanding.GeminiImageUnderstandingProvider
import com.example.data.editor.PhotoEditorEngine
import com.example.model.MediaItem
import com.example.model.ai.understanding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Message model for active image understanding chat session
 */
data class UnderstandingMessage(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val task: ImageUnderstandingTask = ImageUnderstandingTask.ASK_ANYTHING,
  val timestamp: Long = System.currentTimeMillis(),
  val result: ImageUnderstandingResult? = null,
  val cropRegion: ImageCropRegion? = null
)

/**
 * Saved Session item for History Drawer
 */
data class SavedUnderstandingSession(
  val id: String,
  val mediaUri: String,
  val mediaTitle: String,
  val category: String,
  val title: String,
  val dateFormatted: String,
  val lastPreview: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiImageUnderstandingScreen(
  sourceUri: Uri,
  mediaTitle: String = "Photo",
  isFromVault: Boolean = false,
  userApiKey: String = "",
  askBeforeSendingToAi: Boolean = true,
  initialTask: ImageUnderstandingTask = ImageUnderstandingTask.ASK_ANYTHING,
  initialPrompt: String? = null,
  allGalleryPhotos: List<MediaItem> = emptyList(),
  onSaveMetadataToCache: ((mediaId: String, text: String, desc: String, objects: String) -> Unit)? = null,
  onUpdateAskBeforeSendingPreference: (Boolean) -> Unit,
  onSaveApiKey: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  // Bitmap Loading
  var currentSourceUri by remember { mutableStateOf(sourceUri) }
  var currentMediaTitle by remember { mutableStateOf(mediaTitle) }
  var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoadingSource by remember { mutableStateOf(true) }

  // Task & Input
  var activeTask by remember { mutableStateOf(initialTask) }
  var currentInput by remember { mutableStateOf(initialPrompt ?: "") }
  var mathLevel by remember { mutableStateOf(MathExplanationLevel.STEP_BY_STEP) }
  var targetLanguage by remember { mutableStateOf(TranslationLanguage.HINDI) }
  var translationMode by remember { mutableStateOf(TranslationDisplayMode.TEXT) }
  var specificQuestionFilter by remember { mutableStateOf<String?>(null) }

  // Active Crop / Region Selection
  var activeCropRegion by remember { mutableStateOf<ImageCropRegion?>(null) }
  var showRegionSelectorDialog by remember { mutableStateOf(false) }

  // Multi-turn messages
  val conversationMessages = remember { mutableStateListOf<UnderstandingMessage>() }
  var isProcessing by remember { mutableStateOf(false) }
  var processingStatusText by remember { mutableStateOf("Analyzing image...") }
  var activeJob by remember { mutableStateOf<Job?>(null) }

  // History & Photo Switcher Sheets
  var showHistorySheet by remember { mutableStateOf(false) }
  var showPhotoPickerSheet by remember { mutableStateOf(false) }
  val savedSessions = remember { mutableStateListOf<SavedUnderstandingSession>() }

  // Dialogs
  var showConsentDialog by remember { mutableStateOf(false) }
  var rememberConsentChoice by remember { mutableStateOf(false) }
  var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  var showApiKeyDialog by remember { mutableStateOf(false) }
  var enteredApiKey by remember { mutableStateOf(userApiKey) }

  var showVaultAuthDialog by remember { mutableStateOf(false) }
  var vaultPinInput by remember { mutableStateOf("") }

  // Editing Extracted Text Dialog
  var editingOcrText by remember { mutableStateOf<String?>(null) }

  val hasValidKey = remember(userApiKey) {
    GeminiImageUnderstandingProvider.hasApiKey(userApiKey)
  }

  // Load Source Bitmap
  LaunchedEffect(currentSourceUri) {
    isLoadingSource = true
    withContext(Dispatchers.IO) {
      val bmp = PhotoEditorEngine.loadBitmap(context, currentSourceUri, maxDimension = 1920)
      withContext(Dispatchers.Main) {
        sourceBitmap = bmp
        isLoadingSource = false
      }
    }
  }

  // Auto-scroll on new message
  LaunchedEffect(conversationMessages.size, isProcessing) {
    if (conversationMessages.isNotEmpty()) {
      listState.animateScrollToItem(conversationMessages.size)
    }
  }

  // Speech Recognition Activity Launcher
  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
      if (!spoken.isNullOrBlank()) {
        currentInput = spoken
      }
    }
  }

  fun launchVoiceInput() {
    try {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about this image in Hindi or English…")
      }
      speechLauncher.launch(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "Voice input not available on device", Toast.LENGTH_SHORT).show()
    }
  }

  // Execution engine
  val executeTask: (String, ImageUnderstandingTask) -> Unit = { userPrompt, task ->
    val bmp = sourceBitmap
    if (bmp == null) {
      Toast.makeText(context, "Image is still loading...", Toast.LENGTH_SHORT).show()
    } else if (!hasValidKey) {
      showApiKeyDialog = true
    } else {
      isProcessing = true
      val userMsg = UnderstandingMessage(
        isUser = true,
        text = userPrompt.ifBlank { task.defaultPromptHindi },
        task = task,
        cropRegion = activeCropRegion
      )
      conversationMessages.add(userMsg)

      activeJob = scope.launch {
        processingStatusText = "Uploading image to Google Gemini..."
        delay(400)
        processingStatusText = when (task) {
          ImageUnderstandingTask.EXTRACT_TEXT -> "Extracting text, numbers and line layout..."
          ImageUnderstandingTask.SOLVE_MATH -> "Parsing mathematical notation and solving step-by-step..."
          ImageUnderstandingTask.TRANSLATE -> "Detecting language and translating to ${targetLanguage.displayName}..."
          ImageUnderstandingTask.READ_TABLE -> "Detecting rows, columns and cell contents..."
          ImageUnderstandingTask.ANALYZE_CHART -> "Reading chart axes, data points and trends..."
          ImageUnderstandingTask.SUMMARIZE_DOC -> "Extracting dates, amounts, IDs and summary..."
          ImageUnderstandingTask.IDENTIFY_OBJECTS -> "Mapping objects and spatial coordinates..."
          else -> "Analyzing image context..."
        }

        // Construct history turns
        val historyTurns = conversationMessages
          .dropLast(1)
          .takeLast(6)
          .map { Pair(it.text, it.isUser) }

        val res = GeminiImageUnderstandingProvider.processImageUnderstanding(
          context = context,
          bitmap = bmp,
          task = task,
          userPrompt = userPrompt,
          cropRegion = activeCropRegion,
          historyMessages = historyTurns,
          targetLanguage = targetLanguage,
          mathLevel = mathLevel,
          specificQuestionNumber = specificQuestionFilter,
          userApiKey = userApiKey
        )

        isProcessing = false

        val assistantMsg = UnderstandingMessage(
          isUser = false,
          text = if (res.isSuccess) res.mainTextResponse else (res.errorMessage ?: "Failed to analyze image."),
          task = task,
          result = res
        )
        conversationMessages.add(assistantMsg)

        // Cache metadata for local search if successful
        if (res.isSuccess) {
          val extracted = res.extractedText ?: ""
          val desc = if (task == ImageUnderstandingTask.DESCRIBE_IMAGE) res.mainTextResponse else ""
          val objects = res.detectedObjects.joinToString(", ") { it.name }
          onSaveMetadataToCache?.invoke(currentSourceUri.toString(), extracted, desc, objects)
        }
      }
    }
  }

  // Consent & Vault Guard
  val triggerWithConsent: (String, ImageUnderstandingTask) -> Unit = { prompt, task ->
    val action = { executeTask(prompt, task) }
    if (isFromVault) {
      showVaultAuthDialog = true
      pendingAction = action
    } else if (askBeforeSendingToAi) {
      pendingAction = action
      showConsentDialog = true
    } else {
      action()
    }
  }

  BackHandler {
    onClose()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0D13))
      .testTag("screen_ai_image_understanding")
  ) {
    if (isLoadingSource) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = Color(0xFF38BDF8))
          Spacer(modifier = Modifier.height(14.dp))
          Text("Loading image for AI Understanding...", color = Color.White, fontSize = 14.sp)
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        // 1. TOP BAR
        Surface(
          color = Color(0xFF131722),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onClose, modifier = Modifier.testTag("btn_ai_understanding_back")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close", tint = Color.White)
              }
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text(
                  text = "AI Image Understanding",
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = currentMediaTitle,
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              // Region selection button
              IconButton(
                onClick = { showRegionSelectorDialog = true },
                modifier = Modifier.testTag("btn_select_region")
              ) {
                Icon(
                  imageVector = if (activeCropRegion != null && !activeCropRegion!!.isFullImage) Icons.Rounded.CropSquare else Icons.Rounded.Crop,
                  contentDescription = "Select Region",
                  tint = if (activeCropRegion != null && !activeCropRegion!!.isFullImage) Color(0xFF38BDF8) else Color.White
                )
              }

              // Change photo button
              IconButton(
                onClick = { showPhotoPickerSheet = true },
                modifier = Modifier.testTag("btn_change_photo")
              ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Change Image", tint = Color.White)
              }

              // Clear chat
              if (conversationMessages.isNotEmpty()) {
                IconButton(
                  onClick = { conversationMessages.clear() },
                  modifier = Modifier.testTag("btn_clear_ai_conversation")
                ) {
                  Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Chat", tint = Color(0xFF94A3B8))
                }
              }
            }
          }
        }

        // 2. API KEY STATUS BANNER
        if (!hasValidKey) {
          Surface(
            color = Color(0xFF7C2D12),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showApiKeyDialog = true }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Key, contentDescription = null, tint = Color(0xFFFDBA74), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini API Key Required for Image Understanding", color = Color.White, fontSize = 11.sp)
              }
              Text("Enter Key", color = Color(0xFFFDBA74), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // 3. MAIN CONVERSATION / CONTENT AREA
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // LARGE IMAGE PREVIEW CARD AT TOP
          item {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF131722))
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                sourceBitmap?.let { bmp ->
                  var zoom by remember { mutableFloatStateOf(1f) }
                  var pan by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Active Image Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                      .fillMaxSize()
                      .pointerInput(Unit) {
                        detectTransformGestures { _, panDelta, zoomDelta, _ ->
                          zoom = (zoom * zoomDelta).coerceIn(1f, 3f)
                          pan = if (zoom > 1f) pan + panDelta else androidx.compose.ui.geometry.Offset.Zero
                        }
                      }
                      .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = pan.x,
                        translationY = pan.y
                      )
                  )
                }

                // Region Selection Badge if cropped
                if (activeCropRegion != null && !activeCropRegion!!.isFullImage) {
                  Surface(
                    color = Color(0xFF0369A1).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .align(Alignment.TopStart)
                      .padding(8.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Rounded.Crop, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Region Selected", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear Region",
                        tint = Color.White,
                        modifier = Modifier
                          .size(14.dp)
                          .clickable { activeCropRegion = null }
                      )
                    }
                  }
                }
              }
            }
          }

          // WORKSPACE ACTION TABS ROW
          item {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(ImageUnderstandingTask.values().filter { it != ImageUnderstandingTask.CUSTOM_QUESTION }) { task ->
                val isSelected = task == activeTask
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    activeTask = task
                    // Auto-execute default action if no messages yet
                    if (conversationMessages.isEmpty()) {
                      triggerWithConsent(task.defaultPromptHindi, task)
                    }
                  },
                  label = {
                    Text(task.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color.Black,
                    containerColor = Color(0xFF1E2330),
                    labelColor = Color(0xFFCBD5E1)
                  )
                )
              }
            }
          }

          // TASK-SPECIFIC CONFIGURATION BARS
          item {
            when (activeTask) {
              ImageUnderstandingTask.SOLVE_MATH -> {
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF182030)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text("Explanation Depth:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MathExplanationLevel.values().forEach { level ->
                          Surface(
                            color = if (mathLevel == level) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                              .clip(RoundedCornerShape(6.dp))
                              .clickable { mathLevel = level }
                              .padding(horizontal = 8.dp, vertical = 4.dp)
                          ) {
                            Text(
                              text = level.displayName,
                              color = if (mathLevel == level) Color.Black else Color.White,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.SemiBold
                            )
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      val mathQuickPrompts = listOf(
                        "Q1 solve करो" to "Solve question 1 step-by-step",
                        "Q5 solve करो" to "Solve question 5 with formula and steps",
                        "Only Q3 और Q7 solve करो" to "Solve only question 3 and question 7",
                        "सभी questions solve करो" to "Identify and solve all math questions on this page"
                      )
                      mathQuickPrompts.forEach { (label, pr) ->
                        Surface(
                          color = Color(0xFF283548),
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier
                            .clickable {
                              currentInput = pr
                              triggerWithConsent(pr, ImageUnderstandingTask.SOLVE_MATH)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text(label, color = Color(0xFF38BDF8), fontSize = 10.sp)
                        }
                      }
                    }
                  }
                }
              }

              ImageUnderstandingTask.TRANSLATE -> {
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF182030)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("Translate into:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      items(TranslationLanguage.values()) { lang ->
                        val isLangSelected = targetLanguage == lang
                        Surface(
                          color = if (isLangSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier
                            .clickable {
                              targetLanguage = lang
                              triggerWithConsent(
                                "Translate all text in this image into ${lang.displayName}",
                                ImageUnderstandingTask.TRANSLATE
                              )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text(
                            text = "${lang.displayName} (${lang.nativeName})",
                            color = if (isLangSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                          )
                        }
                      }
                    }
                  }
                }
              }

              else -> {}
            }
          }

          // CONVERSATION MESSAGE THREAD
          if (conversationMessages.isEmpty() && !isProcessing) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(36.dp)
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "Ask anything about this image",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Select a tool above or type/speak questions in Hindi, English or Hinglish.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(16.dp))

                  // Quick Suggestion Chips
                  val starterPrompts = listOf(
                    "इस image में क्या है?" to ImageUnderstandingTask.ASK_ANYTHING,
                    "सारा text extract करो" to ImageUnderstandingTask.EXTRACT_TEXT,
                    "इस document का summary बताओ" to ImageUnderstandingTask.SUMMARIZE_DOC,
                    "Q5 solve करो" to ImageUnderstandingTask.SOLVE_MATH,
                    "Table extract करो" to ImageUnderstandingTask.READ_TABLE,
                    "इस graph का summary और trend बताओ" to ImageUnderstandingTask.ANALYZE_CHART,
                    "Hindi में translate करो" to ImageUnderstandingTask.TRANSLATE
                  )
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    starterPrompts.forEach { (text, task) ->
                      Surface(
                        color = Color(0xFF1E2330),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            currentInput = text
                            triggerWithConsent(text, task)
                          }
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(10.dp))
                          Text(text, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          items(conversationMessages, key = { it.id }) { msg ->
            if (msg.isUser) {
              UserMessageCard(message = msg)
            } else {
              AssistantUnderstandingCard(
                message = msg,
                onEditOcr = { textToEdit -> editingOcrText = textToEdit },
                onTranslateText = { textToTranslate ->
                  currentInput = "Translate this: $textToTranslate"
                  triggerWithConsent("Translate this extracted text into ${targetLanguage.displayName}: $textToTranslate", ImageUnderstandingTask.TRANSLATE)
                },
                onSendToAi = { textPrompt ->
                  currentInput = textPrompt
                  triggerWithConsent(textPrompt, ImageUnderstandingTask.ASK_ANYTHING)
                }
              )
            }
          }

          // Processing State Card
          if (isProcessing) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF182030)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Gemini 2.5 Flash Multimodal", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(processingStatusText, color = Color.White, fontSize = 12.sp)
                  }
                  TextButton(
                    onClick = {
                      activeJob?.cancel()
                      isProcessing = false
                    }
                  ) {
                    Text("Cancel", color = Color(0xFFEF4444), fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }

        // 4. BOTTOM INPUT BAR
        Surface(
          color = Color(0xFF131722),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = currentInput,
              onValueChange = { currentInput = it },
              placeholder = {
                Text(
                  text = when (activeTask) {
                    ImageUnderstandingTask.EXTRACT_TEXT -> "Extract text or select region..."
                    ImageUnderstandingTask.SOLVE_MATH -> "E.g. 'Q5 solve करो', 'All questions'..."
                    ImageUnderstandingTask.TRANSLATE -> "Translate image text into Hindi..."
                    ImageUnderstandingTask.READ_TABLE -> "Extract rows and columns..."
                    ImageUnderstandingTask.ANALYZE_CHART -> "Ask about trends, highest value..."
                    ImageUnderstandingTask.SUMMARIZE_DOC -> "Summary, dates, amounts, names..."
                    else -> "Ask anything about this image..."
                  },
                  color = Color(0xFF94A3B8),
                  fontSize = 12.sp
                )
              },
              trailingIcon = {
                IconButton(onClick = { launchVoiceInput() }) {
                  Icon(Icons.Rounded.Mic, contentDescription = "Voice Input", tint = Color(0xFF38BDF8))
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E2330),
                unfocusedContainerColor = Color(0xFF1E2330)
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("input_ai_understanding_query")
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
              onClick = {
                if (currentInput.isNotBlank()) {
                  val prompt = currentInput
                  currentInput = ""
                  triggerWithConsent(prompt, activeTask)
                } else {
                  triggerWithConsent(activeTask.defaultPromptHindi, activeTask)
                }
              },
              enabled = !isProcessing,
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8))
                .testTag("btn_send_ai_query")
            ) {
              Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.Black)
            }
          }
        }
      }
    }

    // 5. REGION SELECTION DIALOG (CROP / QUESTION SELECTION)
    if (showRegionSelectorDialog && sourceBitmap != null) {
      ImageRegionSelectorDialog(
        sourceBitmap = sourceBitmap!!,
        initialCropRegion = activeCropRegion,
        onRegionSelected = { selectedRegion ->
          activeCropRegion = selectedRegion
          showRegionSelectorDialog = false
          Toast.makeText(context, "Region set for focused AI analysis", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { showRegionSelectorDialog = false }
      )
    }

    // 6. PHOTO PICKER SHEET (SWITCH IMAGE)
    if (showPhotoPickerSheet) {
      ModalBottomSheet(
        onDismissRequest = { showPhotoPickerSheet = false },
        containerColor = Color(0xFF131722)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Text("Select Photo for AI Understanding", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(12.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(allGalleryPhotos.filter { it.uri != null }) { photo ->
              Card(
                modifier = Modifier
                  .size(100.dp)
                  .clickable {
                    currentSourceUri = Uri.parse(photo.uri!!)
                    currentMediaTitle = photo.title
                    activeCropRegion = null
                    conversationMessages.clear()
                    showPhotoPickerSheet = false
                  },
                shape = RoundedCornerShape(10.dp)
              ) {
                Box(contentAlignment = Alignment.BottomStart) {
                  Text(photo.title, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }

    // 7. CLOUD PRIVACY CONSENT DIALOG (Prompt 9 Mandate)
    if (showConsentDialog) {
      AlertDialog(
        onDismissRequest = {
          showConsentDialog = false
          pendingAction = null
        },
        icon = {
          Icon(Icons.Rounded.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
        },
        title = {
          Text("Online AI Image Understanding", fontWeight = FontWeight.Bold)
        },
        text = {
          Column {
            Text(
              text = "This image will be sent to an online AI service (Google Gemini) for analysis.\n\nOnly the explicitly selected image is sent. Your entire gallery and Vault remain completely private and on-device.",
              fontSize = 13.sp,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { rememberConsentChoice = !rememberConsentChoice }
            ) {
              Checkbox(
                checked = rememberConsentChoice,
                onCheckedChange = { rememberConsentChoice = it }
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Remember choice (Don't ask again)", fontSize = 12.sp)
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showConsentDialog = false
              if (rememberConsentChoice) {
                onUpdateAskBeforeSendingPreference(false)
              }
              pendingAction?.invoke()
              pendingAction = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
          ) {
            Text("Continue")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showConsentDialog = false
              pendingAction = null
            }
          ) {
            Text("Cancel")
          }
        }
      )
    }

    // 8. VAULT AUTHENTICATION DIALOG
    if (showVaultAuthDialog) {
      AlertDialog(
        onDismissRequest = {
          showVaultAuthDialog = false
          pendingAction = null
        },
        icon = {
          Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(32.dp))
        },
        title = {
          Text("Vault Protected Image", fontWeight = FontWeight.Bold)
        },
        text = {
          Column {
            Text(
              text = "This image is in your Private Vault. Please enter your PIN to confirm sending ONLY this item to online AI.",
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = vaultPinInput,
              onValueChange = { vaultPinInput = it },
              placeholder = { Text("Enter Vault PIN") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showVaultAuthDialog = false
              vaultPinInput = ""
              // Authenticated, now check cloud consent if enabled
              if (askBeforeSendingToAi) {
                showConsentDialog = true
              } else {
                pendingAction?.invoke()
                pendingAction = null
              }
            }
          ) {
            Text("Authorize & Send")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showVaultAuthDialog = false
              pendingAction = null
            }
          ) {
            Text("Cancel")
          }
        }
      )
    }

    // 9. API KEY CONFIGURATION DIALOG
    if (showApiKeyDialog) {
      AlertDialog(
        onDismissRequest = { showApiKeyDialog = false },
        icon = {
          Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(30.dp))
        },
        title = {
          Text("Gemini API Key", fontWeight = FontWeight.Bold)
        },
        text = {
          Column {
            Text(
              text = "Online AI Image Understanding requires a Google Gemini API Key. You can get one for free at ai.google.dev.",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = enteredApiKey,
              onValueChange = { enteredApiKey = it },
              placeholder = { Text("Paste your AI Studio Key here") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              onSaveApiKey(enteredApiKey)
              showApiKeyDialog = false
              Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
            }
          ) {
            Text("Save Key")
          }
        },
        dismissButton = {
          TextButton(onClick = { showApiKeyDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    // 10. EDIT EXTRACTED TEXT DIALOG
    if (editingOcrText != null) {
      AlertDialog(
        onDismissRequest = { editingOcrText = null },
        title = { Text("Edit Extracted Text") },
        text = {
          OutlinedTextField(
            value = editingOcrText ?: "",
            onValueChange = { editingOcrText = it },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 180.dp)
          )
        },
        confirmButton = {
          Button(
            onClick = {
              Toast.makeText(context, "Text updated", Toast.LENGTH_SHORT).show()
              editingOcrText = null
            }
          ) {
            Text("Done")
          }
        },
        dismissButton = {
          TextButton(onClick = { editingOcrText = null }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

// -------------------------------------------------------------
// COMPONENT: USER MESSAGE CARD
// -------------------------------------------------------------
@Composable
private fun UserMessageCard(message: UnderstandingMessage) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.CenterEnd
  ) {
    Surface(
      color = Color(0xFF2563EB),
      shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
      modifier = Modifier.widthIn(max = 300.dp)
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (message.cropRegion != null && !message.cropRegion.isFullImage) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Rounded.Crop, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Specific Region Focus", color = Color.White, fontSize = 9.sp)
          }
          Spacer(modifier = Modifier.height(4.dp))
        }
        Text(message.text, color = Color.White, fontSize = 13.sp)
      }
    }
  }
}

// -------------------------------------------------------------
// COMPONENT: ASSISTANT UNDERSTANDING CARD
// -------------------------------------------------------------
@Composable
private fun AssistantUnderstandingCard(
  message: UnderstandingMessage,
  onEditOcr: (String) -> Unit,
  onTranslateText: (String) -> Unit,
  onSendToAi: (String) -> Unit
) {
  val context = LocalContext.current
  val res = message.result

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24)),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Operation Title & Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = when (message.task) {
              ImageUnderstandingTask.EXTRACT_TEXT -> Icons.Rounded.DocumentScanner
              ImageUnderstandingTask.SOLVE_MATH -> Icons.Rounded.Calculate
              ImageUnderstandingTask.TRANSLATE -> Icons.Rounded.Translate
              ImageUnderstandingTask.READ_TABLE -> Icons.Rounded.TableChart
              ImageUnderstandingTask.ANALYZE_CHART -> Icons.Rounded.BarChart
              ImageUnderstandingTask.SUMMARIZE_DOC -> Icons.Rounded.Description
              ImageUnderstandingTask.IDENTIFY_OBJECTS -> Icons.Rounded.Category
              else -> Icons.Rounded.AutoAwesome
            },
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = message.task.displayName,
            color = Color(0xFF38BDF8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Quick Share / Copy actions
        Row {
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("AI Result", message.text)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
          }

          IconButton(
            onClick = {
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message.text)
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Result"))
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 1. EXTRACTED TEXT (OCR)
      if (message.task == ImageUnderstandingTask.EXTRACT_TEXT && res?.extractedText != null) {
        Surface(
          color = Color(0xFF1E2330),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = res.extractedText,
              color = Color.White,
              fontSize = 13.sp,
              lineHeight = 19.sp,
              fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Action row for OCR
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedButton(
                onClick = { onEditOcr(res.extractedText) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 11.sp)
              }

              OutlinedButton(
                onClick = {
                  try {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloads, "ExtractedText_${System.currentTimeMillis()}.txt")
                    FileOutputStream(file).use { it.write(res.extractedText.toByteArray()) }
                    Toast.makeText(context, "Saved TXT: ${file.name}", Toast.LENGTH_SHORT).show()
                  } catch (e: Exception) {
                    Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                  }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save TXT", fontSize = 11.sp)
              }

              OutlinedButton(
                onClick = { onTranslateText(res.extractedText) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Translate", fontSize = 11.sp)
              }

              OutlinedButton(
                onClick = { onSendToAi(res.extractedText) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send to AI", fontSize = 11.sp)
              }
            }
          }
        }
      }

      // 2. MATH SOLVER RESULTS
      else if (message.task == ImageUnderstandingTask.SOLVE_MATH && res?.mathResults?.isNotEmpty() == true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          res.mathResults.forEach { math ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(math.questionNumber, color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  if (math.isCalculatorVerified) {
                    Surface(
                      color = Color(0xFF065F46),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Calculator Verified", color = Color(0xFF6EE7B7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }

                if (math.questionText.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(math.questionText, color = Color(0xFFCBD5E1), fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }

                if (math.method.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Method: ${math.method}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }

                if (math.steps.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("Step-by-step Solution:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  math.steps.forEach { step ->
                    Text(step, color = Color(0xFFE2E8F0), fontSize = 11.sp, modifier = Modifier.padding(vertical = 1.dp))
                  }
                }

                if (math.finalAnswer.isNotBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Surface(
                    color = Color(0xFF064E3B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Final Answer: ${math.finalAnswer}",
                      color = Color(0xFF6EE7B7),
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }

                if (math.verificationDetails != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(math.verificationDetails, color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
              }
            }
          }
        }
      }

      // 3. TABLE VIEWER
      else if (message.task == ImageUnderstandingTask.READ_TABLE && res?.tableData != null) {
        ExtractedTableViewer(tableData = res.tableData)
      }

      // 4. TRANSLATION (Side-by-side or text)
      else if (message.task == ImageUnderstandingTask.TRANSLATE && res?.translatedText != null) {
        Column {
          if (res.detectedLanguage != null) {
            Text("Original Language: ${res.detectedLanguage}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
          }
          Surface(
            color = Color(0xFF1E2330),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = res.translatedText,
              color = Color.White,
              fontSize = 13.sp,
              lineHeight = 19.sp,
              modifier = Modifier.padding(12.dp)
            )
          }
        }
      }

      // 5. DOCUMENT DETAILS
      else if (message.task == ImageUnderstandingTask.SUMMARIZE_DOC && res?.documentDetails != null) {
        val doc = res.documentDetails
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(doc.summary, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)

          if (doc.dates.isNotEmpty() || doc.amounts.isNotEmpty() || doc.ids.isNotEmpty()) {
            Surface(
              color = Color(0xFF1E2330),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (doc.dates.isNotEmpty()) {
                  Text("Dates: ${doc.dates.joinToString(", ")}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
                if (doc.amounts.isNotEmpty()) {
                  Text("Amounts: ${doc.amounts.joinToString(", ")}", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (doc.ids.isNotEmpty()) {
                  Text("IDs: ${doc.ids.joinToString(", ")}", color = Color(0xFFFBBF24), fontSize = 11.sp)
                }
              }
            }
          }
        }
      }

      // 6. OBJECT DETECTION WITH SPATIAL LABELS
      else if (message.task == ImageUnderstandingTask.IDENTIFY_OBJECTS && res?.detectedObjects?.isNotEmpty() == true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          res.detectedObjects.forEach { obj ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2330), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(obj.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
              Surface(
                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = obj.spatialPosition,
                  color = Color(0xFF38BDF8),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }

      // 7. GENERAL TEXT EXPLANATION / CHAT
      else {
        Text(
          text = message.text,
          color = Color(0xFFE2E8F0),
          fontSize = 13.sp,
          lineHeight = 19.sp
        )
      }
    }
  }
}
