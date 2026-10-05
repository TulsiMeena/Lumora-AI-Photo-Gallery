package com.example.model.pdf

import android.net.Uri

enum class PdfStandardSize(
  val displayName: String,
  val widthPt: Float,
  val heightPt: Float,
  val dimensionDescription: String
) {
  A0("A0", 2384f, 3370f, "841 × 1189 mm"),
  A1("A1", 1684f, 2384f, "594 × 841 mm"),
  A2("A2", 1191f, 1684f, "420 × 594 mm"),
  A3("A3", 842f, 1191f, "297 × 420 mm"),
  A4("A4", 595f, 842f, "210 × 297 mm"),
  A5("A5", 420f, 595f, "148 × 210 mm"),
  A6("A6", 298f, 420f, "105 × 148 mm"),
  LETTER("Letter", 612f, 792f, "8.5 × 11 in"),
  LEGAL("Legal", 612f, 1008f, "8.5 × 14 in"),
  TABLOID("Tabloid", 792f, 1224f, "11 × 17 in"),
  CUSTOM("Custom", 595f, 842f, "User Defined")
}

enum class PdfDimensionUnit(val symbol: String, val pointsPerUnit: Float) {
  MM("mm", 72f / 25.4f),     // 2.8346 pt per mm
  CM("cm", 72f / 2.54f),      // 28.346 pt per cm
  INCH("inch", 72f),          // 72 pt per inch
  PT("pt", 1f),               // 1 pt
  PX("px", 72f / 96f);        // 0.75 pt per px (standard 96 DPI screen px)

  fun toPoints(value: Float): Float = value * pointsPerUnit
  fun fromPoints(points: Float): Float = if (pointsPerUnit > 0) points / pointsPerUnit else points
}

enum class PdfOrientation(val displayName: String) {
  PORTRAIT("Portrait"),
  LANDSCAPE("Landscape"),
  AUTO("Auto (Based on Image)")
}

enum class PdfImageFit(val displayName: String, val description: String) {
  FIT("Fit (Letterbox)", "Maintains aspect ratio with margins if needed"),
  FILL("Fill Page", "Fills entire page; crops overflowing edges"),
  ORIGINAL("Original Size", "Centers image at native resolution/DPI"),
  CROP("Crop & Center", "Crops square or page ratio from center"),
  STRETCH("Stretch", "Stretches to fill page without preserving aspect ratio"),
  CENTER("Center", "Centers on page without enlarging")
}

enum class PdfMargin(val displayName: String, val points: Float, val description: String) {
  NONE("No Margin", 0f, "0 mm (Edge to edge)"),
  SMALL("Small", 14.17f, "5 mm"),
  MEDIUM("Medium", 34f, "12 mm (Recommended)"),
  LARGE("Large", 70.87f, "25 mm (Formal/Binder)"),
  CUSTOM("Custom", 0f, "User specified")
}

enum class PdfBackground(val displayName: String, val colorHex: Long) {
  WHITE("Pure White", 0xFFFFFFFF),
  IVORY("Warm Ivory", 0xFFFAF8F5),
  SOFT_GRAY("Soft Gray", 0xFFF3F4F6),
  DARK("Blueprint Dark", 0xFF1E293B),
  CUSTOM("Custom Color", 0xFFFFFFFF)
}

enum class PdfQualityPreset(
  val displayName: String,
  val description: String,
  val targetDpi: Int,
  val jpegQuality: Int,
  val scaleFactor: Float
) {
  SMALL_FILE("Small File", "Optimized for email and web uploads (120 DPI)", 120, 60, 0.5f),
  BALANCED("Balanced", "Great balance of clarity and size (180 DPI)", 180, 78, 0.75f),
  HIGH_QUALITY("High Quality", "Sharp documents and readable small text (250 DPI)", 250, 90, 0.9f),
  MAXIMUM("Maximum Quality", "Lossless/archival grade fidelity (300 DPI)", 300, 98, 1.0f)
}

enum class PdfTargetSizeOption(val displayName: String, val bytes: Long) {
  NO_LIMIT("No Limit", 0L),
  KB_500("500 KB (Govt Portals)", 500 * 1024L),
  MB_1("1 MB (Email Friendly)", 1024 * 1024L),
  MB_2("2 MB (Standard)", 2 * 1024 * 1024L),
  MB_5("5 MB (High Res)", 5 * 1024 * 1024L),
  MB_10("10 MB (Documents)", 10 * 1024 * 1024L),
  MB_20("20 MB (Portfolios)", 20 * 1024 * 1024L),
  MB_50("50 MB (Maximum)", 50 * 1024 * 1024L),
  CUSTOM("Custom Target Size", 1024 * 1024L)
}

data class PdfCreatorPage(
  val id: String,
  val sourceUri: String,
  val title: String = "",
  val rotationDegrees: Int = 0,
  val fitOverride: PdfImageFit? = null,
  val orientationOverride: PdfOrientation? = null,
  val ocrText: String = "",
  val thumbnailUri: String? = null
)

data class PdfConfig(
  val standardSize: PdfStandardSize = PdfStandardSize.A4,
  val customWidth: Float = 210f,
  val customHeight: Float = 297f,
  val customUnit: PdfDimensionUnit = PdfDimensionUnit.MM,
  val orientation: PdfOrientation = PdfOrientation.PORTRAIT,
  val imageFit: PdfImageFit = PdfImageFit.FIT,
  val margin: PdfMargin = PdfMargin.NONE,
  val customMarginValue: Float = 0f,
  val customMarginUnit: PdfDimensionUnit = PdfDimensionUnit.MM,
  val background: PdfBackground = PdfBackground.WHITE,
  val customBackgroundColor: Long = 0xFFFFFFFF,
  val qualityPreset: PdfQualityPreset = PdfQualityPreset.BALANCED,
  val customDpi: Int = 180,
  val customJpegQuality: Int = 78,
  val targetSizeOption: PdfTargetSizeOption = PdfTargetSizeOption.NO_LIMIT,
  val customTargetSizeBytes: Long = 1024 * 1024L,
  val isMaxLimitStrict: Boolean = false,
  val fileName: String = "",
  val title: String = "",
  val author: String = "",
  val subject: String = "",
  val keywords: String = ""
) {
  val effectiveWidthPt: Float
    get() = if (standardSize == PdfStandardSize.CUSTOM) {
      customUnit.toPoints(customWidth)
    } else {
      standardSize.widthPt
    }

  val effectiveHeightPt: Float
    get() = if (standardSize == PdfStandardSize.CUSTOM) {
      customUnit.toPoints(customHeight)
    } else {
      standardSize.heightPt
    }

  val effectiveMarginPt: Float
    get() = if (margin == PdfMargin.CUSTOM) {
      customMarginUnit.toPoints(customMarginValue)
    } else {
      margin.points
    }

  val effectiveTargetBytes: Long
    get() = if (targetSizeOption == PdfTargetSizeOption.CUSTOM) {
      customTargetSizeBytes
    } else {
      targetSizeOption.bytes
    }
}

data class PdfHistoryItem(
  val id: String,
  val fileName: String,
  val filePath: String,
  val fileSizeBytes: Long,
  val pageCount: Int,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val pageSizeName: String = "A4",
  val orientation: String = "Portrait",
  val thumbnailUri: String? = null,
  val title: String = "",
  val author: String = "",
  val subject: String = "",
  val keywords: String = "",
  val targetSizeBytes: Long = 0L,
  val qualityPreset: String = "Balanced"
) {
  val formattedSize: String
    get() {
      val kb = fileSizeBytes / 1024.0
      val mb = kb / 1024.0
      return if (mb >= 1.0) {
        String.format(java.util.Locale.US, "%.1f MB", mb)
      } else {
        String.format(java.util.Locale.US, "%d KB", kb.toLong())
      }
    }

  val formattedDate: String
    get() = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
      .format(java.util.Date(createdAtMillis))
}

data class PdfGenerationProgress(
  val currentPage: Int,
  val totalPages: Int,
  val statusMessage: String,
  val percent: Int = 0
)

sealed class PdfGenerationResult {
  data class Success(val item: PdfHistoryItem, val uri: Uri) : PdfGenerationResult()
  data class Error(val message: String) : PdfGenerationResult()
}
