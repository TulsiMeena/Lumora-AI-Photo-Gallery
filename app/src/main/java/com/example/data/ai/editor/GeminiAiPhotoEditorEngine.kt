package com.example.data.ai.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.model.ai.AiEditorResult
import com.example.model.ai.AiEditorToolCategory
import com.example.model.ai.AiMaskStroke
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

object GeminiAiPhotoEditorEngine {

  private const val MODEL_NAME = "gemini-2.5-flash-image"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

  // Mandated 60-second timeouts for AI model requests
  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  /**
   * Resolves effective Gemini API Key from user settings or BuildConfig
   */
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

  /**
   * Translates or enriches user natural language instructions (Hindi/Hinglish/English)
   * into structured, high-fidelity prompt guidelines for Gemini image editing.
   */
  fun buildStructuredPrompt(
    category: AiEditorToolCategory,
    userPrompt: String,
    hasMask: Boolean = false,
    relightStrength: Float = 0.8f
  ): String {
    val trimmed = userPrompt.trim()
    val lower = trimmed.lowercase(Locale.getDefault())

    // Convert Hindi / Hinglish phrases to precise descriptive instructions
    val translatedGoal = when {
      lower.contains("background हटाकर") || lower.contains("background remove") || lower.contains("हटा दो") && lower.contains("background") ->
        "Remove the entire background cleanly while preserving fine foreground hair and edge details."
      lower.contains("पीछे beach") || lower.contains("beach लगा दो") ->
        "Replace the background behind the subject with a beautiful tropical beach with ocean waves and sunset glow."
      lower.contains("unwanted object") || lower.contains("हटा दो") || lower.contains("object हटाओ") ->
        "Remove the unwanted object or distraction completely and seamlessly fill in the background texture naturally."
      lower.contains("sunset बना दो") || lower.contains("आसमान को sunset") ->
        "Transform the sky into a rich, vibrant golden-hour sunset and harmoniously warm the scene's ambient lighting."
      lower.contains("clear और sharp") || lower.contains("clear करो") || lower.contains("sharp कर दो") ->
        "Significantly improve clarity, sharpness, and fine textural definition across the entire image."
      lower.contains("पुराने photo") || lower.contains("restore कर दो") || lower.contains("restore photo") ->
        "Restore this old photograph by removing scratches, reducing paper grain, repairing faded colors, and enhancing facial sharpness."
      lower.contains("passport") || lower.contains("पासपोर्ट") ->
        "Format this portrait in official passport photo style with balanced frontal studio lighting and a clean neutral background."
      lower.contains("expand") || lower.contains("जगह बढ़ाओ") || lower.contains("sides में") ->
        "Generatively outpaint and expand the scene outward, extending the natural surrounding environment seamlessly."
      lower.contains("lighting natural") || lower.contains("लाइटिंग") ->
        "Harmonize lighting to look soft, natural, and professionally balanced with realistic shadows."
      trimmed.isNotBlank() -> trimmed
      else -> "Enhance and refine the image with professional photographic quality."
    }

    val maskInstruction = if (hasMask) {
      "NOTE: The attached image contains red semi-transparent overlay brushstrokes marking the exact region to be edited or erased. Seamlessly inpaint and replace ONLY the marked area, matching the surrounding surface texture, lighting, depth of field, and grain. Keep all unmarked regions completely unchanged."
    } else {
      ""
    }

    return buildString {
      append("Act as a world-class professional photo editor and digital image retoucher. ")
      append("Task category: ${category.displayName}. ")
      append("Editing instruction: $translatedGoal ")
      if (relightStrength in 0.1f..0.99f && category == AiEditorToolCategory.RELIGHT) {
        append("Apply lighting adjustment with a natural subtle strength factor of ${(relightStrength * 100).toInt()}%. ")
      }
      if (maskInstruction.isNotEmpty()) {
        append("\n$maskInstruction ")
      }
      append("\nPreserve overall photorealism, authentic facial geometry, and high-frequency textures. Output the edited photo in high resolution.")
    }
  }

  /**
   * Executes the image editing operation using Gemini API
   */
  suspend fun generateEdit(
    context: Context,
    baseBitmap: Bitmap,
    category: AiEditorToolCategory,
    userPrompt: String,
    maskStrokes: List<AiMaskStroke> = emptyList(),
    userApiKey: String? = null,
    relightStrength: Float = 0.8f
  ): AiEditorResult = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey(userApiKey)

    if (apiKey.isBlank()) {
      return@withContext AiEditorResult(
        isSuccess = false,
        errorMessage = "Gemini API Key is missing. Please enter your Gemini API Key in Settings or configure GEMINI_API_KEY in the Secrets panel.",
        isMissingKey = true
      )
    }

    try {
      // 1. Prepare image payload (composite mask if present)
      val processedBitmap = if (maskStrokes.isNotEmpty()) {
        createMaskedComposite(baseBitmap, maskStrokes)
      } else {
        scaleBitmapForApi(baseBitmap, maxDimension = 1536)
      }

      val base64Image = bitmapToBase64(processedBitmap)

      // 2. Build structured prompt
      val structuredPrompt = buildStructuredPrompt(
        category = category,
        userPrompt = userPrompt,
        hasMask = maskStrokes.isNotEmpty(),
        relightStrength = relightStrength
      )

      // 3. Construct Gemini REST JSON Payload
      val partsArray = JSONArray().apply {
        // Text part
        put(JSONObject().apply {
          put("text", structuredPrompt)
        })
        // Image part
        put(JSONObject().apply {
          put("inlineData", JSONObject().apply {
            put("mimeType", "image/jpeg")
            put("data", base64Image)
          })
        })
      }

      val contentsArray = JSONArray().apply {
        put(JSONObject().apply {
          put("parts", partsArray)
        })
      }

      val generationConfig = JSONObject().apply {
        put("responseModalities", JSONArray().apply {
          put("TEXT")
          put("IMAGE")
        })
        put("temperature", 0.4)
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

      // 4. Execute HTTP Call
      val response = okHttpClient.newCall(request).execute()
      val responseCode = response.code
      val responseBodyString = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val parsedError = parseApiError(responseCode, responseBodyString)
        return@withContext AiEditorResult(
          isSuccess = false,
          errorMessage = parsedError.message,
          isQuotaExceeded = parsedError.isQuota,
          isMissingKey = parsedError.isAuth
        )
      }

      // 5. Parse Success Response
      val responseObj = JSONObject(responseBodyString)
      val candidates = responseObj.optJSONArray("candidates")
      if (candidates == null || candidates.length() == 0) {
        return@withContext AiEditorResult(
          isSuccess = false,
          errorMessage = "Gemini returned an empty response. The request may have been flagged by safety guidelines."
        )
      }

      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content")
      val parts = content?.optJSONArray("parts")

      var resultBitmap: Bitmap? = null
      var commentaryText: String? = null

      if (parts != null) {
        for (i in 0 until parts.length()) {
          val part = parts.getJSONObject(i)
          if (part.has("inlineData")) {
            val inlineData = part.getJSONObject("inlineData")
            val base64Data = inlineData.optString("data")
            if (base64Data.isNotBlank()) {
              val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
              resultBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            }
          } else if (part.has("text")) {
            val txt = part.optString("text")
            if (txt.isNotBlank()) {
              commentaryText = txt
            }
          }
        }
      }

      if (resultBitmap != null) {
        AiEditorResult(
          isSuccess = true,
          resultBitmap = resultBitmap,
          message = commentaryText ?: "AI edit applied successfully with Gemini."
        )
      } else {
        AiEditorResult(
          isSuccess = false,
          errorMessage = commentaryText?.ifBlank { null }
            ?: "The AI model returned text analysis instead of an edited image. Please refine your prompt or select a specific tool preset."
        )
      }

    } catch (e: java.net.UnknownHostException) {
      AiEditorResult(
        isSuccess = false,
        errorMessage = "No internet connection. Online Gemini AI editing requires an active network connection.",
        isNetworkError = true
      )
    } catch (e: java.net.SocketTimeoutException) {
      AiEditorResult(
        isSuccess = false,
        errorMessage = "Request timed out after 60 seconds. High-resolution image generation can be intensive. Please check connection and retry.",
        isNetworkError = true
      )
    } catch (e: Exception) {
      AiEditorResult(
        isSuccess = false,
        errorMessage = "AI processing error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
      )
    }
  }

  /**
   * Composites mask brushstrokes as a semi-transparent red overlay onto the bitmap
   */
  private fun createMaskedComposite(base: Bitmap, strokes: List<AiMaskStroke>): Bitmap {
    val scaled = scaleBitmapForApi(base, 1536)
    val mutableBitmap = scaled.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    val paint = Paint().apply {
      color = Color.argb(180, 239, 68, 68) // Bright red highlight
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      isAntiAlias = true
    }

    val width = mutableBitmap.width.toFloat()
    val height = mutableBitmap.height.toFloat()

    for (stroke in strokes) {
      paint.strokeWidth = stroke.strokeWidth * (width / 1000f).coerceAtLeast(1f)
      val pts = stroke.points
      if (pts.size >= 2) {
        for (i in 0 until pts.size - 1) {
          val p1 = pts[i]
          val p2 = pts[i + 1]
          canvas.drawLine(p1.x * width, p1.y * height, p2.x * width, p2.y * height, paint)
        }
      } else if (pts.size == 1) {
        val p = pts[0]
        canvas.drawCircle(p.x * width, p.y * height, paint.strokeWidth / 2, paint)
      }
    }

    return mutableBitmap
  }

  private fun scaleBitmapForApi(src: Bitmap, maxDimension: Int): Bitmap {
    if (src.width <= maxDimension && src.height <= maxDimension) return src
    val ratio = if (src.width > src.height) {
      maxDimension.toFloat() / src.width
    } else {
      maxDimension.toFloat() / src.height
    }
    val targetW = (src.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (src.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, targetW, targetH, true)
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
          ParsedError("API Quota exceeded: You have reached the rate limit for Gemini image editing. Please wait a moment or try again later.", isQuota = true)
        code == 401 || code == 403 || msg.contains("API key", ignoreCase = true) ->
          ParsedError("Invalid or restricted Gemini API key. Please check your key in Settings or AI Studio Secrets.", isAuth = true)
        code == 400 && msg.contains("safety", ignoreCase = true) ->
          ParsedError("Request was blocked by Gemini safety policy filters. Please adjust the prompt.", isQuota = false)
        else ->
          ParsedError("Gemini API Error ($code): $msg")
      }
    } catch (e: Exception) {
      ParsedError("Gemini server returned error ($code): $body")
    }
  }
}
