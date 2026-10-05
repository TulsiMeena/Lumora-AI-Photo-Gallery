package com.example.model.resize

object ResizePresets {

  val PASSPORT_STANDARDS = listOf(
    PassportStandard(
      id = "passport_35x45",
      countryOrStandard = "3.5 × 4.5 cm Standard",
      widthMm = 35.0,
      heightMm = 45.0,
      defaultDpi = 300,
      popularUse = "India, UK, Schengen, Europe, Australia, UAE, Canada Visa",
      faceCoverageNote = "Head height 32–36 mm (70–80% of photo). Centered eyes.",
      recommendedBg = "Plain white or light off-white background."
    ),
    PassportStandard(
      id = "passport_2x2_in",
      countryOrStandard = "2 × 2 inch (51 × 51 mm)",
      widthMm = 50.8,
      heightMm = 50.8,
      defaultDpi = 300,
      popularUse = "United States Passport & Visa, India OCI, Non-immigrant DS-160",
      faceCoverageNote = "Head between 1 - 1 3/8 inches (25 - 35 mm). Eye level 1 1/8 - 1 3/8 in.",
      recommendedBg = "Strict plain white background. No shadows."
    ),
    PassportStandard(
      id = "passport_50x50_mm",
      countryOrStandard = "50 × 50 mm International",
      widthMm = 50.0,
      heightMm = 50.0,
      defaultDpi = 300,
      popularUse = "Square Visa / ID (Israel, Nepal, Colombia)",
      faceCoverageNote = "Full face frontal view, centered.",
      recommendedBg = "White or off-white background."
    ),
    PassportStandard(
      id = "passport_33x48_mm",
      countryOrStandard = "33 × 48 mm Standard",
      widthMm = 33.0,
      heightMm = 48.0,
      defaultDpi = 300,
      popularUse = "China Passport & Chinese Visa standard",
      faceCoverageNote = "Head width 15–22 mm, head height 28–33 mm.",
      recommendedBg = "White or light blue background."
    ),
    PassportStandard(
      id = "passport_50x70_mm",
      countryOrStandard = "50 × 70 mm Standard",
      widthMm = 50.0,
      heightMm = 70.0,
      defaultDpi = 300,
      popularUse = "Canada Passport Official (In-person submission)",
      faceCoverageNote = "Face length between 31 mm and 36 mm.",
      recommendedBg = "Pure white or light-coloured background."
    ),
    PassportStandard(
      id = "passport_35x35_mm",
      countryOrStandard = "35 × 35 mm Standard",
      widthMm = 35.0,
      heightMm = 35.0,
      defaultDpi = 300,
      popularUse = "Japan Visa, specific government application forms",
      faceCoverageNote = "Centered face, neutral expression.",
      recommendedBg = "Plain white background."
    )
  )

  val ALL_PRESETS = listOf(
    // Social Media
    DimensionPreset(
      id = "social_1080_1080",
      title = "Square Post (1:1)",
      category = DimensionCategory.SOCIAL_MEDIA,
      width = 1080.0,
      height = 1080.0,
      unit = ResizeUnit.PIXELS,
      dpi = 72,
      description = "Instagram & Facebook square feed post",
      aspectLabel = "1:1"
    ),
    DimensionPreset(
      id = "social_1080_1350",
      title = "Portrait Post (4:5)",
      category = DimensionCategory.SOCIAL_MEDIA,
      width = 1080.0,
      height = 1350.0,
      unit = ResizeUnit.PIXELS,
      dpi = 72,
      description = "Maximum vertical Instagram feed post",
      aspectLabel = "4:5"
    ),
    DimensionPreset(
      id = "social_1080_1920",
      title = "Story / Reel / Shorts (9:16)",
      category = DimensionCategory.SOCIAL_MEDIA,
      width = 1080.0,
      height = 1920.0,
      unit = ResizeUnit.PIXELS,
      dpi = 72,
      description = "Full screen vertical story, Reel or status",
      aspectLabel = "9:16"
    ),
    DimensionPreset(
      id = "social_1200_630",
      title = "Landscape / Link Preview (1.91:1)",
      category = DimensionCategory.SOCIAL_MEDIA,
      width = 1200.0,
      height = 630.0,
      unit = ResizeUnit.PIXELS,
      dpi = 72,
      description = "Twitter / X, Facebook, LinkedIn link banner",
      aspectLabel = "1.91:1"
    ),

    // Print
    DimensionPreset(
      id = "print_4x6",
      title = "4 × 6 inch Photo",
      category = DimensionCategory.PRINT,
      width = 4.0,
      height = 6.0,
      unit = ResizeUnit.INCHES,
      dpi = 300,
      description = "1200 × 1800 px @ 300 DPI — Classic photo print",
      aspectLabel = "2:3"
    ),
    DimensionPreset(
      id = "print_5x7",
      title = "5 × 7 inch Photo",
      category = DimensionCategory.PRINT,
      width = 5.0,
      height = 7.0,
      unit = ResizeUnit.INCHES,
      dpi = 300,
      description = "1500 × 2100 px @ 300 DPI — Desk photo frame",
      aspectLabel = "5:7"
    ),
    DimensionPreset(
      id = "print_a4",
      title = "A4 Document / Photo (210 × 297 mm)",
      category = DimensionCategory.PRINT,
      width = 210.0,
      height = 297.0,
      unit = ResizeUnit.MILLIMETERS,
      dpi = 300,
      description = "2480 × 3508 px @ 300 DPI — Standard international document",
      aspectLabel = "1:1.414"
    ),
    DimensionPreset(
      id = "print_a5",
      title = "A5 Document (148 × 210 mm)",
      category = DimensionCategory.PRINT,
      width = 148.0,
      height = 210.0,
      unit = ResizeUnit.MILLIMETERS,
      dpi = 300,
      description = "1748 × 2480 px @ 300 DPI — Half-A4 booklet print",
      aspectLabel = "1:1.414"
    ),
    DimensionPreset(
      id = "print_letter",
      title = "US Letter (8.5 × 11 inch)",
      category = DimensionCategory.PRINT,
      width = 8.5,
      height = 11.0,
      unit = ResizeUnit.INCHES,
      dpi = 300,
      description = "2550 × 3300 px @ 300 DPI — Standard North American letter",
      aspectLabel = "8.5:11"
    ),

    // Documents & Portals
    DimensionPreset(
      id = "doc_1000",
      title = "1000 px Max Dimension",
      category = DimensionCategory.DOCUMENTS,
      width = 1000.0,
      height = 1000.0,
      unit = ResizeUnit.PIXELS,
      dpi = 150,
      description = "Ideal for standard job applications & web portals",
      aspectLabel = "Fit"
    ),
    DimensionPreset(
      id = "doc_1500",
      title = "1500 px Max Dimension",
      category = DimensionCategory.DOCUMENTS,
      width = 1500.0,
      height = 1500.0,
      unit = ResizeUnit.PIXELS,
      dpi = 200,
      description = "Recommended for government & banking portals",
      aspectLabel = "Fit"
    ),
    DimensionPreset(
      id = "doc_2000",
      title = "2000 px Crisp Archival",
      category = DimensionCategory.DOCUMENTS,
      width = 2000.0,
      height = 2000.0,
      unit = ResizeUnit.PIXELS,
      dpi = 300,
      description = "High clarity for certificates, contracts and stamps",
      aspectLabel = "Fit"
    ),

    // Passport
    DimensionPreset(
      id = "preset_passport_35x45",
      title = "3.5 × 4.5 cm (India / Schengen / UK)",
      category = DimensionCategory.PASSPORT,
      width = 3.5,
      height = 4.5,
      unit = ResizeUnit.CENTIMETERS,
      dpi = 300,
      description = "413 × 531 px @ 300 DPI — Standard passport photo",
      aspectLabel = "7:9"
    ),
    DimensionPreset(
      id = "preset_passport_2x2",
      title = "2 × 2 inch (US Visa / DS-160 / OCI)",
      category = DimensionCategory.PASSPORT,
      width = 2.0,
      height = 2.0,
      unit = ResizeUnit.INCHES,
      dpi = 300,
      description = "600 × 600 px @ 300 DPI — Square biometric photo",
      aspectLabel = "1:1"
    )
  )

  val DPI_PRESETS = listOf(72, 96, 150, 200, 300, 600)
}
