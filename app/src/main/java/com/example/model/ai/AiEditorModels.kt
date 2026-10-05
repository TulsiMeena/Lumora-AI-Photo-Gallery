package com.example.model.ai

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Tool categories for Gemini AI Photo Editor
 */
enum class AiEditorToolCategory(val displayName: String, val iconName: String, val description: String) {
  PROMPT("Natural Prompt", "AutoAwesome", "Describe any edit in Hindi, English or Hinglish"),
  OBJECTS("Magic Eraser", "AutoFixHigh", "Remove unwanted objects, wires, people or fill areas"),
  BACKGROUND("Background AI", "Wallpaper", "Remove, replace, blur or generate backgrounds"),
  ENHANCE("AI Enhance", "Tune", "Upscale, unblur, sharpen and denoise"),
  PORTRAIT("Portrait AI", "Face", "Enhance skin, facial details and portrait lighting"),
  RELIGHT("AI Relight", "LightMode", "Adjust studio, sunset, ambient and cinematic lighting"),
  SKY("Sky AI", "CloudQueue", "Replace sky with sunset, azure, night or dramatic clouds"),
  EXPAND("AI Expand", "AspectRatio", "Generative outpainting and aspect ratio expansion"),
  STYLES("AI Styles", "Palette", "Artistic transformations: film, watercolor, sketch, anime"),
  RESTORE("Old Photo Restore", "HistoryEdu", "Fix scratches, noise, faded colors and restore vintage photos")
}

/**
 * Historical generation step in the AI editing session
 */
data class AiGenerationStep(
  val id: String = UUID.randomUUID().toString(),
  val bitmap: Bitmap,
  val prompt: String,
  val category: AiEditorToolCategory,
  val timestamp: Long = System.currentTimeMillis(),
  val detailsNote: String? = null
)

/**
 * Mask stroke for interactive object eraser / generative fill
 */
data class AiMaskStroke(
  val points: List<Offset>,
  val strokeWidth: Float = 40f
)

/**
 * Result from Gemini AI photo editing engine
 */
data class AiEditorResult(
  val isSuccess: Boolean,
  val resultBitmap: Bitmap? = null,
  val message: String? = null,
  val errorMessage: String? = null,
  val isQuotaExceeded: Boolean = false,
  val isMissingKey: Boolean = false,
  val isNetworkError: Boolean = false
)

/**
 * Background presets for 1-tap edits
 */
enum class BackgroundPreset(val title: String, val prompt: String, val isTransparent: Boolean = false) {
  TRANSPARENT("Transparent", "Remove the background completely and make it fully transparent, isolating the foreground subject cleanly", true),
  WHITE_STUDIO("Pure White", "Remove the background and replace it with a clean pure white professional studio background"),
  LUXURY_OFFICE("Modern Office", "Replace the background with a softly blurred, modern luxury corporate office interior with glass and warm ambient light"),
  LUSH_NATURE("Lush Nature", "Replace the background with a scenic lush green botanical garden with soft sunlight filtering through leaves"),
  SUNSET_BEACH("Sunset Beach", "Replace the background with a romantic golden sunset over ocean waves and sandy beach"),
  INDIAN_WEDDING("Festive Bokeh", "Replace the background with festive golden fairy lights, marigold floral decor and warm celebratory bokeh"),
  NEON_CYBERPUNK("Neon Cyberpunk", "Replace the background with a futuristic cyberpunk cityscape with rain reflections and vivid blue and magenta neon lights"),
  BLUR_BACKGROUND("Portrait Blur", "Keep the existing background but apply a natural optical shallow depth of field (f/1.4 lens bokeh) blur behind the subject")
}

/**
 * AI Enhance presets
 */
enum class EnhancePreset(val title: String, val prompt: String) {
  NATURAL_HDR("Natural HDR", "Enhance the photo with natural dynamic range, balanced exposure, rich contrast, and true-to-life vibrant colors"),
  CRYSTAL_SHARP("Crystal Sharp", "Sharpen the photo optics, reveal crisp edge definition, reduce motion blur, and boost micro-contrast without over-sharpening artifacts"),
  DENOISE_CLEAN("Denoise & Clean", "Clean digital sensor noise, luminance grain, and compression artifacts while preserving authentic surface textures"),
  UNBLUR_FACES("Unblur & Recover", "Unblur subtle motion and out-of-focus areas, recovering crisp facial features and sharp focal details"),
  LOW_LIGHT_BOOST("Low-Light Night", "Brighten underexposed shadows and low-light night capture, recovering rich colors and detail while suppressing noise"),
  MAX_DETAIL("Maximum Detail", "Upscale and reconstruct high-frequency textures, fabrics, foliage, and architectural details to maximum clarity")
}

/**
 * Portrait presets
 */
enum class PortraitPreset(val title: String, val prompt: String) {
  NATURAL_GLOW("Natural Glow", "Enhance portrait with natural healthy skin tone, clean blemishes while preserving authentic pores, brighten eyes"),
  STUDIO_LIGHTING("Studio Lighting", "Apply professional 3-point portrait studio lighting with soft key light, gentle fill, and subtle hair rim light"),
  HAIR_REFINEMENT("Hair & Edges", "Refine hair strand details, remove stray flyaway hairs, and sharpen edges cleanly"),
  BOKEH_DEPTH("Portrait Depth", "Create creamy aesthetic portrait background bokeh blur while maintaining razor-sharp subject isolation"),
  PASSPORT_STYLE("Passport / ID Style", "Format portrait in professional passport photo style with neutral lighting, clean off-white background and upright pose")
}

/**
 * AI Relight presets
 */
enum class RelightPreset(val title: String, val prompt: String) {
  STUDIO_KEY("Studio Key", "Relight scene with professional photographic studio lighting, soft diffused frontal key light and balanced ambient fill"),
  WARM_GOLDEN("Golden Hour", "Relight with warm, radiant golden-hour sunlight casting gentle amber highlights and long soft shadows"),
  COOL_TWILIGHT("Cool Blue Hour", "Relight with cool twilight blue-hour ambient atmosphere, soft violet-blue tones and gentle highlights"),
  SUNSET_RIM("Sunset Rim", "Add dramatic warm rim lighting along the edges of the subject from a low-angle golden sunset"),
  DRAMATIC_REMBRANDT("Rembrandt Light", "Apply classic Rembrandt portrait lighting with directional side light, triangle cheek highlight and deep moody shadows"),
  SOFT_DIFFUSED("Soft Diffuser", "Diffuse harsh lighting and hard shadows into gentle, flattering cloud-filtered daylight")
}

/**
 * Sky AI presets
 */
enum class SkyPreset(val title: String, val prompt: String) {
  AZURE_CLEAR("Clear Azure", "Replace the sky with a clear vibrant blue sky with subtle atmospheric gradient, keeping the foreground seamlessly preserved"),
  VIBRANT_SUNSET("Vibrant Sunset", "Replace the sky with a dramatic sunset of rich crimson, orange, and gold clouds, adjusting foreground lighting to match"),
  GOLDEN_SUNRISE("Golden Sunrise", "Replace the sky with a peaceful golden morning sunrise with soft amber morning light bathing the horizon"),
  CUMULUS_CLOUDS("Fluffy Clouds", "Replace the sky with a picturesque summer sky filled with fluffy white cumulus clouds"),
  STORMY_DRAMA("Stormy Drama", "Replace the sky with dramatic, moody dark storm clouds and atmospheric silver-gray highlights"),
  STARRY_NIGHT("Milky Way Night", "Replace the sky with a breathtaking dark night sky showcasing the Milky Way galaxy, cosmic stars and nebulae"),
  AURORA_BOREALIS("Aurora Borealis", "Replace the sky with vibrant green and violet northern lights shimmering across a starry night horizon")
}

/**
 * AI Expand aspect ratios
 */
enum class ExpandDirection(val title: String, val prompt: String) {
  ALL_SIDES("All Sides", "Generatively outpaint and expand the scene outward in all directions, continuing the natural environment seamlessly"),
  EXPAND_HORIZONTAL("Wider (16:9)", "Expand the image horizontally on left and right sides to create a cinematic 16:9 panoramic composition"),
  EXPAND_VERTICAL("Taller (9:16)", "Expand the image vertically top and bottom to create a full 9:16 vertical portrait composition"),
  EXPAND_SQUARE("Square (1:1)", "Expand surrounding borders to format into a perfectly balanced 1:1 square canvas")
}

/**
 * Art Style presets
 */
enum class StylePreset(val title: String, val prompt: String) {
  VINTAGE_35MM("35mm Film", "Transform visual style into authentic 35mm analog film aesthetic with subtle warm grain, classic Kodachrome color profile and nostalgic tones"),
  WATERCOLOR("Watercolor Art", "Transform image into an artistic watercolor painting with delicate paper texture, fluid washes, and organic pigment bleeding"),
  OIL_PAINTING("Oil on Canvas", "Transform into a classical oil painting with textured brushstrokes, rich impasto depth, and masterwork palette"),
  PENCIL_SKETCH("Pencil Sketch", "Transform into an intricate graphite pencil sketch with detailed cross-hatching, fine line work, and monochrome shading"),
  ANIME_STUDIO("Anime Style", "Transform into a vibrant Japanese animated film style with vivid clean line work, saturated skies, and painterly backgrounds"),
  CYBERPUNK_NEON("Cyberpunk", "Transform into a futuristic cyberpunk aesthetic with high contrast, neon teal and magenta lighting, and dark reflective shadows"),
  RETRO_1980S("1980s Retro", "Transform into a vibrant 1980s synthwave aesthetic with bold color grading, subtle scanlines, and retro neon glow"),
  FINE_ART_BW("Fine Art B&W", "Transform into a gallery-worthy fine art black and white photograph with deep velvet blacks, luminous whites, and zone-system tonal range")
}

/**
 * Old Photo Restoration presets
 */
enum class RestorePreset(val title: String, val prompt: String) {
  FULL_RESTORE("Full Restoration", "Thoroughly restore this vintage photograph: remove scratches, cracks, fold lines, dust spots, reduce paper noise, and sharpen soft details"),
  COLORIZE_BW("Colorize & Revive", "Intelligently colorize this black and white vintage photo with realistic, natural, historically accurate skin tones and environmental colors"),
  FACE_REPAIR("Face Detail Repair", "Restore and sharpen facial features in this degraded old photo, eliminating blur and reconstructing authentic eye, nose and lip definition"),
  DE_FADE("Revive Faded Colors", "Restore faded, yellowed, or washed-out vintage colors back to their original vibrant, rich, balanced photographic appearance"),
  REMOVE_SCRATCHES("Erase Scratches & Creases", "Seamlessly inpaint and remove physical surface damage, dust specks, water stains, and fold creases while keeping original content intact")
}
