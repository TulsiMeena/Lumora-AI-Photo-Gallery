package com.example

import com.example.data.ai.editor.GeminiAiPhotoEditorEngine
import com.example.model.ai.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPhotoEditorTest {

  @Test
  fun testToolCategoriesExist() {
    val categories = AiEditorToolCategory.values()
    assertTrue(categories.size >= 10)
    assertNotNull(AiEditorToolCategory.PROMPT)
    assertNotNull(AiEditorToolCategory.OBJECTS)
    assertNotNull(AiEditorToolCategory.BACKGROUND)
    assertNotNull(AiEditorToolCategory.ENHANCE)
    assertNotNull(AiEditorToolCategory.PORTRAIT)
    assertNotNull(AiEditorToolCategory.RELIGHT)
    assertNotNull(AiEditorToolCategory.SKY)
    assertNotNull(AiEditorToolCategory.EXPAND)
    assertNotNull(AiEditorToolCategory.STYLES)
    assertNotNull(AiEditorToolCategory.RESTORE)
  }

  @Test
  fun testHindiTranslationAndStructuredPromptBuilding() {
    // 1. Background removal in Hindi
    val prompt1 = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.BACKGROUND,
      userPrompt = "Background हटाकर white background कर दो",
      hasMask = false
    )
    assertTrue(prompt1.contains("Remove the entire background cleanly"))
    assertTrue(prompt1.contains("Background AI"))

    // 2. Beach background in Hindi
    val prompt2 = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.BACKGROUND,
      userPrompt = "इस आदमी के पीछे beach लगा दो",
      hasMask = false
    )
    assertTrue(prompt2.contains("tropical beach"))

    // 3. Sky sunset in Hindi
    val prompt3 = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.SKY,
      userPrompt = "आसमान को sunset बना दो",
      hasMask = false
    )
    assertTrue(prompt3.contains("sunset"))

    // 4. Clarity & sharp in Hindi
    val prompt4 = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.ENHANCE,
      userPrompt = "Photo को ज्यादा clear और sharp कर दो",
      hasMask = false
    )
    assertTrue(prompt4.contains("clarity, sharpness"))

    // 5. Old photo restore in Hindi
    val prompt5 = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.RESTORE,
      userPrompt = "इस पुराने photo को restore कर दो",
      hasMask = false
    )
    assertTrue(prompt5.contains("Restore this old photograph"))

    // 6. Passport photo in Hindi/English
    val prompt6 = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.PORTRAIT,
      userPrompt = "इस image को passport-photo style में तैयार करो",
      hasMask = false
    )
    assertTrue(prompt6.contains("passport photo style"))
  }

  @Test
  fun testMaskPromptInclusion() {
    val withMaskPrompt = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.OBJECTS,
      userPrompt = "Remove unwanted person in the background",
      hasMask = true
    )
    assertTrue(withMaskPrompt.contains("NOTE: The attached image contains red semi-transparent overlay brushstrokes"))
    assertTrue(withMaskPrompt.contains("inpaint and replace ONLY the marked area"))

    val withoutMaskPrompt = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.OBJECTS,
      userPrompt = "Remove unwanted person in the background",
      hasMask = false
    )
    assertFalse(withoutMaskPrompt.contains("overlay brushstrokes"))
  }

  @Test
  fun testRelightStrengthApplication() {
    val prompt = GeminiAiPhotoEditorEngine.buildStructuredPrompt(
      category = AiEditorToolCategory.RELIGHT,
      userPrompt = "Apply sunset rim light",
      hasMask = false,
      relightStrength = 0.65f
    )
    assertTrue(prompt.contains("65%"))
  }

  @Test
  fun testBackgroundPresetsCoverage() {
    val presets = BackgroundPreset.values()
    assertTrue(presets.any { it.isTransparent })
    assertTrue(presets.any { it == BackgroundPreset.WHITE_STUDIO })
    assertTrue(presets.any { it == BackgroundPreset.LUXURY_OFFICE })
    assertTrue(presets.any { it == BackgroundPreset.SUNSET_BEACH })
  }

  @Test
  fun testEnhancePresetsCoverage() {
    val presets = EnhancePreset.values()
    assertTrue(presets.any { it == EnhancePreset.NATURAL_HDR })
    assertTrue(presets.any { it == EnhancePreset.CRYSTAL_SHARP })
    assertTrue(presets.any { it == EnhancePreset.UNBLUR_FACES })
  }
}
