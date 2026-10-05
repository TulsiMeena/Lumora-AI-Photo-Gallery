package com.example.model.resize

import androidx.compose.ui.graphics.Color

enum class ResizeUnit(val symbol: String, val label: String) {
  PIXELS("px", "Pixels"),
  INCHES("in", "Inches"),
  CENTIMETERS("cm", "Centimeters"),
  MILLIMETERS("mm", "Millimeters")
}

enum class ResizeMode(val label: String, val description: String) {
  FIT("Fit (Contain)", "Resizes image to fit completely within target dimensions without cropping. Aspect ratio is preserved."),
  FILL("Fill (Crop to Size)", "Fills entire target dimensions. Any excess content is cropped symmetrically from the center."),
  STRETCH("Stretch", "Forces exact width and height. May distort aspect ratio if proportions differ."),
  CROP_TO_SIZE("Crop to Size", "Allows interactive crop before resizing to exact target dimensions."),
  ORIGINAL_RATIO("Original Ratio", "Calculates the opposing dimension automatically to preserve original proportions.")
}

enum class OutputFormat(
  val displayName: String,
  val extension: String,
  val mimeType: String,
  val supportsTransparency: Boolean,
  val supportsQualitySetting: Boolean
) {
  JPEG("JPEG", ".jpg", "image/jpeg", supportsTransparency = false, supportsQualitySetting = true),
  PNG("PNG", ".png", "image/png", supportsTransparency = true, supportsQualitySetting = false),
  WEBP("WebP", ".webp", "image/webp", supportsTransparency = true, supportsQualitySetting = true)
}

enum class QualityPreset(val label: String, val qualityValue: Int) {
  MAXIMUM("Maximum (100%)", 100),
  HIGH("High (90%)", 90),
  MEDIUM("Medium (75%)", 75),
  CUSTOM("Custom", 85)
}

enum class DimensionCategory(val label: String) {
  SOCIAL_MEDIA("Social Media"),
  PRINT("Print & Paper"),
  DOCUMENTS("Documents & Portals"),
  PASSPORT("Passport & Visa")
}

data class DimensionPreset(
  val id: String,
  val title: String,
  val category: DimensionCategory,
  val width: Double,
  val height: Double,
  val unit: ResizeUnit,
  val dpi: Int = 300,
  val description: String = "",
  val aspectLabel: String = ""
)

data class PassportStandard(
  val id: String,
  val countryOrStandard: String,
  val widthMm: Double,
  val heightMm: Double,
  val defaultDpi: Int = 300,
  val popularUse: String,
  val faceCoverageNote: String = "Face height: 70–80% of photo",
  val recommendedBg: String = "Plain white or off-white"
) {
  val widthInches: Double get() = widthMm / 25.4
  val heightInches: Double get() = heightMm / 25.4
  val widthCm: Double get() = widthMm / 10.0
  val heightCm: Double get() = heightMm / 10.0

  fun getPixels(dpi: Int = defaultDpi): Pair<Int, Int> {
    val pxW = Math.round((widthMm / 25.4) * dpi).toInt()
    val pxH = Math.round((heightMm / 25.4) * dpi).toInt()
    return Pair(pxW, pxH)
  }
}

enum class PhotoSheetPaperSize(
  val displayName: String,
  val widthMm: Double,
  val heightMm: Double
) {
  A4("A4 (210 × 297 mm)", 210.0, 297.0),
  A5("A5 (148 × 210 mm)", 148.0, 210.0),
  LETTER("Letter (8.5 × 11 in)", 215.9, 279.4),
  FOUR_BY_SIX("4 × 6 Photo Paper (102 × 152 mm)", 101.6, 152.4)
}

data class PhotoSheetConfig(
  val paperSize: PhotoSheetPaperSize = PhotoSheetPaperSize.FOUR_BY_SIX,
  val photoWidthMm: Double = 35.0,
  val photoHeightMm: Double = 45.0,
  val copiesCount: Int = 8,
  val spacingMm: Double = 3.0,
  val marginMm: Double = 8.0,
  val isLandscape: Boolean = false,
  val drawCutLines: Boolean = true,
  val targetDpi: Int = 300
)

enum class RenamePattern(val label: String, val example: String) {
  ORIGINAL_SUFFIX("Original + '_resized'", "IMG_001_resized.jpg"),
  CUSTOM_PREFIX("Custom Prefix", "Passport_001.jpg"),
  CUSTOM_SUFFIX("Custom Suffix", "Photo_300dpi.jpg"),
  SEQUENTIAL("Sequential Numbers", "photo_001.jpg")
}

data class BatchResizeConfig(
  val renamePattern: RenamePattern = RenamePattern.ORIGINAL_SUFFIX,
  val customText: String = "resized",
  val preserveMetadata: Boolean = false,
  val outputFormat: OutputFormat = OutputFormat.JPEG,
  val quality: Int = 90
)

data class ImageMetadataInfo(
  val fileName: String = "image.jpg",
  val formatName: String = "JPEG",
  val widthPx: Int = 1920,
  val heightPx: Int = 1080,
  val sizeBytes: Long = 0L,
  val existingDpi: Int = 72,
  val aspectRatioStr: String = "16:9",
  val hasAlpha: Boolean = false,
  val colorSpace: String = "sRGB"
) {
  val formattedSize: String
    get() {
      val kb = sizeBytes / 1024.0
      val mb = kb / 1024.0
      return if (mb >= 1.0) "%.2f MB".format(mb) else "%.1f KB".format(kb)
    }

  fun getPhysicalDimensions(dpi: Int = existingDpi): String {
    val effectiveDpi = if (dpi > 0) dpi else 72
    val wIn = widthPx.toDouble() / effectiveDpi
    val hIn = heightPx.toDouble() / effectiveDpi
    val wCm = wIn * 2.54
    val hCm = hIn * 2.54
    return "%.1f × %.1f cm (%.1f × %.1f in) @ %d DPI".format(wCm, hCm, wIn, hIn, effectiveDpi)
  }
}

data class ResizeExecutionResult(
  val originalUri: String,
  val originalTitle: String,
  val outputUri: String,
  val outputFilePath: String,
  val width: Int,
  val height: Int,
  val dpi: Int,
  val outputFormat: OutputFormat,
  val fileSizeBytes: Long,
  val isSuccess: Boolean = true,
  val errorMessage: String? = null
)
