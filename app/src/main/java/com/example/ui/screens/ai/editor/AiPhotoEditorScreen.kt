package com.example.ui.screens.ai.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.editor.GeminiAiPhotoEditorEngine
import com.example.data.editor.PhotoEditorEngine
import com.example.model.ai.*
import com.example.model.editor.CompareMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPhotoEditorScreen(
  sourceUri: Uri,
  mediaTitle: String = "Photo",
  userApiKey: String = "",
  askBeforeSendingToAi: Boolean = true,
  initialPrompt: String? = null,
  onSaveExport: (Bitmap, String) -> Unit,
  onOpenInProEditor: (Bitmap, String) -> Unit,
  onUpdateAskBeforeSendingPreference: (Boolean) -> Unit,
  onSaveApiKey: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Bitmaps
  var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isLoadingSource by remember { mutableStateOf(true) }

  // Generation State & Job
  var isGenerating by remember { mutableStateOf(false) }
  var generationStepText by remember { mutableStateOf("Preparing request...") }
  var activeJob by remember { mutableStateOf<Job?>(null) }

  // Tool Category & Prompt State
  var activeCategory by remember { mutableStateOf(AiEditorToolCategory.PROMPT) }
  var promptInput by remember { mutableStateOf(initialPrompt ?: "") }
  var relightStrength by remember { mutableFloatStateOf(0.8f) }

  // Object Mask Strokes
  val maskStrokes = remember { mutableStateListOf<AiMaskStroke>() }
  var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
  var maskBrushSize by remember { mutableFloatStateOf(40f) }

  // History & Undo / Redo
  val undoStack = remember { mutableStateListOf<AiGenerationStep>() }
  val redoStack = remember { mutableStateListOf<AiGenerationStep>() }

  // Compare Mode
  var compareMode by remember { mutableStateOf(CompareMode.NONE) }
  var splitSliderPosition by remember { mutableFloatStateOf(0.5f) }

  // Dialogs
  var showConsentDialog by remember { mutableStateOf(false) }
  var rememberConsentChoice by remember { mutableStateOf(false) }
  var pendingExecutionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  var showApiKeyDialog by remember { mutableStateOf(false) }
  var enteredApiKey by remember { mutableStateOf(userApiKey) }

  var showUnsavedDialog by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember { mutableStateOf<String?>(null) }

  // Effective API key check
  val hasValidKey = remember(userApiKey) {
    GeminiAiPhotoEditorEngine.hasApiKey(userApiKey)
  }

  // Load original bitmap
  LaunchedEffect(sourceUri) {
    isLoadingSource = true
    withContext(Dispatchers.IO) {
      val bmp = PhotoEditorEngine.loadBitmap(context, sourceUri, maxDimension = 1920)
      withContext(Dispatchers.Main) {
        originalBitmap = bmp
        currentBitmap = bmp
        isLoadingSource = false
      }
    }
  }

  // Speech Recognition Activity Launcher
  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
      if (!spoken.isNullOrBlank()) {
        promptInput = spoken
      }
    }
  }

  fun launchVoiceInput() {
    try {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your photo edit (Hindi or English)…")
      }
      speechLauncher.launch(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "Voice input not available on device", Toast.LENGTH_SHORT).show()
    }
  }

  // Trigger AI generation
  val performGeneration: (String) -> Unit = { finalPrompt ->
    val base = currentBitmap ?: originalBitmap
    if (base == null) {
      errorMessage = "Source photo is still loading. Please wait a moment."
    } else if (!hasValidKey) {
      showApiKeyDialog = true
    } else {
      isGenerating = true
      errorMessage = null
      infoMessage = null

      activeJob = scope.launch {
        // Multi-stage progress feedback
        generationStepText = "Uploading photo to Google Gemini..."
        delay(400)
        generationStepText = "Processing AI image editing model..."
        delay(600)
        generationStepText = "Synthesizing high-fidelity result..."

        val result = GeminiAiPhotoEditorEngine.generateEdit(
          context = context,
          baseBitmap = base,
          category = activeCategory,
          userPrompt = finalPrompt,
          maskStrokes = if (activeCategory == AiEditorToolCategory.OBJECTS) maskStrokes.toList() else emptyList(),
          userApiKey = userApiKey,
          relightStrength = relightStrength
        )

        isGenerating = false

        if (result.isSuccess && result.resultBitmap != null) {
          // Push old state to undo stack
          val step = AiGenerationStep(
            bitmap = base,
            prompt = finalPrompt,
            category = activeCategory
          )
          undoStack.add(step)
          redoStack.clear()

          currentBitmap = result.resultBitmap
          infoMessage = result.message ?: "AI edit applied successfully!"
          // Clear mask after successful eraser/fill
          maskStrokes.clear()
        } else {
          errorMessage = result.errorMessage ?: "Failed to generate edit with AI."
        }
      }
    }
  }

  // Wrapper verifying consent before sending photo
  val initiateEditWithConsent: (String) -> Unit = { promptToRun ->
    if (askBeforeSendingToAi) {
      pendingExecutionAction = { performGeneration(promptToRun) }
      showConsentDialog = true
    } else {
      performGeneration(promptToRun)
    }
  }

  BackHandler {
    if (undoStack.isNotEmpty()) {
      showUnsavedDialog = true
    } else {
      onClose()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0D0F15))
      .testTag("screen_ai_photo_editor")
  ) {
    if (isLoadingSource) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = Color(0xFF38BDF8))
          Spacer(modifier = Modifier.height(14.dp))
          Text("Preparing photo for Gemini AI Studio...", color = Color.White, fontSize = 14.sp)
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        // 1. TOP BAR
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color(0xFF131722).copy(alpha = 0.95f)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(
              onClick = {
                if (undoStack.isNotEmpty()) showUnsavedDialog = true else onClose()
              },
              modifier = Modifier.testTag("btn_ai_editor_back")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Close",
                tint = Color.White
              )
            }

            // Undo / Redo & Compare controls
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  if (undoStack.isNotEmpty()) {
                    val prev = undoStack.removeAt(undoStack.lastIndex)
                    currentBitmap?.let { cur ->
                      redoStack.add(
                        AiGenerationStep(
                          bitmap = cur,
                          prompt = prev.prompt,
                          category = prev.category
                        )
                      )
                    }
                    currentBitmap = prev.bitmap
                  }
                },
                enabled = undoStack.isNotEmpty(),
                modifier = Modifier.testTag("btn_ai_undo")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Undo,
                  contentDescription = "Undo",
                  tint = if (undoStack.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f)
                )
              }

              IconButton(
                onClick = {
                  if (redoStack.isNotEmpty()) {
                    val next = redoStack.removeAt(redoStack.lastIndex)
                    currentBitmap?.let { cur ->
                      undoStack.add(
                        AiGenerationStep(
                          bitmap = cur,
                          prompt = next.prompt,
                          category = next.category
                        )
                      )
                    }
                    currentBitmap = next.bitmap
                  }
                },
                enabled = redoStack.isNotEmpty(),
                modifier = Modifier.testTag("btn_ai_redo")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Redo,
                  contentDescription = "Redo",
                  tint = if (redoStack.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f)
                )
              }

              Spacer(modifier = Modifier.width(4.dp))

              // Compare Button (Hold Before / Toggle Split)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    if (compareMode != CompareMode.NONE) Color(0xFF38BDF8).copy(alpha = 0.35f)
                    else Color.White.copy(alpha = 0.08f)
                  )
                  .pointerInput(Unit) {
                    detectTapGestures(
                      onPress = {
                        compareMode = CompareMode.HOLD_BEFORE
                        tryAwaitRelease()
                        compareMode = CompareMode.NONE
                      },
                      onDoubleTap = {
                        compareMode = if (compareMode == CompareMode.SPLIT_SLIDER) CompareMode.NONE else CompareMode.SPLIT_SLIDER
                      }
                    )
                  }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("btn_ai_compare"),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.Compare,
                    contentDescription = null,
                    tint = if (compareMode != CompareMode.NONE) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier.size(16.dp)
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

            // Export & Hand-off actions
            Row(verticalAlignment = Alignment.CenterVertically) {
              // Send to Offline Pro Editor
              IconButton(
                onClick = {
                  currentBitmap?.let { bmp ->
                    onOpenInProEditor(bmp, "${mediaTitle}_AI")
                  }
                },
                modifier = Modifier.testTag("btn_send_to_pro_editor")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = "Open in Professional Editor",
                  tint = Color(0xFFA78BFA)
                )
              }

              // Save to Gallery
              Button(
                onClick = {
                  currentBitmap?.let { bmp ->
                    onSaveExport(bmp, "${mediaTitle}_AI_${System.currentTimeMillis()}")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF2563EB),
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_ai_save_image")
              ) {
                Icon(
                  imageVector = Icons.Rounded.SaveAlt,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // 2. ONLINE / API STATUS BANNER
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
                Icon(
                  imageVector = Icons.Rounded.Key,
                  contentDescription = null,
                  tint = Color(0xFFFDBA74),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Gemini API Key Required for Online AI Edits",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              Text(
                text = "Enter Key",
                color = Color(0xFFFDBA74),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        } else {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131722))
              .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Gemini 2.5 Flash Image • Online Cloud AI",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            }

            Text(
              text = "Settings",
              color = Color(0xFF38BDF8),
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.clickable { showApiKeyDialog = true }
            )
          }
        }

        // 3. MAIN WORKSPACE CANVAS
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF090B0F)),
          contentAlignment = Alignment.Center
        ) {
          val bmpToRender = if (compareMode == CompareMode.HOLD_BEFORE) originalBitmap else currentBitmap

          if (bmpToRender != null) {
            var scale by remember { mutableFloatStateOf(1f) }
            var panOffset by remember { mutableStateOf(Offset.Zero) }

            Box(
              modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeCategory) {
                  // Only pan/zoom when not painting mask in OBJECTS mode
                  if (activeCategory != AiEditorToolCategory.OBJECTS) {
                    detectTransformGestures { _, pan, zoom, _ ->
                      scale = (scale * zoom).coerceIn(1f, 4f)
                      panOffset = if (scale > 1f) panOffset + pan else Offset.Zero
                    }
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              Image(
                bitmap = bmpToRender.asImageBitmap(),
                contentDescription = "AI Editor Canvas",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = panOffset.x,
                    translationY = panOffset.y
                  )
              )

              // Interactive Mask Painting Layer (Magic Eraser / Inpainting)
              if (activeCategory == AiEditorToolCategory.OBJECTS) {
                Canvas(
                  modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maskBrushSize) {
                      detectDragGestures(
                        onDragStart = { startOffset ->
                          currentStrokePoints = listOf(startOffset)
                        },
                        onDrag = { change, _ ->
                          change.consume()
                          currentStrokePoints = currentStrokePoints + change.position
                        },
                        onDragEnd = {
                          if (currentStrokePoints.isNotEmpty()) {
                            maskStrokes.add(
                              AiMaskStroke(
                                points = currentStrokePoints,
                                strokeWidth = maskBrushSize
                              )
                            )
                            currentStrokePoints = emptyList()
                          }
                        }
                      )
                    }
                ) {
                  // Draw completed mask strokes
                  for (stroke in maskStrokes) {
                    val pts = stroke.points
                    if (pts.size >= 2) {
                      for (i in 0 until pts.size - 1) {
                        drawLine(
                          color = Color(0xB0EF4444),
                          start = pts[i],
                          end = pts[i + 1],
                          strokeWidth = stroke.strokeWidth,
                          cap = StrokeCap.Round
                        )
                      }
                    } else if (pts.size == 1) {
                      drawCircle(
                        color = Color(0xB0EF4444),
                        radius = stroke.strokeWidth / 2,
                        center = pts[0]
                      )
                    }
                  }

                  // Draw active stroke
                  if (currentStrokePoints.size >= 2) {
                    for (i in 0 until currentStrokePoints.size - 1) {
                      drawLine(
                        color = Color(0xD0EF4444),
                        start = currentStrokePoints[i],
                        end = currentStrokePoints[i + 1],
                        strokeWidth = maskBrushSize,
                        cap = StrokeCap.Round
                      )
                    }
                  }
                }
              }

              // Before Label Indicator
              if (compareMode == CompareMode.HOLD_BEFORE) {
                Surface(
                  color = Color.Black.copy(alpha = 0.75f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                ) {
                  Text(
                    text = "ORIGINAL (BEFORE)",
                    color = Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }

          // In-canvas Mask Controls when Magic Eraser is active
          if (activeCategory == AiEditorToolCategory.OBJECTS) {
            Surface(
              color = Color(0xFF1E2330).copy(alpha = 0.9f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = {
                    if (maskStrokes.isNotEmpty()) maskStrokes.removeAt(maskStrokes.lastIndex)
                  },
                  enabled = maskStrokes.isNotEmpty(),
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo Mask Stroke",
                    tint = if (maskStrokes.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                  )
                }

                IconButton(
                  onClick = { maskStrokes.clear() },
                  enabled = maskStrokes.isNotEmpty(),
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear Mask",
                    tint = if (maskStrokes.isNotEmpty()) Color(0xFFEF4444) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                  )
                }

                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Strokes: ${maskStrokes.size}",
                  color = Color(0xFFCBD5E1),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          // Progress Overlay during generation
          if (isGenerating) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
              contentAlignment = Alignment.Center
            ) {
              Surface(
                color = Color(0xFF1E2330),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                modifier = Modifier
                  .widthIn(max = 320.dp)
                  .padding(24.dp)
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Box(
                    modifier = Modifier
                      .size(54.dp)
                      .clip(CircleShape)
                      .background(
                        Brush.sweepGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFF38BDF8)))
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    CircularProgressIndicator(
                      color = Color.White,
                      strokeWidth = 3.dp,
                      modifier = Modifier.size(38.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(16.dp))
                  Text(
                    text = "Generating with Gemini AI",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = generationStepText,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  OutlinedButton(
                    onClick = {
                      activeJob?.cancel()
                      isGenerating = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                  ) {
                    Text("Cancel", fontSize = 12.sp)
                  }
                }
              }
            }
          }

          // Floating Error Banner with Retry
          errorMessage?.let { err ->
            Surface(
              color = Color(0xFF7F1D1D),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(err, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                  onClick = { errorMessage = null },
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Text("Dismiss", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          // Floating Success Notice
          infoMessage?.let { info ->
            Surface(
              color = Color(0xFF064E3B),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(info, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                  onClick = { infoMessage = null },
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Text("OK", color = Color(0xFF6EE7B7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // 4. BOTTOM TOOLS & CONTROLS DOCKED PANEL
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color(0xFF131722),
          tonalElevation = 8.dp
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
          ) {
            // PROMPT INPUT BAR
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = {
                  Text(
                    text = when (activeCategory) {
                      AiEditorToolCategory.PROMPT -> "Describe edit (e.g. 'Add beach behind', 'Make sunset')..."
                      AiEditorToolCategory.OBJECTS -> "What to remove/fill in marked mask?..."
                      AiEditorToolCategory.BACKGROUND -> "Background details or custom style..."
                      AiEditorToolCategory.RELIGHT -> "Lighting tone or custom angle..."
                      AiEditorToolCategory.SKY -> "Custom sky description..."
                      else -> "Additional instructions for AI..."
                    },
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                  )
                },
                trailingIcon = {
                  IconButton(onClick = { launchVoiceInput() }) {
                    Icon(
                      imageVector = Icons.Rounded.Mic,
                      contentDescription = "Voice Input",
                      tint = Color(0xFF38BDF8)
                    )
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
                  .testTag("ai_editor_prompt_input")
              )

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = {
                  if (promptInput.isNotBlank()) {
                    initiateEditWithConsent(promptInput)
                  } else {
                    initiateEditWithConsent(activeCategory.description)
                  }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF38BDF8),
                  contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("btn_ai_generate")
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            // TOOL-SPECIFIC OPTIONS ROW
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
              when (activeCategory) {
                AiEditorToolCategory.PROMPT -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val quickPrompts = listOf(
                      "Background हटाकर white कर दो" to "Remove background and make it clean white",
                      "पीछे Beach लगा दो" to "Add a tropical beach with ocean sunset behind the subject",
                      "Photo को clear और sharp करो" to "Make the photo sharp, clear and enhance high-resolution details",
                      "पुराने photo को restore करो" to "Restore this vintage photograph, remove scratches and noise",
                      "Passport-photo style बनाओ" to "Format as official passport photo with studio neutral background",
                      "Lighting natural करो" to "Make the scene lighting soft, natural, and harmonized"
                    )
                    items(quickPrompts) { (label, pr) ->
                      PresetChip(
                        title = label,
                        onClick = {
                          promptInput = pr
                          initiateEditWithConsent(pr)
                        }
                      )
                    }
                  }
                }

                AiEditorToolCategory.OBJECTS -> {
                  Column {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Brush Size: ${maskBrushSize.toInt()}px",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                      )
                      Text(
                        text = if (maskStrokes.isEmpty()) "Paint on unwanted area with finger" else "${maskStrokes.size} stroke(s) painted",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                      )
                    }
                    Slider(
                      value = maskBrushSize,
                      onValueChange = { maskBrushSize = it },
                      valueRange = 15f..120f,
                      colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFEF4444),
                        activeTrackColor = Color(0xFFEF4444)
                      ),
                      modifier = Modifier.height(28.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      val eraserActions = listOf(
                        "Erase Object" to "Erase the masked object seamlessly and inpaint realistic matching background",
                        "Remove Person" to "Remove the marked person and reconstruct the natural background behind them",
                        "Remove Wires / Text" to "Erase marked cables, wires, watermarks and text cleanly",
                        "Generative Fill" to promptInput.ifBlank { "Generatively fill the masked region with realistic details matching the scene" }
                      )
                      items(eraserActions) { (label, actionPrompt) ->
                        PresetChip(
                          title = label,
                          onClick = {
                            initiateEditWithConsent(actionPrompt)
                          }
                        )
                      }
                    }
                  }
                }

                AiEditorToolCategory.BACKGROUND -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BackgroundPreset.values()) { preset ->
                      PresetChip(
                        title = preset.title,
                        onClick = {
                          promptInput = preset.prompt
                          initiateEditWithConsent(preset.prompt)
                        }
                      )
                    }
                  }
                }

                AiEditorToolCategory.ENHANCE -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EnhancePreset.values()) { preset ->
                      PresetChip(
                        title = preset.title,
                        onClick = {
                          promptInput = preset.prompt
                          initiateEditWithConsent(preset.prompt)
                        }
                      )
                    }
                  }
                }

                AiEditorToolCategory.PORTRAIT -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PortraitPreset.values()) { preset ->
                      PresetChip(
                        title = preset.title,
                        onClick = {
                          promptInput = preset.prompt
                          initiateEditWithConsent(preset.prompt)
                        }
                      )
                    }
                  }
                }

                AiEditorToolCategory.RELIGHT -> {
                  Column {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text("Light Intensity: ${(relightStrength * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Slider(
                      value = relightStrength,
                      onValueChange = { relightStrength = it },
                      valueRange = 0.2f..1.0f,
                      colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFBBF24),
                        activeTrackColor = Color(0xFFFBBF24)
                      ),
                      modifier = Modifier.height(28.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      items(RelightPreset.values()) { preset ->
                        PresetChip(
                          title = preset.title,
                          onClick = {
                            promptInput = preset.prompt
                            initiateEditWithConsent(preset.prompt)
                          }
                        )
                      }
                    }
                  }
                }

                AiEditorToolCategory.SKY -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SkyPreset.values()) { preset ->
                      PresetChip(
                        title = preset.title,
                        onClick = {
                          promptInput = preset.prompt
                          initiateEditWithConsent(preset.prompt)
                        }
                      )
                    }
                  }
                }

                AiEditorToolCategory.EXPAND -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpandDirection.values()) { preset ->
                      PresetChip(
                        title = preset.title,
                        onClick = {
                          promptInput = preset.prompt
                          initiateEditWithConsent(preset.prompt)
                        }
                      )
                    }
                  }
                }

                AiEditorToolCategory.STYLES -> {
                  Column {
                    Text(
                      text = "Notice: Art style transforms the aesthetic into painterly or analog film mediums.",
                      color = Color(0xFFFBBF24),
                      fontSize = 10.sp,
                      modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      items(StylePreset.values()) { preset ->
                        PresetChip(
                          title = preset.title,
                          onClick = {
                            promptInput = preset.prompt
                            initiateEditWithConsent(preset.prompt)
                          }
                        )
                      }
                    }
                  }
                }

                AiEditorToolCategory.RESTORE -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RestorePreset.values()) { preset ->
                      PresetChip(
                        title = preset.title,
                        onClick = {
                          promptInput = preset.prompt
                          initiateEditWithConsent(preset.prompt)
                        }
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // CATEGORY NAVIGATION TABS (Horizontal Scroll)
            LazyRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              items(AiEditorToolCategory.values()) { cat ->
                val isSelected = cat == activeCategory
                val icon = when (cat) {
                  AiEditorToolCategory.PROMPT -> Icons.Rounded.AutoAwesome
                  AiEditorToolCategory.OBJECTS -> Icons.Rounded.AutoFixHigh
                  AiEditorToolCategory.BACKGROUND -> Icons.Rounded.Wallpaper
                  AiEditorToolCategory.ENHANCE -> Icons.Rounded.Tune
                  AiEditorToolCategory.PORTRAIT -> Icons.Rounded.Face
                  AiEditorToolCategory.RELIGHT -> Icons.Rounded.LightMode
                  AiEditorToolCategory.SKY -> Icons.Rounded.CloudQueue
                  AiEditorToolCategory.EXPAND -> Icons.Rounded.AspectRatio
                  AiEditorToolCategory.STYLES -> Icons.Rounded.Palette
                  AiEditorToolCategory.RESTORE -> Icons.Rounded.HistoryEdu
                }

                Surface(
                  color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.Transparent,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { activeCategory = cat }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("ai_cat_${cat.name.lowercase()}")
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 60.dp)
                  ) {
                    Icon(
                      imageVector = icon,
                      contentDescription = cat.displayName,
                      tint = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.6f),
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = cat.displayName,
                      color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                      fontSize = 10.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 5. CLOUD PRIVACY CONSENT DIALOG (Prompt 8 Mandate)
    if (showConsentDialog) {
      AlertDialog(
        onDismissRequest = {
          showConsentDialog = false
          pendingExecutionAction = null
        },
        icon = {
          Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(32.dp)
          )
        },
        title = {
          Text(
            text = "Cloud AI Photo Permission",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        text = {
          Column {
            Text(
              text = "This feature sends the selected image and your editing instruction securely to the online Google Gemini AI service.\n\nYour photo is processed exclusively to generate this edit and is never shared or stored permanently.",
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
              Text(
                text = "Remember my choice (Don't ask again)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
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
              pendingExecutionAction?.invoke()
              pendingExecutionAction = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
          ) {
            Text("Consent & Proceed")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showConsentDialog = false
              pendingExecutionAction = null
            }
          ) {
            Text("Cancel")
          }
        }
      )
    }

    // 6. API KEY CONFIGURATION DIALOG
    if (showApiKeyDialog) {
      AlertDialog(
        onDismissRequest = { showApiKeyDialog = false },
        icon = {
          Icon(Icons.Rounded.Key, contentDescription = null, tint = Color(0xFF38BDF8))
        },
        title = {
          Text("Gemini API Key Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
          Column {
            Text(
              text = "Online AI editing requires a Google Gemini API key. You can create a free key in Google AI Studio or configure GEMINI_API_KEY in the Secrets panel.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = enteredApiKey,
              onValueChange = { enteredApiKey = it },
              label = { Text("Paste API Key") },
              placeholder = { Text("AIzaSy...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              onSaveApiKey(enteredApiKey.trim())
              showApiKeyDialog = false
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

    // 7. UNSAVED CHANGES WARNING DIALOG
    if (showUnsavedDialog) {
      AlertDialog(
        onDismissRequest = { showUnsavedDialog = false },
        title = { Text("Discard AI Edits?") },
        text = { Text("You have generated edits that have not been saved to your gallery. Would you like to save or discard?") },
        confirmButton = {
          Button(
            onClick = {
              showUnsavedDialog = false
              currentBitmap?.let { bmp ->
                onSaveExport(bmp, "${mediaTitle}_AI")
              }
              onClose()
            }
          ) {
            Text("Save & Exit")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showUnsavedDialog = false
              onClose()
            }
          ) {
            Text("Discard")
          }
        }
      )
    }
  }
}

@Composable
private fun PresetChip(
  title: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(Color(0xFF1E2330))
      .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = title,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
