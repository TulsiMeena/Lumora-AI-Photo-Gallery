package com.example.model.editor

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Standard Crop Aspect Ratios
 */
enum class CropAspectRatio(val displayName: String, val ratio: Float?) {
  FREE("Free", null),
  ORIGINAL("Original", null),
  SQUARE_1_1("1:1", 1.0f),
  PORTRAIT_4_5("4:5", 4f / 5f),
  RATIO_3_4("3:4", 3f / 4f),
  RATIO_4_3("4:3", 4f / 3f),
  RATIO_16_9("16:9", 16f / 9f),
  RATIO_9_16("9:16", 9f / 16f),
  CUSTOM("Custom", null)
}

/**
 * Crop and Geometry Transformation State
 */
data class CropTransform(
  val cropLeft: Float = 0f,
  val cropTop: Float = 0f,
  val cropRight: Float = 1f,
  val cropBottom: Float = 1f,
  val rotationDegrees: Int = 0, // 0, 90, 180, 270
  val straightenAngle: Float = 0f, // -45f to +45f
  val flipHorizontal: Boolean = false,
  val flipVertical: Boolean = false,
  val perspectiveH: Float = 0f, // -30f to +30f
  val perspectiveV: Float = 0f, // -30f to +30f
  val aspectRatio: CropAspectRatio = CropAspectRatio.FREE
) {
  val isDefault: Boolean
    get() = cropLeft == 0f && cropTop == 0f && cropRight == 1f && cropBottom == 1f &&
        rotationDegrees == 0 && straightenAngle == 0f && !flipHorizontal && !flipVertical &&
        perspectiveH == 0f && perspectiveV == 0f
}

/**
 * Light Adjustment Controls
 */
data class LightAdjustments(
  val exposure: Float = 0f,    // -2.0f to +2.0f (EV stops)
  val brightness: Float = 0f,  // -100f to +100f
  val contrast: Float = 0f,    // -100f to +100f
  val highlights: Float = 0f,  // -100f to +100f
  val shadows: Float = 0f,     // -100f to +100f
  val whites: Float = 0f,      // -100f to +100f
  val blacks: Float = 0f       // -100f to +100f
) {
  val isDefault: Boolean
    get() = exposure == 0f && brightness == 0f && contrast == 0f &&
        highlights == 0f && shadows == 0f && whites == 0f && blacks == 0f
}

/**
 * Global Color Adjustments
 */
data class ColorAdjustments(
  val saturation: Float = 0f,  // -100f to +100f
  val vibrance: Float = 0f,    // -100f to +100f
  val temperature: Float = 0f, // -100f (Blue) to +100f (Amber/Warm)
  val tint: Float = 0f,        // -100f (Green) to +100f (Magenta)
  val hue: Float = 0f          // -180f to +180f
) {
  val isDefault: Boolean
    get() = saturation == 0f && vibrance == 0f && temperature == 0f && tint == 0f && hue == 0f
}

/**
 * HSL Color Bands
 */
enum class HslColorBand(val displayName: String, val baseColorHex: Long) {
  RED("Red", 0xFFEF4444),
  ORANGE("Orange", 0xFFF97316),
  YELLOW("Yellow", 0xFFEAB308),
  GREEN("Green", 0xFF22C55E),
  CYAN("Cyan", 0xFF06B6D4),
  BLUE("Blue", 0xFF3B82F6),
  PURPLE("Purple", 0xFFA855F7),
  MAGENTA("Magenta", 0xFFEC4899)
}

data class HslBandAdjustment(
  val hue: Float = 0f,        // -100f to +100f
  val saturation: Float = 0f, // -100f to +100f
  val luminance: Float = 0f   // -100f to +100f
) {
  val isDefault: Boolean get() = hue == 0f && saturation == 0f && luminance == 0f
}

data class HslAdjustments(
  val bands: Map<HslColorBand, HslBandAdjustment> = HslColorBand.values().associateWith { HslBandAdjustment() }
) {
  val isDefault: Boolean get() = bands.values.all { it.isDefault }

  fun updateBand(band: HslColorBand, transform: (HslBandAdjustment) -> HslBandAdjustment): HslAdjustments {
    val current = bands[band] ?: HslBandAdjustment()
    return copy(bands = bands + (band to transform(current)))
  }
}

/**
 * Tone Curves (RGB Master, Red, Green, Blue)
 */
enum class CurveChannel(val displayName: String, val colorHex: Long) {
  RGB("RGB", 0xFFFFFFFF),
  RED("Red", 0xFFEF4444),
  GREEN("Green", 0xFF22C55E),
  BLUE("Blue", 0xFF3B82F6)
}

data class CurvePoint(val x: Float, val y: Float)

data class SingleCurve(
  val points: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
) {
  val isDefault: Boolean
    get() = points.size == 2 &&
        points[0].x == 0f && points[0].y == 0f &&
        points[1].x == 1f && points[1].y == 1f
}

data class CurvesAdjustment(
  val curves: Map<CurveChannel, SingleCurve> = CurveChannel.values().associateWith { SingleCurve() }
) {
  val isDefault: Boolean get() = curves.values.all { it.isDefault }

  fun getCurve(channel: CurveChannel): SingleCurve = curves[channel] ?: SingleCurve()

  fun updateCurve(channel: CurveChannel, newCurve: SingleCurve): CurvesAdjustment {
    return copy(curves = curves + (channel to newCurve))
  }
}

/**
 * Detail & Clarity Adjustments
 */
data class DetailAdjustments(
  val sharpness: Float = 0f,       // 0f to 100f
  val clarity: Float = 0f,         // -100f to +100f
  val texture: Float = 0f,         // -100f to +100f
  val dehaze: Float = 0f,          // -100f to +100f
  val noiseReduction: Float = 0f,  // 0f to 100f
  val grain: Float = 0f            // 0f to 100f
) {
  val isDefault: Boolean
    get() = sharpness == 0f && clarity == 0f && texture == 0f &&
        dehaze == 0f && noiseReduction == 0f && grain == 0f
}

/**
 * Vignette, Fade, and Special Effects
 */
data class VignetteEffects(
  val vignetteAmount: Float = 0f,     // -100f (White) to +100f (Dark)
  val vignetteMidpoint: Float = 50f,  // 0f to 100f
  val vignetteFeather: Float = 50f,   // 0f to 100f
  val vignetteRoundness: Float = 0f,  // -100f to +100f
  val fade: Float = 0f,               // 0f to 100f
  val filmGrain: Float = 0f,          // 0f to 100f
  val blur: Float = 0f,               // 0f to 100f
  val motionBlur: Float = 0f          // 0f to 100f
) {
  val isDefault: Boolean
    get() = vignetteAmount == 0f && fade == 0f && filmGrain == 0f && blur == 0f && motionBlur == 0f
}

/**
 * 3-Way Color Grading (Shadows, Midtones, Highlights)
 */
data class ColorGradingWheel(
  val hue: Float = 0f,        // 0f to 360f
  val saturation: Float = 0f, // 0f to 100f
  val luminance: Float = 0f   // -100f to +100f
) {
  val isDefault: Boolean get() = saturation == 0f && luminance == 0f
}

data class ColorGrading(
  val shadows: ColorGradingWheel = ColorGradingWheel(),
  val midtones: ColorGradingWheel = ColorGradingWheel(),
  val highlights: ColorGradingWheel = ColorGradingWheel(),
  val balance: Float = 0f     // -100f (Shadow bias) to +100f (Highlight bias)
) {
  val isDefault: Boolean
    get() = shadows.isDefault && midtones.isDefault && highlights.isDefault && balance == 0f
}

/**
 * Retouching: Healing & Clone Stamp
 */
enum class RetouchType { HEALING, CLONE }

data class RetouchItem(
  val id: String = UUID.randomUUID().toString(),
  val type: RetouchType = RetouchType.HEALING,
  val sourceX: Float = 0.5f,
  val sourceY: Float = 0.5f,
  val targetX: Float = 0.5f,
  val targetY: Float = 0.5f,
  val radius: Float = 30f,
  val softness: Float = 0.5f,
  val opacity: Float = 1.0f
)

/**
 * Blur & Mosaic (Redaction / Privacy)
 */
enum class BlurMosaicType { BLUR, MOSAIC }

data class BlurMosaicRegion(
  val id: String = UUID.randomUUID().toString(),
  val type: BlurMosaicType = BlurMosaicType.MOSAIC,
  val normalizedLeft: Float = 0.2f,
  val normalizedTop: Float = 0.2f,
  val normalizedRight: Float = 0.8f,
  val normalizedBottom: Float = 0.8f,
  val intensity: Float = 50f,
  val pixelTileSize: Float = 24f
)

/**
 * Drawing Strokes
 */
data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
  val id: String = UUID.randomUUID().toString(),
  val points: List<DrawingPoint> = emptyList(),
  val colorHex: Long = 0xFFFF3B30,
  val strokeWidth: Float = 8f,
  val opacity: Float = 1.0f,
  val isEraser: Boolean = false,
  val softness: Float = 0f
)

/**
 * Text Layer
 */
enum class EditorFont(val displayName: String) {
  SANS_SERIF("Modern Sans"),
  SERIF("Classic Serif"),
  MONOSPACE("Code Mono"),
  CURSIVE("Script Calligraphy")
}

data class TextLayerItem(
  val id: String = UUID.randomUUID().toString(),
  val text: String = "Double tap to edit",
  val font: EditorFont = EditorFont.SANS_SERIF,
  val fontSizeSp: Float = 28f,
  val isBold: Boolean = false,
  val isItalic: Boolean = false,
  val textColorHex: Long = 0xFFFFFFFF,
  val backgroundColorHex: Long? = null, // Pill background
  val outlineColorHex: Long? = null,
  val outlineWidth: Float = 0f,
  val shadowColorHex: Long? = 0x88000000,
  val opacity: Float = 1.0f,
  val normalizedX: Float = 0.5f, // Center of text (0f..1f)
  val normalizedY: Float = 0.5f,
  val rotationDegrees: Float = 0f,
  val scale: Float = 1.0f
)

/**
 * Shapes & Stickers
 */
enum class EditorShapeType(val displayName: String) {
  RECTANGLE("Rectangle"),
  CIRCLE("Circle"),
  LINE("Line"),
  ARROW("Arrow"),
  STAR("Star"),
  HEART("Heart"),
  STICKER("Sticker")
}

data class ShapeStickerItem(
  val id: String = UUID.randomUUID().toString(),
  val type: EditorShapeType = EditorShapeType.RECTANGLE,
  val stickerSymbol: String = "⭐", // For sticker type
  val normalizedX: Float = 0.5f,
  val normalizedY: Float = 0.5f,
  val normalizedWidth: Float = 0.25f,
  val normalizedHeight: Float = 0.25f,
  val colorHex: Long = 0xFF3B82F6,
  val strokeWidth: Float = 4f,
  val isFilled: Boolean = false,
  val opacity: Float = 1.0f,
  val rotationDegrees: Float = 0f
)

/**
 * Layer System & Blend Modes
 */
enum class LayerBlendMode(val displayName: String) {
  NORMAL("Normal"),
  MULTIPLY("Multiply"),
  SCREEN("Screen"),
  OVERLAY("Overlay"),
  SOFT_LIGHT("Soft Light"),
  HARD_LIGHT("Hard Light"),
  DARKEN("Darken"),
  LIGHTEN("Lighten")
}

enum class LayerType(val displayName: String) {
  IMAGE_BASE("Base Image"),
  ADJUSTMENT("Adjustments"),
  DRAWING("Drawing"),
  TEXT("Text"),
  SHAPE("Shape/Sticker"),
  RETOUCH("Healing & Retouch"),
  PRIVACY("Blur & Mosaic")
}

data class EditorLayer(
  val id: String,
  val name: String,
  val type: LayerType,
  val isVisible: Boolean = true,
  val opacity: Float = 1.0f,
  val blendMode: LayerBlendMode = LayerBlendMode.NORMAL
)

/**
 * Editor Tool Navigation Categories
 */
enum class EditorCategory(val displayName: String) {
  CROP("Crop"),
  LIGHT("Light"),
  COLOR("Color"),
  HSL("HSL"),
  CURVES("Curves"),
  DETAIL("Detail"),
  EFFECTS("Effects"),
  GRADING("Grading"),
  RETOUCH("Healing"),
  PRIVACY("Blur"),
  DRAW("Draw"),
  TEXT("Text"),
  SHAPES("Shapes"),
  LAYERS("Layers")
}

/**
 * Before/After Compare Mode
 */
enum class CompareMode {
  NONE,
  HOLD_BEFORE,
  SPLIT_SLIDER,
  SIDE_BY_SIDE
}

/**
 * Export Format Options
 */
enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
  JPEG("JPEG (Balanced)", "jpg", "image/jpeg"),
  PNG("PNG (Lossless)", "png", "image/png"),
  WEBP("WebP (Modern)", "webp", "image/webp")
}

/**
 * Full Non-Destructive Photo Editor State
 */
data class PhotoEditorState(
  val originalMediaId: String = "",
  val originalUri: String = "",
  val title: String = "Edited_Photo",
  val cropTransform: CropTransform = CropTransform(),
  val lightAdjustments: LightAdjustments = LightAdjustments(),
  val colorAdjustments: ColorAdjustments = ColorAdjustments(),
  val hslAdjustments: HslAdjustments = HslAdjustments(),
  val curvesAdjustment: CurvesAdjustment = CurvesAdjustment(),
  val detailAdjustments: DetailAdjustments = DetailAdjustments(),
  val vignetteEffects: VignetteEffects = VignetteEffects(),
  val colorGrading: ColorGrading = ColorGrading(),
  val retouchItems: List<RetouchItem> = emptyList(),
  val blurMosaicRegions: List<BlurMosaicRegion> = emptyList(),
  val drawingStrokes: List<DrawingStroke> = emptyList(),
  val textLayers: List<TextLayerItem> = emptyList(),
  val shapes: List<ShapeStickerItem> = emptyList(),
  val layers: List<EditorLayer> = listOf(
    EditorLayer("layer_base", "Original Photo", LayerType.IMAGE_BASE),
    EditorLayer("layer_adjust", "Tone & Color", LayerType.ADJUSTMENT),
    EditorLayer("layer_retouch", "Retouch", LayerType.RETOUCH),
    EditorLayer("layer_privacy", "Privacy / Mosaic", LayerType.PRIVACY),
    EditorLayer("layer_draw", "Drawing Canvas", LayerType.DRAWING),
    EditorLayer("layer_shapes", "Graphics & Shapes", LayerType.SHAPE),
    EditorLayer("layer_text", "Text Titles", LayerType.TEXT)
  ),
  val activeCategory: EditorCategory = EditorCategory.CROP,
  val compareMode: CompareMode = CompareMode.NONE,
  val splitSliderPosition: Float = 0.5f // 0f (all before) to 1f (all after)
) {
  val hasModifications: Boolean
    get() = !cropTransform.isDefault ||
        !lightAdjustments.isDefault ||
        !colorAdjustments.isDefault ||
        !hslAdjustments.isDefault ||
        !curvesAdjustment.isDefault ||
        !detailAdjustments.isDefault ||
        !vignetteEffects.isDefault ||
        !colorGrading.isDefault ||
        retouchItems.isNotEmpty() ||
        blurMosaicRegions.isNotEmpty() ||
        drawingStrokes.isNotEmpty() ||
        textLayers.isNotEmpty() ||
        shapes.isNotEmpty()
}

/**
 * Project representation stored in Room database
 */
data class EditingProject(
  val id: String,
  val originalMediaId: String,
  val originalUri: String,
  val title: String,
  val thumbnailPath: String? = null,
  val lastModifiedMillis: Long = System.currentTimeMillis(),
  val state: PhotoEditorState
)
