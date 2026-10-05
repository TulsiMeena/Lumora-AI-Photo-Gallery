package com.example.data.ai.understanding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import com.example.BuildConfig
import com.example.model.ai.understanding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Clean Provider Abstraction for AI Image Understanding
 */
interface AIImageUnderstandingProvider {
  suspend fun processImageUnderstanding(
    context: Context,
    bitmap: Bitmap,
    task: ImageUnderstandingTask,
    userPrompt: String,
    cropRegion: ImageCropRegion? = null,
    historyMessages: List<Pair<String, Boolean>> = emptyList(), // Pair(text, isUser)
    targetLanguage: TranslationLanguage = TranslationLanguage.HINDI,
    mathLevel: MathExplanationLevel = MathExplanationLevel.STEP_BY_STEP,
    specificQuestionNumber: String? = null,
    userApiKey: String? = null
  ): ImageUnderstandingResult
}

/**
 * Gemini-based Implementation using direct REST API and gemini-2.5-flash
 */
object GeminiImageUnderstandingProvider : AIImageUnderstandingProvider {

  private const val MODEL_NAME = "gemini-2.5-flash"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  fun getEffectiveApiKey(userKey: String?): String {
    val trimmedUser = userKey?.trim() ?: ""
    if (trimmedUser.isNotBlank() && trimmedUser != "DEFAULT_PLACEHOLDER") return trimmedUser
    return try {
      val bcKey = BuildConfig.GEMINI_API_KEY.trim()
      if (bcKey == "DEFAULT_PLACEHOLDER") "" else bcKey
    } catch (e: Throwable) {
      ""
    }
  }

  fun hasApiKey(userKey: String?): Boolean {
    return getEffectiveApiKey(userKey).isNotBlank()
  }

  override suspend fun processImageUnderstanding(
    context: Context,
    bitmap: Bitmap,
    task: ImageUnderstandingTask,
    userPrompt: String,
    cropRegion: ImageCropRegion?,
    historyMessages: List<Pair<String, Boolean>>,
    targetLanguage: TranslationLanguage,
    mathLevel: MathExplanationLevel,
    specificQuestionNumber: String?,
    userApiKey: String?
  ): ImageUnderstandingResult = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey(userApiKey)
    if (apiKey.isBlank()) {
      return@withContext ImageUnderstandingResult(
        isSuccess = false,
        task = task,
        errorMessage = "Gemini API Key is missing. Please configure your key in Settings or AI Studio Secrets.",
        isMissingKey = true
      )
    }

    try {
      // 1. Prepare and crop image if region specified
      val preparedBitmap = prepareBitmap(bitmap, cropRegion)
      val base64Image = bitmapToBase64(preparedBitmap)

      // 2. Build structured system directive and user prompt
      val structuredPrompt = buildTaskPrompt(
        task = task,
        userPrompt = userPrompt,
        targetLanguage = targetLanguage,
        mathLevel = mathLevel,
        specificQuestionNumber = specificQuestionNumber,
        hasCrop = cropRegion != null && !cropRegion.isFullImage
      )

      // 3. Assemble JSON Payload with multi-turn conversation support
      val contentsArray = JSONArray()

      // Prior history turns
      for ((msgText, isUser) in historyMessages) {
        val role = if (isUser) "user" else "model"
        val turnObj = JSONObject().apply {
          put("role", role)
          put("parts", JSONArray().apply {
            put(JSONObject().apply { put("text", msgText) })
          })
        }
        contentsArray.put(turnObj)
      }

      // Active prompt turn with image and instructions
      val currentTurnParts = JSONArray().apply {
        put(JSONObject().apply {
          put("text", structuredPrompt)
        })
        put(JSONObject().apply {
          put("inlineData", JSONObject().apply {
            put("mimeType", "image/jpeg")
            put("data", base64Image)
          })
        })
      }

      val currentTurnObj = JSONObject().apply {
        put("role", "user")
        put("parts", currentTurnParts)
      }
      contentsArray.put(currentTurnObj)

      val generationConfig = JSONObject().apply {
        put("temperature", if (task == ImageUnderstandingTask.SOLVE_MATH || task == ImageUnderstandingTask.EXTRACT_TEXT || task == ImageUnderstandingTask.READ_TABLE) 0.1 else 0.4)
      }

      val requestJson = JSONObject().apply {
        put("contents", contentsArray)
        put("generationConfig", generationConfig)
      }

      val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
      val endpointUrl = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

      val request = Request.Builder()
        .url(endpointUrl)
        .post(requestBody)
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseCode = response.code
      val responseBodyString = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val parsedError = parseApiError(responseCode, responseBodyString)
        return@withContext ImageUnderstandingResult(
          isSuccess = false,
          task = task,
          errorMessage = parsedError.message,
          isQuotaExceeded = parsedError.isQuota,
          isMissingKey = parsedError.isAuth
        )
      }

      // 4. Parse Gemini Response Text
      val responseObj = JSONObject(responseBodyString)
      val candidates = responseObj.optJSONArray("candidates")
      if (candidates == null || candidates.length() == 0) {
        return@withContext ImageUnderstandingResult(
          isSuccess = false,
          task = task,
          errorMessage = "Gemini returned an empty response or the image was flagged by safety policies."
        )
      }

      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content")
      val parts = content?.optJSONArray("parts")

      var responseText = ""
      if (parts != null) {
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
          val part = parts.getJSONObject(i)
          if (part.has("text")) {
            sb.append(part.getString("text"))
          }
        }
        responseText = sb.toString().trim()
      }

      if (responseText.isBlank()) {
        return@withContext ImageUnderstandingResult(
          isSuccess = false,
          task = task,
          errorMessage = "Could not extract meaning from the image. Please try a clearer photo or specify your question."
        )
      }

      // 5. Structure results according to task
      parseStructuredResult(task, responseText, targetLanguage)

    } catch (e: java.net.UnknownHostException) {
      ImageUnderstandingResult(
        isSuccess = false,
        task = task,
        errorMessage = "Offline — AI service unavailable. Please check your internet connection.",
        isNetworkError = true
      )
    } catch (e: java.net.SocketTimeoutException) {
      ImageUnderstandingResult(
        isSuccess = false,
        task = task,
        errorMessage = "Request timed out after 60 seconds. Image analysis can be computationally intensive. Please retry.",
        isNetworkError = true
      )
    } catch (e: Exception) {
      ImageUnderstandingResult(
        isSuccess = false,
        task = task,
        errorMessage = "AI analysis error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
      )
    }
  }

  /**
   * Constructs prompt tailored to the requested capability
   */
  private fun buildTaskPrompt(
    task: ImageUnderstandingTask,
    userPrompt: String,
    targetLanguage: TranslationLanguage,
    mathLevel: MathExplanationLevel,
    specificQuestionNumber: String?,
    hasCrop: Boolean
  ): String {
    val baseConstraint = """
      You are an expert AI multimodal vision assistant.
      Analyze the attached image accurately.
      STRICT RULES:
      - Answer based ONLY on visible or reasonably inferable information.
      - DO NOT invent, hallucinate, or assume facts not visible.
      - If the image is blurry, cut off, or unclear, state clearly: "Image साफ नहीं है। कृपया crop करके clearer image भेजें।" or describe what cannot be determined.
      - Support Hindi, Hinglish, and English naturally.
    """.trimIndent()

    val cropNote = if (hasCrop) {
      "\nNOTE: The user has cropped/selected a specific region of the image. Focus your primary analysis specifically on that region."
    } else ""

    val taskSpecific = when (task) {
      ImageUnderstandingTask.EXTRACT_TEXT -> """
        TASK: IMAGE OCR & TEXT EXTRACTION
        - Detect all printed text, numbers, headings, paragraphs, and lists.
        - Detect handwritten text if readable.
        - Preserve the natural line breaks and tabular structure.
        - Do not add commentary or conversational filler. Return the extracted text cleanly.
      """.trimIndent()

      ImageUnderstandingTask.TRANSLATE -> """
        TASK: IMAGE TRANSLATION
        - Auto-detect the original language of the text in the image.
        - Translate all detected text accurately into ${targetLanguage.displayName} (${targetLanguage.nativeName}).
        - Format the response as:
          [DETECTED_LANGUAGE: <language name>]
          [TRANSLATED_TEXT]
          <translated text here>
      """.trimIndent()

      ImageUnderstandingTask.SOLVE_MATH -> """
        TASK: AI MATHEMATICAL PROBLEM SOLVER
        Explanation Level: ${mathLevel.displayName} (${mathLevel.promptDirective})
        ${if (specificQuestionNumber != null) "Focus ONLY on question: $specificQuestionNumber" else ""}
        - Identify the mathematical question(s).
        - Accurately preserve all mathematical symbols, fractions, powers, and equations.
        - For each problem, format your response in this exact structured pattern:
          === PROBLEM START ===
          QUESTION_NUMBER: Q1 (or specific number)
          QUESTION: <exact question text>
          METHOD: <mathematical formula or method used>
          STEPS:
          - Step 1: ...
          - Step 2: ...
          FINAL_ANSWER: <exact final numerical or algebraic answer>
          === PROBLEM END ===
      """.trimIndent()

      ImageUnderstandingTask.SUMMARIZE_DOC -> """
        TASK: DOCUMENT UNDERSTANDING & EXTRACTION
        - Provide an executive summary of this document.
        - Extract key points.
        - Identify any dates, amounts/currency, names, IDs (Aadhaar, PAN, Passport, Invoice #), phone numbers, and addresses.
        - Format cleanly with bold headings.
      """.trimIndent()

      ImageUnderstandingTask.READ_TABLE -> """
        TASK: IMAGE TO STRUCTURED TABLE
        - Detect rows, columns, headers, numbers, and text.
        - Output the table strictly as a Markdown table (e.g. | Col 1 | Col 2 | ...).
        - If any cell text is blurry or uncertain, append [?] to that cell value so the user knows to verify it.
        - Do not change or invent numerical values.
      """.trimIndent()

      ImageUnderstandingTask.ANALYZE_CHART -> """
        TASK: CHART & GRAPH UNDERSTANDING
        - Identify the chart type (Bar chart, Line graph, Pie chart, etc.).
        - Read the axis labels, categories, and numeric data points accurately without fabricating values.
        - Identify minimum, maximum, and significant trends.
        - Provide a concise summary and answer the user question.
      """.trimIndent()

      ImageUnderstandingTask.IDENTIFY_OBJECTS -> """
        TASK: OBJECT IDENTIFICATION & SPATIAL MAPPING
        - List the prominent objects visible.
        - Provide their approximate spatial position in the frame: (Top-Left, Top-Center, Top-Right, Center-Left, Center, Center-Right, Bottom-Left, Bottom-Center, Bottom-Right).
        - Count instances where applicable (e.g. "3 cars").
      """.trimIndent()

      ImageUnderstandingTask.DESCRIBE_IMAGE -> """
        TASK: COMPREHENSIVE IMAGE DESCRIPTION
        - Short Overview: 1-2 sentences.
        - Detailed Scene & Setting: environment, lighting, mood.
        - Prominent Objects and Layout: visual placement.
        - Colors & Aesthetics: dominant color palette.
      """.trimIndent()

      ImageUnderstandingTask.ASK_ANYTHING, ImageUnderstandingTask.EXPLAIN_IMAGE, ImageUnderstandingTask.CUSTOM_QUESTION -> """
        TASK: IMAGE INQUIRY & INTERACTION
        - Answer the user's specific query about this image thoroughly and clearly.
        - If asked in Hindi or Hinglish, respond in natural, polite Hindi or Hinglish.
      """.trimIndent()
    }

    val userQuery = if (userPrompt.isNotBlank()) {
      "\nUSER QUESTION / INSTRUCTION: $userPrompt"
    } else {
      "\nUSER INSTRUCTION: ${task.defaultPromptEnglish}"
    }

    return "$baseConstraint\n$cropNote\n$taskSpecific\n$userQuery"
  }

  /**
   * Parses text response into structured objects (tables, math steps, etc.)
   */
  private fun parseStructuredResult(
    task: ImageUnderstandingTask,
    text: String,
    targetLanguage: TranslationLanguage
  ): ImageUnderstandingResult {
    when (task) {
      ImageUnderstandingTask.READ_TABLE -> {
        val tableData = parseMarkdownTable(text)
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = text,
          tableData = tableData
        )
      }

      ImageUnderstandingTask.SOLVE_MATH -> {
        val mathResults = parseMathProblems(text)
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = text,
          mathResults = mathResults
        )
      }

      ImageUnderstandingTask.EXTRACT_TEXT -> {
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = text,
          extractedText = text
        )
      }

      ImageUnderstandingTask.TRANSLATE -> {
        var detectedLang: String? = null
        var translated = text
        val langRegex = Regex("""\[DETECTED_LANGUAGE:\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
        val match = langRegex.find(text)
        if (match != null) {
          detectedLang = match.groupValues[1].trim()
          translated = text.replace(match.value, "").replace("[TRANSLATED_TEXT]", "").trim()
        }
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = translated,
          detectedLanguage = detectedLang ?: "Auto-detected",
          translatedText = translated
        )
      }

      ImageUnderstandingTask.SUMMARIZE_DOC -> {
        val docDetails = parseDocumentDetails(text)
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = text,
          documentDetails = docDetails
        )
      }

      ImageUnderstandingTask.IDENTIFY_OBJECTS -> {
        val objects = parseObjectsList(text)
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = text,
          detectedObjects = objects
        )
      }

      else -> {
        return ImageUnderstandingResult(
          isSuccess = true,
          task = task,
          mainTextResponse = text
        )
      }
    }
  }

  private fun parseMarkdownTable(text: String): ExtractedTableData? {
    val lines = text.lines().map { it.trim() }.filter { it.startsWith("|") && it.endsWith("|") }
    if (lines.size < 2) return null

    val headerLine = lines[0]
    val headers = headerLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }

    val rows = mutableListOf<List<String>>()
    val uncertainCells = mutableSetOf<Pair<Int, Int>>()

    var rowIdx = 0
    for (i in 1 until lines.size) {
      val line = lines[i]
      if (line.contains("---") || line.contains("===")) continue // separator row
      val rawCells = line.split("|").map { it.trim() }
      val cells = if (rawCells.size >= 2) rawCells.subList(1, rawCells.size - 1) else rawCells

      val cleanedRow = cells.mapIndexed { colIdx, cell ->
        if (cell.contains("[?]")) {
          uncertainCells.add(Pair(rowIdx, colIdx))
          cell.replace("[?]", "").trim()
        } else {
          cell
        }
      }
      if (cleanedRow.isNotEmpty()) {
        rows.add(cleanedRow)
        rowIdx++
      }
    }

    return ExtractedTableData(
      headers = headers,
      rows = rows,
      uncertainCells = uncertainCells
    )
  }

  private fun parseMathProblems(text: String): List<MathProblemResult> {
    val results = mutableListOf<MathProblemResult>()
    val problemBlocks = text.split("=== PROBLEM START ===").filter { it.isNotBlank() }

    if (problemBlocks.isNotEmpty()) {
      for (block in problemBlocks) {
        val cleanBlock = block.replace("=== PROBLEM END ===", "").trim()
        var qNum = "Q1"
        var qText = ""
        var method = ""
        val steps = mutableListOf<String>()
        var finalAns = ""

        val lines = cleanBlock.lines()
        var inSteps = false
        for (line in lines) {
          val trimmed = line.trim()
          when {
            trimmed.startsWith("QUESTION_NUMBER:", ignoreCase = true) -> {
              qNum = trimmed.substringAfter(":").trim()
              inSteps = false
            }
            trimmed.startsWith("QUESTION:", ignoreCase = true) -> {
              qText = trimmed.substringAfter(":").trim()
              inSteps = false
            }
            trimmed.startsWith("METHOD:", ignoreCase = true) -> {
              method = trimmed.substringAfter(":").trim()
              inSteps = false
            }
            trimmed.startsWith("STEPS:", ignoreCase = true) -> {
              inSteps = true
            }
            trimmed.startsWith("FINAL_ANSWER:", ignoreCase = true) -> {
              finalAns = trimmed.substringAfter(":").trim()
              inSteps = false
            }
            inSteps && (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches(Regex("""^\d+\..*"""))) -> {
              steps.add(trimmed)
            }
          }
        }

        if (finalAns.isNotBlank() || qText.isNotBlank()) {
          val initialResult = MathProblemResult(
            questionNumber = qNum,
            questionText = qText,
            method = method,
            steps = steps,
            finalAnswer = finalAns
          )
          // Run through LocalCalculatorVerifier
          val verified = LocalCalculatorVerifier.verifyMathResult(initialResult)
          results.add(verified)
        }
      }
    }

    // Fallback if formatting was non-standard
    if (results.isEmpty()) {
      val fallback = MathProblemResult(
        questionNumber = "Math Problem",
        questionText = "Problem from image",
        method = "Step-by-step mathematical reasoning",
        steps = text.lines().filter { it.trim().startsWith("-") || it.trim().startsWith("*") },
        finalAnswer = text.lines().lastOrNull { it.contains("answer", ignoreCase = true) || it.contains("उत्तर", ignoreCase = true) } ?: "See full steps above."
      )
      results.add(LocalCalculatorVerifier.verifyMathResult(fallback))
    }

    return results
  }

  private fun parseDocumentDetails(text: String): DocumentDetails {
    val lines = text.lines().map { it.trim() }
    val dates = mutableListOf<String>()
    val amounts = mutableListOf<String>()
    val names = mutableListOf<String>()
    val ids = mutableListOf<String>()

    val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\w+ \d{1,2},? \d{4})\b""")
    val amountRegex = Regex("""(?:\$|₹|INR|USD|EUR)\s*[\d,]+(?:\.\d{2})?""")

    for (line in lines) {
      dateRegex.findAll(line).forEach { dates.add(it.value) }
      amountRegex.findAll(line).forEach { amounts.add(it.value) }
    }

    return DocumentDetails(
      summary = text.take(300),
      dates = dates.distinct(),
      amounts = amounts.distinct(),
      names = names.distinct(),
      ids = ids.distinct()
    )
  }

  private fun parseObjectsList(text: String): List<DetectedObjectItem> {
    val list = mutableListOf<DetectedObjectItem>()
    val lines = text.lines().map { it.trim() }.filter { it.startsWith("-") || it.startsWith("*") || it.matches(Regex("""^\d+\..*""")) }

    for (line in lines) {
      val clean = line.replace(Regex("""^[-*\d.]+\s*"""), "").trim()
      val spatial = when {
        clean.contains("top-left", ignoreCase = true) || clean.contains("ऊपर बाईं", ignoreCase = true) -> "Top-Left"
        clean.contains("top-right", ignoreCase = true) || clean.contains("ऊपर दाईं", ignoreCase = true) -> "Top-Right"
        clean.contains("top", ignoreCase = true) || clean.contains("ऊपर", ignoreCase = true) -> "Top"
        clean.contains("bottom-left", ignoreCase = true) || clean.contains("नीचे बाईं", ignoreCase = true) -> "Bottom-Left"
        clean.contains("bottom-right", ignoreCase = true) || clean.contains("नीचे दाईं", ignoreCase = true) -> "Bottom-Right"
        clean.contains("bottom", ignoreCase = true) || clean.contains("नीचे", ignoreCase = true) -> "Bottom"
        clean.contains("left", ignoreCase = true) || clean.contains("बाईं", ignoreCase = true) -> "Left"
        clean.contains("right", ignoreCase = true) || clean.contains("दाईं", ignoreCase = true) -> "Right"
        else -> "Center"
      }
      list.add(DetectedObjectItem(name = clean, spatialPosition = spatial))
    }
    return list
  }

  private fun prepareBitmap(src: Bitmap, crop: ImageCropRegion?): Bitmap {
    var working = src

    // Apply rotation if needed
    if (crop != null && crop.rotationDegrees != 0) {
      val matrix = Matrix().apply { postRotate(crop.rotationDegrees.toFloat()) }
      working = Bitmap.createBitmap(working, 0, 0, working.width, working.height, matrix, true)
    }

    // Apply crop if needed
    if (crop != null && !crop.isFullImage) {
      val w = working.width
      val h = working.height
      val x = (crop.left * w).toInt().coerceIn(0, w - 1)
      val y = (crop.top * h).toInt().coerceIn(0, h - 1)
      val cropW = ((crop.right - crop.left) * w).toInt().coerceIn(1, w - x)
      val cropH = ((crop.bottom - crop.top) * h).toInt().coerceIn(1, h - y)
      working = Bitmap.createBitmap(working, x, y, cropW, cropH)
    }

    // Scale to max dimension 1536
    val maxDim = 1536
    if (working.width > maxDim || working.height > maxDim) {
      val ratio = if (working.width > working.height) {
        maxDim.toFloat() / working.width
      } else {
        maxDim.toFloat() / working.height
      }
      val targetW = (working.width * ratio).toInt().coerceAtLeast(1)
      val targetH = (working.height * ratio).toInt().coerceAtLeast(1)
      working = Bitmap.createScaledBitmap(working, targetW, targetH, true)
    }

    return working
  }

  private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
  }

  private data class ParsedError(val message: String, val isQuota: Boolean = false, val isAuth: Boolean = false)

  private fun parseApiError(code: Int, body: String): ParsedError {
    return try {
      val obj = JSONObject(body)
      val err = obj.optJSONObject("error")
      val msg = err?.optString("message") ?: "HTTP $code"
      when {
        code == 429 || msg.contains("quota", ignoreCase = true) || msg.contains("rate limit", ignoreCase = true) ->
          ParsedError("API Quota exceeded: Rate limit reached for Gemini image understanding. Please try again in a few moments.", isQuota = true)
        code == 401 || code == 403 || msg.contains("API key", ignoreCase = true) ->
          ParsedError("Invalid or restricted Gemini API key. Please check your key in Settings or AI Studio Secrets.", isAuth = true)
        code == 400 && msg.contains("safety", ignoreCase = true) ->
          ParsedError("Request was blocked by Gemini safety policy filters. Please adjust your query.", isQuota = false)
        else ->
          ParsedError("Gemini API Error ($code): $msg")
      }
    } catch (e: Exception) {
      ParsedError("Gemini server returned error ($code): $body")
    }
  }
}
