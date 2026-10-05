package com.example.model.creative

import android.graphics.Color
import com.example.model.MediaItem
import com.example.model.resize.OutputFormat
import com.example.model.resize.ResizeMode

enum class CreativeStudioTab(val title: String) {
  ALL("All Tools"),
  COLLAGE("Collage"),
  POSTER("Poster"),
  WATERMARK("Watermark & Sign"),
  CONTACT_SHEET("Contact Sheet"),
  GIF("GIF Maker"),
  SLIDESHOW("Slideshow"),
  BATCH("Batch Tools")
}

enum class CollageLayout(val label: String, val photoCount: Int) {
  SPLIT_2_V("2 Vertical", 2),
  SPLIT_2_H("2 Horizontal", 2),
  GRID_3("3 Photo Grid", 3),
  GRID_4("4 Grid (2x2)", 4),
  GRID_6("6 Grid (2x3)", 6),
  GRID_9("9 Grid (3x3)", 9)
}

data class CollageConfig(
  val layout: CollageLayout = CollageLayout.GRID_4,
  val paddingDp: Float = 8f,
  val cornerRadiusDp: Float = 12f,
  val aspectRatio: Float = 1f, // 1f = 1:1, 0.8f = 4:5, 1.77f = 16:9
  val backgroundColor: Int = Color.WHITE,
  val borderWidthDp: Float = 0f,
  val borderColor: Int = Color.BLACK
)

enum class WatermarkPosition(val label: String) {
  TOP_LEFT("Top Left"),
  TOP_CENTER("Top Center"),
  TOP_RIGHT("Top Right"),
  CENTER("Center"),
  BOTTOM_LEFT("Bottom Left"),
  BOTTOM_CENTER("Bottom Center"),
  BOTTOM_RIGHT("Bottom Right"),
  TILED("Tiled Repeat")
}

data class WatermarkConfig(
  val text: String = "© My Gallery AI",
  val fontSizeSp: Float = 28f,
  val textColor: Int = Color.WHITE,
  val opacity: Float = 0.85f,
  val rotation: Float = 0f,
  val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
  val marginDp: Float = 16f,
  val hasShadow: Boolean = true,
  val tileRepeat: Boolean = false,
  val logoUri: String? = null
)

data class SignaturePoint(val x: Float, val y: Float)

data class SignatureStroke(
  val points: List<SignaturePoint>,
  val color: Int = Color.BLACK,
  val strokeWidth: Float = 5f
)

data class SignatureConfig(
  val strokes: List<SignatureStroke> = emptyList(),
  val importedImageUri: String? = null,
  val opacity: Float = 0.9f,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
  val marginDp: Float = 16f
)

enum class PosterFormat(val label: String, val aspectRatio: Float) {
  SQUARE_1_1("Square 1:1", 1f),
  STORY_9_16("Story 9:16", 9f / 16f),
  PORTRAIT_4_5("Portrait 4:5", 4f / 5f),
  LANDSCAPE_16_9("Landscape 16:9", 16f / 9f),
  A4_DOCUMENT("A4 Sheet", 210f / 297f)
}

enum class PosterTemplate(val label: String) {
  MINIMAL("Minimalist"),
  VINTAGE("Vintage Classic"),
  MODERN("Modern Bold"),
  STORY("Editorial Story"),
  ELEGANT("Elegant Luxe")
}

data class PosterConfig(
  val format: PosterFormat = PosterFormat.PORTRAIT_4_5,
  val template: PosterTemplate = PosterTemplate.MODERN,
  val title: String = "CAPTURED MOMENTS",
  val subtitle: String = "A Visual Journey by My Gallery AI",
  val author: String = "Gallery Studio",
  val primaryColor: Int = Color.parseColor("#38BDF8"),
  val backgroundColor: Int = Color.parseColor("#0F172A")
)

data class ContactSheetConfig(
  val columns: Int = 3,
  val headerTitle: String = "Photo Index & Contact Sheet",
  val showFilename: Boolean = true,
  val showDate: Boolean = true,
  val showDimensions: Boolean = true,
  val showFileSize: Boolean = true,
  val backgroundColor: Int = Color.WHITE,
  val textColor: Int = Color.parseColor("#1E293B")
)

data class GifConfig(
  val frameDelayMs: Int = 300,
  val loopCount: Int = 0, // 0 = infinite loop
  val maxDimension: Int = 480,
  val quality: Int = 80
)

data class SlideshowConfig(
  val durationPerSlideSec: Int = 3,
  val transitionName: String = "Crossfade",
  val loop: Boolean = true
)

enum class BatchOperation(val title: String, val description: String) {
  RESIZE("Batch Resize", "Apply dimensions or aspect ratios to all photos"),
  COMPRESS("Batch Compress", "Reduce file weight to target KB/MB"),
  CONVERT("Batch Convert", "Convert between JPG, PNG, and WebP"),
  RENAME("Batch Rename", "Sequentially rename files with prefix & index"),
  WATERMARK("Batch Watermark", "Stamp copyright or logo on all photos"),
  SIGNATURE("Batch Signature", "Apply handwritten or PNG signature"),
  ADJUSTMENTS("Batch Adjustments", "Apply color, contrast, and brightness"),
  PDF("Compile to PDF", "Generate a multi-page PDF document"),
  CONTACT_SHEET("Contact Sheet", "Combine all images onto a labeled index sheet")
}

data class BatchRenameConfig(
  val prefix: String = "Photo_",
  val suffix: String = "",
  val startNumber: Int = 1,
  val digitsCount: Int = 3,
  val preserveExtension: Boolean = true
)

data class BatchAdjustmentConfig(
  val brightness: Float = 0f, // -50f..+50f
  val contrast: Float = 1f,    // 0.5f..2.0f
  val saturation: Float = 1f,  // 0f..2.0f
  val warmth: Float = 0f,      // -30f..+30f
  val sharpness: Float = 0f    // 0f..2.0f
)

enum class BatchJobStatus {
  IDLE,
  RUNNING,
  PAUSED,
  COMPLETED,
  CANCELLED,
  FAILED
}

data class BatchJob(
  val id: String,
  val operation: BatchOperation,
  val totalItems: Int,
  val completedItems: Int = 0,
  val failedItems: Int = 0,
  val currentFileName: String = "",
  val status: BatchJobStatus = BatchJobStatus.IDLE,
  val outputUris: List<String> = emptyList(),
  val errorDetails: List<String> = emptyList(),
  val startTimeMillis: Long = System.currentTimeMillis()
)

enum class ShareMode(val label: String, val subtitle: String) {
  ORIGINAL("Original Quality", "Send full-resolution untouched media files"),
  COMPRESSED("Compressed Copy", "Optimize size for fast messaging/upload"),
  RESIZED("Resized Copy (Full HD)", "1920px max dimension, preserving aspect ratio")
}

data class BatchResizeParams(
  val targetWidth: Int = 1080,
  val targetHeight: Int = 1080,
  val resizeMode: ResizeMode = ResizeMode.FIT,
  val dpi: Int = 300,
  val outputFormat: OutputFormat = OutputFormat.JPEG,
  val quality: Int = 90
)

data class SharePreviewConfig(
  val items: List<MediaItem>,
  val selectedMode: ShareMode = ShareMode.ORIGINAL,
  val isFromVault: Boolean = false,
  val vaultPasswordVerified: Boolean = false
) {
  val totalSizeBytes: Long
    get() = items.sumOf { it.sizeBytes }

  val formattedTotalSize: String
    get() {
      val kb = totalSizeBytes / 1024.0
      val mb = kb / 1024.0
      return when {
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$totalSizeBytes B"
      }
    }
}

data class TextDesignItem(
  val text: String = "Design Text",
  val fontSizeSp: Float = 28f,
  val isBold: Boolean = true,
  val isItalic: Boolean = false,
  val textColor: Int = Color.WHITE,
  val backgroundColor: Int = Color.TRANSPARENT,
  val outlineColor: Int = Color.BLACK,
  val hasOutline: Boolean = false,
  val hasShadow: Boolean = true,
  val opacity: Float = 1f,
  val rotation: Float = 0f,
  val alignment: String = "Center",
  val letterSpacing: Float = 0f,
  val positionX: Float = 0.5f,
  val positionY: Float = 0.5f
)

enum class FrameType(val label: String) {
  SIMPLE("Clean Border"),
  ROUNDED("Soft Rounded"),
  POLAROID("Polaroid Classic"),
  SHADOW("Soft Drop Shadow"),
  VINTAGE("Vintage Frame")
}

data class FrameConfig(
  val type: FrameType = FrameType.POLAROID,
  val borderWidthDp: Float = 16f,
  val color: Int = Color.WHITE,
  val cornerRadiusDp: Float = 12f,
  val shadowElevationDp: Float = 10f,
  val paddingDp: Float = 16f
)

data class StickerItem(
  val id: String,
  val emojiOrSymbol: String,
  val isEmoji: Boolean = true,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val positionX: Float = 0.5f,
  val positionY: Float = 0.5f,
  val opacity: Float = 1f
)

enum class CreativeTemplateCategory(val label: String) {
  BIRTHDAY("Birthday Celebration"),
  WEDDING("Wedding & Anniversary"),
  TRAVEL("Travel Journal"),
  FESTIVAL("Festival & Holiday"),
  BUSINESS("Business Portfolio"),
  PRODUCT("Product Showcase"),
  SOCIAL_MEDIA("Social Story & Post"),
  ANNOUNCEMENT("Special Announcement")
}

data class CreativeTemplate(
  val id: String,
  val title: String,
  val category: CreativeTemplateCategory,
  val defaultSubtitle: String,
  val defaultTagline: String,
  val accentColor: Int,
  val backgroundDark: Boolean = true
)

