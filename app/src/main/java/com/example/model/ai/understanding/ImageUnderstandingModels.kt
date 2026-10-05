package com.example.model.ai.understanding

import android.graphics.RectF
import java.util.UUID

/**
 * Main Image Understanding Actions for Prompt 9
 */
enum class ImageUnderstandingTask(
  val displayName: String,
  val shortLabel: String,
  val iconName: String,
  val description: String,
  val defaultPromptHindi: String,
  val defaultPromptEnglish: String
) {
  ASK_ANYTHING(
    "Ask About Image",
    "Ask",
    "Chat",
    "Ask any questions about this image in Hindi or English",
    "इस image में क्या है? विस्तार से समझाओ।",
    "What is in this image? Explain in detail."
  ),
  EXTRACT_TEXT(
    "Extract Text",
    "OCR",
    "DocumentScanner",
    "Extract printed and handwritten text preserving line structure",
    "इस image का सारा text extract करो।",
    "Extract all printed and handwritten text preserving structure."
  ),
  TRANSLATE(
    "Translate Image",
    "Translate",
    "Translate",
    "Translate text into Hindi, English, Sanskrit, Spanish, etc.",
    "इस image में लिखे text को Hindi में translate करो।",
    "Translate all text in this image into Hindi."
  ),
  SOLVE_MATH(
    "Solve Math",
    "Math",
    "Calculate",
    "Step-by-step math solver with calculator verification",
    "इस image में मौजूद math questions को step-by-step solve करो।",
    "Identify and solve all math questions step-by-step."
  ),
  EXPLAIN_IMAGE(
    "Explain Image",
    "Explain",
    "MenuBook",
    "Educational, diagrammatic, and conceptual explanation",
    "इस photo/diagram का concept आसान भाषा में समझाओ।",
    "Explain the concepts and meaning of this image clearly."
  ),
  SUMMARIZE_DOC(
    "Summarize Document",
    "Document",
    "Description",
    "Summarize, extract dates, amounts, IDs, names, and phone numbers",
    "इस document का summary बताओ और key dates, amounts निकालो।",
    "Summarize this document and extract key dates, amounts, IDs, and names."
  ),
  READ_TABLE(
    "Read Table",
    "Table",
    "TableChart",
    "Extract structured rows and columns into an editable table & CSV",
    "इस image के table को rows और columns में extract करो।",
    "Extract the table in this image into structured rows and columns."
  ),
  ANALYZE_CHART(
    "Analyze Chart",
    "Chart",
    "BarChart",
    "Analyze bar, line, pie charts and explain trends and key values",
    "इस chart/graph का data analyze करो और key trends बताओ।",
    "Analyze the chart, read values, and explain key trends."
  ),
  IDENTIFY_OBJECTS(
    "Identify Objects",
    "Objects",
    "Category",
    "Identify objects with approximate spatial locations (Top, Left, etc.)",
    "इस image में कौन-कौन सी चीजें हैं और कहाँ स्थित हैं?",
    "Identify all objects with spatial locations (Left, Right, Center, etc.)."
  ),
  DESCRIBE_IMAGE(
    "Describe Image",
    "Describe",
    "Visibility",
    "Detailed scene, objects, visual layout, and color palette",
    "इस image का short और detailed description दो।",
    "Provide a detailed description of this image, layout, and colors."
  ),
  CUSTOM_QUESTION(
    "Custom Question",
    "Custom",
    "HelpOutline",
    "Custom user question or specific question selection (e.g. Q5)",
    "",
    ""
  )
}

/**
 * Levels of math explanations
 */
enum class MathExplanationLevel(val displayName: String, val promptDirective: String) {
  SHORT("Short Answer", "Provide a concise summary with the direct final answer and basic formula used."),
  STEP_BY_STEP("Step-by-Step", "Provide a clear step-by-step solution showing each mathematical step clearly before the final answer."),
  DETAILED("Detailed Explanation", "Provide a comprehensive, in-depth explanation explaining the theoretical method, each step, intermediate simplifications, and the final answer."),
  STUDENT_FRIENDLY("Student Friendly", "Explain simply as a supportive math tutor, using intuitive language, clear intermediate steps, and relatable explanations suitable for a student.")
}

/**
 * Structured result for an individual solved math problem
 */
data class MathProblemResult(
  val questionNumber: String = "Q1",
  val questionText: String = "",
  val method: String = "",
  val steps: List<String> = emptyList(),
  val finalAnswer: String = "",
  val isCalculatorVerified: Boolean = false,
  val verificationDetails: String? = null
)

/**
 * Translation language options
 */
enum class TranslationLanguage(val code: String, val displayName: String, val nativeName: String) {
  HINDI("hi", "Hindi", "हिन्दी"),
  ENGLISH("en", "English", "English"),
  SANSKRIT("sa", "Sanskrit", "संस्कृतम्"),
  BENGALI("bn", "Bengali", "বাংলা"),
  MARATHI("mr", "Marathi", "मराठी"),
  GUJARATI("gu", "Gujarati", "ગુજરાતી"),
  PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ"),
  TAMIL("ta", "Tamil", "தமிழ்"),
  TELUGU("te", "Telugu", "తెలుగు"),
  KANNADA("kn", "Kannada", "ಕನ್ನಡ"),
  MALAYALAM("ml", "Malayalam", "മലയാളം"),
  URDU("ur", "Urdu", "اردو"),
  SPANISH("es", "Spanish", "Español"),
  FRENCH("fr", "French", "Français"),
  GERMAN("de", "German", "Deutsch"),
  ARABIC("ar", "Arabic", "العربية")
}

/**
 * Translation display modes
 */
enum class TranslationDisplayMode(val displayName: String) {
  TEXT("Text Translation"),
  SIDE_BY_SIDE("Side-by-Side"),
  OVERLAY("Overlay Preview")
}

/**
 * Structured table extracted from image
 */
data class ExtractedTableData(
  val title: String? = null,
  val headers: List<String> = emptyList(),
  val rows: List<List<String>> = emptyList(),
  val uncertainCells: Set<Pair<Int, Int>> = emptySet() // (rowIndex, colIndex)
) {
  fun toCsvString(): String = buildString {
    if (headers.isNotEmpty()) {
      append(headers.joinToString(",") { escapeCsv(it) }).append("\n")
    }
    for (row in rows) {
      append(row.joinToString(",") { escapeCsv(it) }).append("\n")
    }
  }

  fun toTsvString(): String = buildString {
    if (headers.isNotEmpty()) {
      append(headers.joinToString("\t")).append("\n")
    }
    for (row in rows) {
      append(row.joinToString("\t")).append("\n")
    }
  }

  private fun escapeCsv(str: String): String {
    val needsQuotes = str.contains(",") || str.contains("\"") || str.contains("\n")
    return if (needsQuotes) {
      "\"" + str.replace("\"", "\"\"") + "\""
    } else {
      str
    }
  }
}

/**
 * Document extraction specific fields
 */
data class DocumentDetails(
  val summary: String = "",
  val keyPoints: List<String> = emptyList(),
  val dates: List<String> = emptyList(),
  val amounts: List<String> = emptyList(),
  val names: List<String> = emptyList(),
  val ids: List<String> = emptyList(),
  val phoneNumbers: List<String> = emptyList(),
  val addresses: List<String> = emptyList()
)

/**
 * Spatial object detection result
 */
data class DetectedObjectItem(
  val name: String,
  val count: Int = 1,
  val spatialPosition: String = "Center", // Left, Right, Center, Top, Bottom, etc.
  val description: String? = null
)

/**
 * Comprehensive result from Image Understanding Engine
 */
data class ImageUnderstandingResult(
  val isSuccess: Boolean,
  val task: ImageUnderstandingTask,
  val mainTextResponse: String = "",
  val extractedText: String? = null,
  val tableData: ExtractedTableData? = null,
  val mathResults: List<MathProblemResult> = emptyList(),
  val documentDetails: DocumentDetails? = null,
  val detectedObjects: List<DetectedObjectItem> = emptyList(),
  val detectedLanguage: String? = null,
  val translatedText: String? = null,
  val dominantScene: String? = null,
  val errorMessage: String? = null,
  val isNetworkError: Boolean = false,
  val isQuotaExceeded: Boolean = false,
  val isMissingKey: Boolean = false
)

/**
 * Normalized crop region on an image (0f..1f)
 */
data class ImageCropRegion(
  val left: Float = 0f,
  val top: Float = 0f,
  val right: Float = 1f,
  val bottom: Float = 1f,
  val rotationDegrees: Int = 0
) {
  val isFullImage: Boolean
    get() = left <= 0.01f && top <= 0.01f && right >= 0.99f && bottom >= 0.99f && rotationDegrees == 0

  fun toRectF(bitmapWidth: Int, bitmapHeight: Int): RectF {
    return RectF(
      left * bitmapWidth,
      top * bitmapHeight,
      right * bitmapWidth,
      bottom * bitmapHeight
    )
  }
}
