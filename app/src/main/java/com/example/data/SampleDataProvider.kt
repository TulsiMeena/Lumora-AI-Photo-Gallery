package com.example.data

import com.example.model.AiActionItem
import com.example.model.AlbumItem
import com.example.model.AlbumType
import com.example.model.DocumentFormat
import com.example.model.DocumentItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.VaultFolderItem

object SampleDataProvider {

  private val now = System.currentTimeMillis()
  private const val DAY = 24 * 60 * 60 * 1000L

  val samplePhotos: List<MediaItem> = listOf(
    MediaItem(
      id = "m1",
      title = "Hawa Mahal Sunset",
      mediaType = MediaType.PHOTO,
      dateTaken = "Today, 5:42 PM",
      dateTakenMillis = now - 3600_000L,
      sizeBytes = 4823450,
      width = 4032,
      height = 3024,
      isFavorite = true,
      albumName = "Camera",
      resolution = "4032 x 3024 • 12.2 MP",
      cameraModel = "Pixel 8 Pro • 24mm ƒ/1.68",
      iso = "ISO 45",
      shutterSpeed = "1/320s",
      locationName = "Jaipur, India",
      categoryTag = "Architecture",
      colorSeed = 1
    ),
    MediaItem(
      id = "m2",
      title = "Misty Mountain Valley",
      mediaType = MediaType.PHOTO,
      dateTaken = "Today, 2:15 PM",
      dateTakenMillis = now - 14400_000L,
      sizeBytes = 6124500,
      width = 3840,
      height = 2160,
      isFavorite = true,
      albumName = "Camera",
      resolution = "3840 x 2160 • 8.3 MP",
      cameraModel = "Pixel 8 Pro • 50mm ƒ/2.0",
      iso = "ISO 64",
      shutterSpeed = "1/500s",
      locationName = "Manali, Himachal",
      categoryTag = "Nature",
      colorSeed = 2
    ),
    MediaItem(
      id = "m3",
      title = "Cinematic Forest Trail",
      mediaType = MediaType.VIDEO,
      durationSeconds = 48,
      dateTaken = "Yesterday, 6:10 PM",
      dateTakenMillis = now - DAY,
      sizeBytes = 38421000,
      width = 3840,
      height = 2160,
      isFavorite = false,
      albumName = "Videos",
      resolution = "4K UHD 60fps",
      cameraModel = "Pixel 8 Pro • 4K 10-bit HDR",
      iso = "ISO 125",
      shutterSpeed = "1/120s",
      locationName = "Shimla Ridge",
      categoryTag = "Landscape",
      colorSeed = 3
    ),
    MediaItem(
      id = "m4",
      title = "Portrait in Red Shirt",
      mediaType = MediaType.PHOTO,
      dateTaken = "Yesterday, 11:30 AM",
      dateTakenMillis = now - (DAY + 7200_000L),
      sizeBytes = 5421000,
      width = 4000,
      height = 3000,
      isFavorite = true,
      albumName = "Camera",
      resolution = "4000 x 3000 • 12 MP",
      cameraModel = "Pixel 8 Pro • Portrait Mode 2x",
      iso = "ISO 80",
      shutterSpeed = "1/200s",
      locationName = "Amit's Studio",
      categoryTag = "Portrait",
      colorSeed = 4,
      dominantColor = "Red",
      semanticTags = listOf("red", "shirt", "red shirt", "portrait", "person", "studio", "लाल", "शर्ट"),
      detectedObjects = listOf("Person", "Red Shirt", "Studio Light")
    ),
    MediaItem(
      id = "m5",
      title = "Golden Dunes at Twilight",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 15, 2026",
      dateTakenMillis = now - (3 * DAY),
      sizeBytes = 7214000,
      width = 4032,
      height = 3024,
      isFavorite = false,
      albumName = "Camera",
      resolution = "4032 x 3024 • 12.2 MP",
      cameraModel = "Pixel 8 Pro • 24mm ƒ/1.68",
      iso = "ISO 50",
      shutterSpeed = "1/640s",
      locationName = "Jaisalmer, Rajasthan",
      categoryTag = "Desert",
      colorSeed = 5,
      dominantColor = "Golden",
      semanticTags = listOf("desert", "dunes", "golden", "sand", "twilight", "रेगिस्तान", "सनसेट"),
      detectedObjects = listOf("Sand Dunes", "Desert Terrain", "Warm Sunset")
    ),
    MediaItem(
      id = "m6",
      title = "Night Sky & Constellations",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 14, 2026",
      dateTakenMillis = now - (4 * DAY),
      sizeBytes = 8940000,
      width = 4032,
      height = 3024,
      isFavorite = true,
      albumName = "Camera",
      resolution = "4032 x 3024 • Astrophotography",
      cameraModel = "Pixel 8 Pro • 16s Astro Exposure",
      iso = "ISO 1600",
      shutterSpeed = "16s",
      locationName = "Spiti Valley",
      categoryTag = "Astro",
      colorSeed = 6,
      dominantColor = "Deep Slate & Indigo",
      semanticTags = listOf("night", "stars", "sky", "milky way", "astro", "आकाश", "तारे"),
      detectedObjects = listOf("Night Sky", "Stars", "Milky Way")
    ),
    MediaItem(
      id = "m7",
      title = "Raindrops on Emerald Leaf",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 12, 2026",
      dateTakenMillis = now - (5 * DAY),
      sizeBytes = 3940000,
      width = 4032,
      height = 3024,
      isFavorite = false,
      albumName = "Recently Edited",
      resolution = "4032 x 3024 • Macro Focus",
      cameraModel = "Pixel 8 Pro • Macro Ultra-wide",
      iso = "ISO 100",
      shutterSpeed = "1/400s",
      locationName = "Bengaluru",
      categoryTag = "Macro",
      colorSeed = 7,
      dominantColor = "Emerald Green",
      isEdited = true,
      semanticTags = listOf("leaf", "green", "macro", "rain", "nature", "हरा", "पत्ता"),
      detectedObjects = listOf("Leaf", "Raindrops", "Botanical Stem")
    ),
    MediaItem(
      id = "m8",
      title = "Coastal Waves Slow Motion",
      mediaType = MediaType.VIDEO,
      durationSeconds = 124,
      dateTaken = "Sep 10, 2026",
      dateTakenMillis = now - (7 * DAY),
      sizeBytes = 74120000,
      width = 1920,
      height = 1080,
      isFavorite = false,
      albumName = "Videos",
      resolution = "1080p 240fps Slow-mo",
      cameraModel = "Pixel 8 Pro • OIS Active",
      iso = "ISO 200",
      shutterSpeed = "1/480s",
      locationName = "Goa Shore",
      categoryTag = "Ocean",
      colorSeed = 8,
      dominantColor = "Blue",
      semanticTags = listOf("beach", "ocean", "waves", "sea", "goa", "समुद्र", "बीच", "पानी"),
      detectedObjects = listOf("Ocean Waves", "Coastline", "Water")
    ),
    MediaItem(
      id = "m9",
      title = "Metropolitan Skyline Screenshot",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 8, 2026",
      dateTakenMillis = now - (8 * DAY),
      sizeBytes = 5821000,
      width = 4032,
      height = 3024,
      isFavorite = false,
      albumName = "Screenshots",
      resolution = "4032 x 3024 • 12.2 MP",
      cameraModel = "Pixel 8 Pro • 5x Telephoto ƒ/2.8",
      iso = "ISO 100",
      shutterSpeed = "1/1000s",
      locationName = "Mumbai, BKC",
      categoryTag = "Screenshot",
      colorSeed = 9,
      semanticTags = listOf("screenshot", "city", "skyline", "mumbai", "स्क्रीनशॉट"),
      detectedObjects = listOf("Screenshot UI", "Cityscape", "Buildings")
    ),
    MediaItem(
      id = "m10",
      title = "Vibrant Autumn Blooms",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 5, 2026",
      dateTakenMillis = now - (9 * DAY),
      sizeBytes = 4120000,
      width = 4000,
      height = 3000,
      isFavorite = true,
      albumName = "Favorites",
      resolution = "4000 x 3000 • 12 MP",
      cameraModel = "Pixel 8 Pro • HDR+",
      iso = "ISO 50",
      shutterSpeed = "1/500s",
      locationName = "Kashmir Gardens",
      categoryTag = "Flora",
      colorSeed = 10,
      dominantColor = "Crimson Red",
      semanticTags = listOf("flower", "flowers", "autumn", "blooms", "flora", "फूल", "बगीचा"),
      detectedObjects = listOf("Flowers", "Petals", "Garden Leaves")
    ),
    MediaItem(
      id = "m11",
      title = "Artisan Coffee Pour",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 3, 2026",
      dateTakenMillis = now - (10 * DAY),
      sizeBytes = 3280000,
      width = 3840,
      height = 2160,
      isFavorite = false,
      albumName = "Downloads",
      resolution = "3840 x 2160 • 8.3 MP",
      cameraModel = "Pixel 8 Pro • Macro",
      iso = "ISO 160",
      shutterSpeed = "1/160s",
      locationName = "Delhi Cafe",
      categoryTag = "Lifestyle",
      colorSeed = 11,
      semanticTags = listOf("coffee", "cup", "cafe", "pour", "कॉफ़ी"),
      detectedObjects = listOf("Coffee Cup", "Espresso Pour", "Wooden Table")
    ),
    MediaItem(
      id = "m12",
      title = "Drone Flight Over River",
      mediaType = MediaType.VIDEO,
      durationSeconds = 76,
      dateTaken = "Aug 29, 2026",
      dateTakenMillis = now - (22 * DAY),
      sizeBytes = 52300000,
      width = 3840,
      height = 2160,
      isFavorite = true,
      albumName = "Videos",
      resolution = "4K Cinema 24fps",
      cameraModel = "Air Master • D-Log M",
      iso = "ISO 100",
      shutterSpeed = "1/50s",
      locationName = "Ganga Basin, Rishikesh",
      categoryTag = "Aerial",
      colorSeed = 12,
      semanticTags = listOf("river", "drone", "rishikesh", "video", "aerial", "नदी"),
      detectedObjects = listOf("River Water", "Mountain Ridge", "River Bank")
    ),
    MediaItem(
      id = "m13",
      title = "Aadhaar Card Front & Back",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 16, 2026",
      dateTakenMillis = now - (2 * DAY),
      sizeBytes = 2450000,
      width = 2480,
      height = 1754,
      isFavorite = true,
      albumName = "Documents",
      resolution = "2480 x 1754 • 300 DPI",
      cameraModel = "Document Scanner Mode",
      iso = "ISO 100",
      shutterSpeed = "1/250s",
      locationName = "New Delhi",
      categoryTag = "Document",
      colorSeed = 1,
      dominantColor = "White & Navy",
      semanticTags = listOf("aadhaar", "aadhaar card", "id card", "document", "आधार", "pehchan", "identity", "uidai"),
      detectedText = "Government of India • Unique Identification Authority of India (UIDAI)\nName: Amit Meena\nDOB: 15/08/1998\nGender: Male\nMera Aadhaar, Meri Pehchan",
      detectedObjects = listOf("Identity Card", "Government Document", "Emblem", "Photo ID")
    ),
    MediaItem(
      id = "m14",
      title = "Baga Beach Sunset Waves",
      mediaType = MediaType.PHOTO,
      dateTaken = "Sep 11, 2026",
      dateTakenMillis = now - (6 * DAY),
      sizeBytes = 6840000,
      width = 4032,
      height = 3024,
      isFavorite = true,
      albumName = "Camera",
      resolution = "4032 x 3024 • 12.2 MP",
      cameraModel = "Pixel 8 Pro • 24mm ƒ/1.68",
      iso = "ISO 64",
      shutterSpeed = "1/800s",
      locationName = "Goa, Baga Beach",
      categoryTag = "Beach",
      colorSeed = 5,
      dominantColor = "Golden & Amber",
      semanticTags = listOf("beach", "ocean", "sunset", "waves", "goa", "sea", "समुद्र", "बीच", "तट", "पानी"),
      detectedObjects = listOf("Beach", "Ocean Waves", "Golden Sunset", "Sand")
    )
  )

  val sampleAlbums: List<AlbumItem> = listOf(
    AlbumItem("alb_all", "All Photos", 1248, AlbumType.ALL, 1, "Complete photo library"),
    AlbumItem("alb_cam", "Camera", 842, AlbumType.CAMERA, 2, "Taken with this device"),
    AlbumItem("alb_vid", "Videos", 94, AlbumType.VIDEOS, 3, "High frame rate & 4K clips"),
    AlbumItem("alb_fav", "Favorites", 86, AlbumType.FAVORITES, 4, "Starred special memories"),
    AlbumItem("alb_scr", "Screenshots", 142, AlbumType.SCREENSHOTS, 5, "Captured screen snapshots"),
    AlbumItem("alb_rec", "Recently Edited", 38, AlbumType.RECENT_EDITED, 6, "Enhanced with AI tools"),
    AlbumItem("alb_wa", "WhatsApp", 215, AlbumType.WHATSAPP, 7, "Received chat media"),
    AlbumItem("alb_dl", "Downloads", 78, AlbumType.DOWNLOADS, 8, "Downloaded web assets"),
    AlbumItem("alb_doc", "Documents", 26, AlbumType.DOCUMENTS, 9, "Scans, receipts & cards"),
    AlbumItem("alb_c1", "Leh Ladakh Trip", 185, AlbumType.CUSTOM, 10, "Custom travel collection"),
    AlbumItem("alb_c2", "Studio Portfolio", 64, AlbumType.CUSTOM, 11, "Professional photo shoots"),
    AlbumItem("alb_c3", "Family Celebrations", 112, AlbumType.CUSTOM, 12, "Birthdays & festivals")
  )

  val sampleAiSuggestions: List<String> = listOf(
    "September की photos दिखाओ",
    "लाल shirt वाली photos दिखाओ",
    "मेरी Aadhaar वाली photo खोजो",
    "कल की photos दिखाओ",
    "इस महीने के videos दिखाओ",
    "मेरी सबसे recent photos दिखाओ",
    "इस album में कितनी photos हैं?",
    "Large size वाली photos दिखाओ",
    "मेरी screenshots दिखाओ",
    "Beach वाली photos दिखाओ"
  )

  val sampleAiActions: List<AiActionItem> = listOf(
    AiActionItem(
      id = "ai_1",
      title = "Magic Eraser Applied",
      subtitle = "Removed 3 background passers-by on 'Hawa Mahal Sunset'",
      badge = "Retouch",
      timestamp = "2 hours ago"
    ),
    AiActionItem(
      id = "ai_2",
      title = "Portrait Relighting",
      subtitle = "Added natural soft studio bounce light to portrait",
      badge = "Lighting",
      timestamp = "Yesterday"
    ),
    AiActionItem(
      id = "ai_3",
      title = "OCR Text Extraction",
      subtitle = "Extracted 42 lines of text from Electricity Bill",
      badge = "Vision OCR",
      timestamp = "Sep 16"
    ),
    AiActionItem(
      id = "ai_4",
      title = "Sky Replacement",
      subtitle = "Upgraded overcast sky to vibrant golden twilight",
      badge = "GenAI Sky",
      timestamp = "Sep 14"
    )
  )

  val sampleDocuments: List<DocumentItem> = listOf(
    DocumentItem(
      id = "doc_1",
      title = "Aadhaar Card - Front & Back",
      pageCount = 2,
      sizeBytes = 1420000,
      format = DocumentFormat.SCAN,
      dateModified = "Today, 10:15 AM",
      dateModifiedMillis = now - 3600_000L * 2,
      category = "ID Documents",
      colorSeed = 1,
      ocrText = """
        GOVERNMENT OF INDIA • UNIQUE IDENTIFICATION AUTHORITY OF INDIA
        
        Name: Amit Meena
        DOB: 15/08/1998
        Gender: Male / पुरुष
        Aadhaar No: 5418 9021 3478
        
        Address: House No. 42, Civil Lines,
        Jaipur, Rajasthan - 302006
        
        Mera Aadhaar, Meri Pehchan
        VID: 9182 3847 1928 3491
      """.trimIndent(),
      detectedDates = listOf("15/08/1998"),
      detectedAmounts = emptyList(),
      detectedIds = listOf("5418 9021 3478", "VID: 9182 3847 1928"),
      resolution = "2480 × 1754 • 300 DPI",
      width = 2480,
      height = 1754,
      pages = listOf(
        com.example.model.DocumentPageItem(
          pageId = "page_1_1",
          pageNumber = 1,
          imageUri = "sample://doc_aadhaar_front",
          rotationDegrees = 0,
          ocrText = "Government of India\nName: Amit Meena\nDOB: 15/08/1998\nGender: Male\n5418 9021 3478",
          filterApplied = "Color Scan"
        ),
        com.example.model.DocumentPageItem(
          pageId = "page_1_2",
          pageNumber = 2,
          imageUri = "sample://doc_aadhaar_back",
          rotationDegrees = 0,
          ocrText = "Address: Civil Lines, Jaipur - 302006\nUIDAI Help: 1947",
          filterApplied = "Color Scan"
        )
      )
    ),
    DocumentItem(
      id = "doc_2",
      title = "Vehicle Comprehensive Insurance 2026",
      pageCount = 4,
      sizeBytes = 3840000,
      format = DocumentFormat.PDF,
      dateModified = "Sep 15, 2026",
      dateModifiedMillis = now - (3 * DAY),
      category = "Forms",
      colorSeed = 2,
      ocrText = """
        NATIONAL GENERAL INSURANCE CO. LTD.
        POLICY CERTIFICATE & SCHEDULE
        Policy Number: VCI-2026-892184-RJ
        Period of Insurance: 16/09/2026 to 15/09/2027
        
        Insured Name: Amit Meena
        Registration No: RJ-14-CE-8921
        Make / Model: Hyundai Creta SX (O) Turbo
        IDV (Insured Declared Value): ₹14,50,000.00
        
        Premium Breakdown:
        Basic OD Premium: ₹18,450.00
        Third Party Liability: ₹7,890.00
        Zero Depreciation Cover: ₹4,200.00
        Net Premium: ₹30,540.00
        IGST (18%): ₹5,497.20
        Total Payable: ₹36,037.20
      """.trimIndent(),
      detectedDates = listOf("16/09/2026", "15/09/2027"),
      detectedAmounts = listOf("₹36,037.20", "₹14,50,000.00", "₹30,540.00"),
      detectedIds = listOf("VCI-2026-892184-RJ", "RJ-14-CE-8921"),
      resolution = "2480 × 3508 • A4",
      width = 2480,
      height = 3508,
      pages = listOf(
        com.example.model.DocumentPageItem("page_2_1", 1, "sample://doc_insurance_1", 0, "Policy Certificate: VCI-2026-892184-RJ\nTotal: ₹36,037.20", "Black & White"),
        com.example.model.DocumentPageItem("page_2_2", 2, "sample://doc_insurance_2", 0, "Terms and Conditions of OD Cover", "Grayscale"),
        com.example.model.DocumentPageItem("page_2_3", 3, "sample://doc_insurance_3", 0, "Schedule of Benefits & Deductibles", "Grayscale"),
        com.example.model.DocumentPageItem("page_2_4", 4, "sample://doc_insurance_4", 0, "Authorized Signatory and Seal", "Color Scan")
      )
    ),
    DocumentItem(
      id = "doc_3",
      title = "Apple Store Official Invoice",
      pageCount = 1,
      sizeBytes = 620000,
      format = DocumentFormat.PDF,
      dateModified = "Sep 12, 2026",
      dateModifiedMillis = now - (6 * DAY),
      category = "Receipts",
      colorSeed = 3,
      ocrText = """
        Apple India Private Limited • Tax Invoice
        Invoice No: APL-IND-2026-0912
        Date: 12/09/2026
        GSTIN: 27AABCA1234F1Z8
        
        Item Description:
        1x iPhone 16 Pro Max 256GB Natural Titanium
        Subtotal: ₹1,14,900.00
        CGST 9%: ₹10,341.00
        SGST 9%: ₹10,341.00
        Total Invoice Amount: ₹1,35,582.00
        Payment Method: UPI / HDFC Bank
        Status: PAID IN FULL
      """.trimIndent(),
      detectedDates = listOf("12/09/2026"),
      detectedAmounts = listOf("₹1,35,582.00", "₹1,14,900.00"),
      detectedIds = listOf("APL-IND-2026-0912", "27AABCA1234F1Z8"),
      resolution = "2480 × 3508 • A4",
      width = 2480,
      height = 3508,
      pages = listOf(
        com.example.model.DocumentPageItem("page_3_1", 1, "sample://doc_invoice_1", 0, "Tax Invoice Apple\nTotal: ₹1,35,582.00", "Color Scan")
      )
    ),
    DocumentItem(
      id = "doc_4",
      title = "Indian Passport Bio Page",
      pageCount = 1,
      sizeBytes = 1850000,
      format = DocumentFormat.SCAN,
      dateModified = "Sep 08, 2026",
      dateModifiedMillis = now - (10 * DAY),
      category = "ID Documents",
      colorSeed = 4,
      ocrText = """
        REPUBLIC OF INDIA • PASSPORT
        Type: P • Country Code: IND • Passport No: Z9182341
        
        Surname: MEENA
        Given Name: AMIT
        Nationality: INDIAN • Sex: M
        Date of Birth: 15/08/1998
        Place of Birth: JAIPUR, RAJASTHAN
        Place of Issue: JAIPUR
        Date of Issue: 20/01/2022
        Date of Expiry: 19/01/2032
        
        P<INDMEENA<<AMIT<<<<<<<<<<<<<<<<<<<<<<<<<<<
        Z9182341<8IND9808154M3201193<<<<<<<<<<<<<<02
      """.trimIndent(),
      detectedDates = listOf("15/08/1998", "20/01/2022", "19/01/2032"),
      detectedAmounts = emptyList(),
      detectedIds = listOf("Z9182341"),
      resolution = "2480 × 1754 • 300 DPI",
      width = 2480,
      height = 1754,
      pages = listOf(
        com.example.model.DocumentPageItem("page_4_1", 1, "sample://doc_passport_1", 0, "Republic of India Passport\nPassport No: Z9182341\nAMIT MEENA", "Auto Enhance")
      )
    ),
    DocumentItem(
      id = "doc_5",
      title = "IIT Tech Degree Certificate",
      pageCount = 1,
      sizeBytes = 2940000,
      format = DocumentFormat.PDF,
      dateModified = "Aug 24, 2026",
      dateModifiedMillis = now - (20 * DAY),
      category = "Certificates",
      colorSeed = 5,
      ocrText = """
        INDIAN INSTITUTE OF TECHNOLOGY
        This is to certify that
        AMIT MEENA
        has successfully completed the curriculum and fulfilled all academic requirements
        for the conferment of the degree of
        BACHELOR OF TECHNOLOGY
        IN COMPUTER SCIENCE & ENGINEERING
        with First Class Distinction.
        Conferred on this 24th Day of August, 2024.
        Roll No: 20BCS042 • Registration: IIT-D-2020-094
      """.trimIndent(),
      detectedDates = listOf("24/08/2024"),
      detectedAmounts = emptyList(),
      detectedIds = listOf("20BCS042", "IIT-D-2020-094"),
      resolution = "3508 × 2480 • Landscape",
      width = 3508,
      height = 2480,
      pages = listOf(
        com.example.model.DocumentPageItem("page_5_1", 1, "sample://doc_certificate_1", 0, "Degree Certificate • B.Tech CSE • Amit Meena", "Color Scan")
      )
    ),
    DocumentItem(
      id = "doc_6",
      title = "Lab Blood Panel Report",
      pageCount = 3,
      sizeBytes = 2120000,
      format = DocumentFormat.PDF,
      dateModified = "Aug 18, 2026",
      dateModifiedMillis = now - (26 * DAY),
      category = "Notes",
      colorSeed = 6,
      ocrText = """
        APOLLO DIAGNOSTICS & HEALTH CLINIC
        Comprehensive Metabolic & CBC Report
        Patient Name: Amit Meena • Age: 28 • Male
        Referred By: Dr. R. K. Sharma, MD
        Collection Date: 18/08/2026
        
        Test Results Summary:
        - Hemoglobin: 14.8 g/dL (Normal Range: 13.0 - 17.0)
        - Fasting Blood Sugar: 92 mg/dL (Normal: 70 - 100)
        - HbA1c: 5.3% (Normal: < 5.7%)
        - Total Cholesterol: 168 mg/dL (Desirable: < 200)
        - Vitamin D3: 42 ng/mL (Sufficient: 30 - 100)
        
        Doctor's Clinical Notes:
        All vital parameters within healthy baseline physiological range.
        Recommendation: Continue balanced nutrition and hydration.
      """.trimIndent(),
      detectedDates = listOf("18/08/2026"),
      detectedAmounts = emptyList(),
      detectedIds = listOf("Apollo-2026-9921"),
      resolution = "2480 × 3508 • A4",
      width = 2480,
      height = 3508,
      pages = listOf(
        com.example.model.DocumentPageItem("page_6_1", 1, "sample://doc_lab_1", 0, "CBC & Metabolic Panel • Amit Meena", "Black & White"),
        com.example.model.DocumentPageItem("page_6_2", 2, "sample://doc_lab_2", 0, "Lipid Profile & Liver Enzymes", "Grayscale"),
        com.example.model.DocumentPageItem("page_6_3", 3, "sample://doc_lab_3", 0, "Doctor Notes & Recommendations", "Black & White")
      )
    )
  )

  val sampleVaultFolders: List<VaultFolderItem> = listOf(
    VaultFolderItem("vf_1", "Private Photos", 24, "Updated yesterday", "lock"),
    VaultFolderItem("vf_2", "Identity & Passwords", 6, "Updated Sep 12", "badge"),
    VaultFolderItem("vf_3", "Bank & Tax Records", 12, "Updated Aug 30", "folder"),
    VaultFolderItem("vf_4", "Confidential Contracts", 8, "Updated Jul 15", "description")
  )
}
