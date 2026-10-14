package com.example.data.ai

import com.example.model.DateRangeFilter
import com.example.model.MediaTypeFilter
import com.example.model.OrientationFilter
import com.example.model.SortOption
import java.util.Calendar
import java.util.Locale

object LocalAiQueryParser {

  private val MONTH_MAP = mapOf(
    "january" to Calendar.JANUARY, "jan" to Calendar.JANUARY, "जनवरी" to Calendar.JANUARY,
    "february" to Calendar.FEBRUARY, "feb" to Calendar.FEBRUARY, "फ़रवरी" to Calendar.FEBRUARY, "फरवरी" to Calendar.FEBRUARY,
    "march" to Calendar.MARCH, "mar" to Calendar.MARCH, "मार्च" to Calendar.MARCH,
    "april" to Calendar.APRIL, "apr" to Calendar.APRIL, "अप्रैल" to Calendar.APRIL,
    "may" to Calendar.MAY, "मई" to Calendar.MAY,
    "june" to Calendar.JUNE, "jun" to Calendar.JUNE, "जून" to Calendar.JUNE,
    "july" to Calendar.JULY, "jul" to Calendar.JULY, "जुलाई" to Calendar.JULY,
    "august" to Calendar.AUGUST, "aug" to Calendar.AUGUST, "अगस्त" to Calendar.AUGUST,
    "september" to Calendar.SEPTEMBER, "sep" to Calendar.SEPTEMBER, "सितंबर" to Calendar.SEPTEMBER, "सितम्बर" to Calendar.SEPTEMBER,
    "october" to Calendar.OCTOBER, "oct" to Calendar.OCTOBER, "अक्टूबर" to Calendar.OCTOBER,
    "november" to Calendar.NOVEMBER, "nov" to Calendar.NOVEMBER, "नवंबर" to Calendar.NOVEMBER, "नवम्बर" to Calendar.NOVEMBER,
    "december" to Calendar.DECEMBER, "dec" to Calendar.DECEMBER, "दिसंबर" to Calendar.DECEMBER, "दिसम्बर" to Calendar.DECEMBER
  )

  private val MONTH_DISPLAY = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  fun parse(query: String, hasAttachedImage: Boolean = false): ParsedAiQuery {
    val raw = query.trim()
    val lower = raw.lowercase(Locale.getDefault())

    // 1. Check if this is an image inspection question
    val isImageQuestion = hasAttachedImage || (
      lower.contains("इस photo") ||
      lower.contains("इस फोटो") ||
      lower.contains("इस image") ||
      lower.contains("इसमें क्या") ||
      lower.contains("photo में क्या") ||
      lower.contains("explain this image") ||
      lower.contains("what is in this photo") ||
      lower.contains("summary") ||
      lower.contains("objects") ||
      lower.contains("क्या लिखा है")
    )

    // 2. Check if this is a count question (e.g. "इस album में कितनी photos हैं?", "how many photos")
    val isCountQuery = lower.contains("कितनी") ||
      lower.contains("कितने") ||
      lower.contains("how many") ||
      lower.contains("count") ||
      lower.contains("total photos")

    // 3. Media Type
    val mediaType = when {
      lower.contains("video") || lower.contains("videos") || lower.contains("वीडियो") || lower.contains("विडियो") ->
        MediaTypeFilter.VIDEOS
      lower.contains("photo") || lower.contains("photos") || lower.contains("तस्वीर") || lower.contains("तस्वीरें") || lower.contains("फोटो") || lower.contains("चित्र") ->
        MediaTypeFilter.PHOTOS
      else -> MediaTypeFilter.ALL
    }

    // 4. Date ranges & Specific month/year
    var dateRange = DateRangeFilter.ALL
    var specificMonth: Int? = null
    var specificMonthName: String? = null
    var specificYear: Int? = null

    when {
      lower.contains("today") || lower.contains("आज") -> dateRange = DateRangeFilter.TODAY
      lower.contains("yesterday") || lower.contains("कल") -> dateRange = DateRangeFilter.YESTERDAY
      lower.contains("this week") || lower.contains("इस हफ्ते") || lower.contains("इस सप्ताह") || lower.contains("last week") || lower.contains("पिछले हफ्ते") ->
        dateRange = DateRangeFilter.THIS_WEEK
      lower.contains("this month") || lower.contains("इस महीने") || lower.contains("इस माह") ->
        dateRange = DateRangeFilter.THIS_MONTH
      lower.contains("this year") || lower.contains("इस साल") || lower.contains("इस वर्ष") ->
        dateRange = DateRangeFilter.THIS_YEAR
    }

    // Check specific month
    for ((monthWord, monthIndex) in MONTH_MAP) {
      if (lower.contains(monthWord)) {
        specificMonth = monthIndex
        specificMonthName = MONTH_DISPLAY[monthIndex]
        break
      }
    }

    // Check specific 4-digit year (e.g., 2024, 2025, 2026)
    val yearRegex = Regex("""\b(202[0-9])\b""")
    val yearMatch = yearRegex.find(lower)
    if (yearMatch != null) {
      specificYear = yearMatch.groupValues[1].toIntOrNull()
    }

    // 5. Attributes: Favorites, Edited, Screenshots, Documents, File sizes, Orientation
    val onlyFavorites = lower.contains("favorite") ||
      lower.contains("favorites") ||
      lower.contains("star") ||
      lower.contains("starred") ||
      lower.contains("पसंदीदा") ||
      lower.contains("फेवरेट")

    val onlyEdited = lower.contains("edit") ||
      lower.contains("edited") ||
      lower.contains("retouch") ||
      lower.contains("एडिट") ||
      lower.contains("संपादित")

    val onlyScreenshots = lower.contains("screenshot") ||
      lower.contains("screenshots") ||
      lower.contains("स्क्रीनशॉट")

    val onlyDocuments = lower.contains("document") ||
      lower.contains("documents") ||
      lower.contains("दस्तावेज़") ||
      lower.contains("aadhaar") ||
      lower.contains("आधार") ||
      lower.contains("receipt") ||
      lower.contains("receipts") ||
      lower.contains("bill") ||
      lower.contains("invoice") ||
      lower.contains("pdf") ||
      lower.contains("passport") ||
      lower.contains("पासपोर्ट")

    val onlyLargeFiles = lower.contains("large") ||
      lower.contains("big") ||
      lower.contains("heavy") ||
      lower.contains("बड़ा") ||
      lower.contains("बड़ी") ||
      lower.contains("5mb") ||
      lower.contains("10mb") ||
      lower.contains("large size")

    val onlySmallFiles = lower.contains("small") ||
      lower.contains("small size") ||
      lower.contains("छोटा") ||
      lower.contains("छोटी") ||
      lower.contains("light")

    val orientation = when {
      lower.contains("portrait") || lower.contains("पोर्ट्रेट") -> OrientationFilter.PORTRAIT
      lower.contains("landscape") || lower.contains("लैंडस्केप") -> OrientationFilter.LANDSCAPE
      else -> OrientationFilter.ALL
    }

    // 6. Target Album or Folder
    val targetAlbum = when {
      lower.contains("camera") || lower.contains("कैमरा") -> "Camera"
      lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> "WhatsApp"
      lower.contains("download") || lower.contains("downloads") || lower.contains("डाउनलोड") -> "Downloads"
      lower.contains("screenshot") || lower.contains("स्क्रीनशॉट") -> "Screenshots"
      lower.contains("portfolio") || lower.contains("पोर्टफोलियो") -> "Studio Portfolio"
      lower.contains("ladakh") || lower.contains("लद्दाख") -> "Leh Ladakh Trip"
      lower.contains("family") || lower.contains("परिवार") -> "Family Celebrations"
      else -> null
    }

    // 7. Sort options
    var sortOption: SortOption? = null
    if (lower.contains("recent") || lower.contains("newest") || lower.contains("हाल ही की") || lower.contains("सबसे recent") || lower.contains("ताज़ा")) {
      sortOption = SortOption.NEWEST_FIRST
    } else if (lower.contains("oldest") || lower.contains("purani") || lower.contains("पुरानी")) {
      sortOption = SortOption.OLDEST_FIRST
    } else if (onlyLargeFiles) {
      sortOption = SortOption.LARGEST_FILE
    }

    // 8. Semantic keywords, primary subjects, and colors
    val semanticKeywords = mutableListOf<String>()
    var primarySubject: String? = null
    var primaryColor: String? = null
    var exactCompoundPhrase: String? = null

    // Colors
    when {
      lower.contains("red") || lower.contains("लाल") -> {
        primaryColor = "red"
        semanticKeywords.add("red")
      }
      lower.contains("blue") || lower.contains("नीला") || lower.contains("नीली") -> {
        primaryColor = "blue"
        semanticKeywords.add("blue")
      }
      lower.contains("green") || lower.contains("हरा") || lower.contains("हरी") -> {
        primaryColor = "green"
        semanticKeywords.add("green")
      }
      lower.contains("golden") || lower.contains("सुनहरा") || lower.contains("सुनहरी") -> {
        primaryColor = "golden"
        semanticKeywords.add("golden")
      }
      lower.contains("black") || lower.contains("काला") || lower.contains("काली") -> {
        primaryColor = "black"
        semanticKeywords.add("black")
      }
      lower.contains("white") || lower.contains("सफ़ेद") || lower.contains("सफेद") -> {
        primaryColor = "white"
        semanticKeywords.add("white")
      }
    }

    // Objects & Apparel
    val isApparel = lower.contains("shirt") || lower.contains("tshirt") || lower.contains("t-shirt") ||
      lower.contains("t shirt") || lower.contains("tee") || lower.contains("शर्ट") || lower.contains("t-shirts") ||
      lower.contains("kurta") || lower.contains("dress") || lower.contains("कपड़े")

    if (isApparel) {
      primarySubject = "shirt"
      semanticKeywords.add("shirt")
      if (lower.contains("tshirt") || lower.contains("t-shirt") || lower.contains("t shirt") || lower.contains("tee")) {
        semanticKeywords.add("tshirt")
      }
    } else if (lower.contains("car") || lower.contains("गाड़ी")) {
      primarySubject = "car"
      semanticKeywords.add("car")
    } else if (lower.contains("coffee") || lower.contains("कॉफ़ी") || lower.contains("chai") || lower.contains("cup")) {
      primarySubject = "coffee"
      semanticKeywords.add("coffee")
    } else if (lower.contains("flower") || lower.contains("flowers") || lower.contains("फूल") || lower.contains("blooms")) {
      primarySubject = "flower"
      semanticKeywords.add("flower")
    } else if (lower.contains("beach") || lower.contains("समुद्र") || lower.contains("बीच") || lower.contains("coast")) {
      primarySubject = "beach"
      semanticKeywords.add("beach")
    } else if (lower.contains("mountain") || lower.contains("पहाड़") || lower.contains("valley") || lower.contains("hills")) {
      primarySubject = "mountain"
      semanticKeywords.add("mountain")
    } else if (lower.contains("sunset") || lower.contains("सनसेट") || lower.contains("सूर्यास्त")) {
      primarySubject = "sunset"
      semanticKeywords.add("sunset")
    } else if (lower.contains("night") || lower.contains("star") || lower.contains("stars") || lower.contains("आकाश")) {
      primarySubject = "night"
      semanticKeywords.add("night")
    } else if (lower.contains("forest") || lower.contains("जंगल") || lower.contains("trees")) {
      primarySubject = "forest"
      semanticKeywords.add("forest")
    } else if (lower.contains("desert") || lower.contains("रेगिस्तान") || lower.contains("dunes") || lower.contains("sand")) {
      primarySubject = "desert"
      semanticKeywords.add("desert")
    } else if (lower.contains("leaf") || lower.contains("leaves") || lower.contains("पत्ता") || lower.contains("macro")) {
      primarySubject = "leaf"
      semanticKeywords.add("leaf")
    } else if (lower.contains("river") || lower.contains("नदी")) {
      primarySubject = "river"
      semanticKeywords.add("river")
    }

    // Specific identities & documents
    if (lower.contains("aadhaar") || lower.contains("आधार")) {
      primarySubject = "aadhaar"
      exactCompoundPhrase = "aadhaar card"
      semanticKeywords.add("aadhaar")
    }
    if (lower.contains("passport") || lower.contains("पासपोर्ट")) {
      primarySubject = "passport"
      semanticKeywords.add("passport")
    }
    if (lower.contains("hawa mahal") || lower.contains("हवा महल")) {
      primarySubject = "hawa mahal"
      exactCompoundPhrase = "hawa mahal"
      semanticKeywords.add("hawa mahal")
    }
    if (lower.contains("studio") || lower.contains("स्टूडियो")) {
      semanticKeywords.add("studio")
    }

    // Check specific compound match: Red Shirt / Red T-shirt
    if ((primaryColor == "red") && isApparel) {
      exactCompoundPhrase = "red shirt"
    }

    // Add general words if no specific semantic keyword recognized yet
    if (semanticKeywords.isEmpty() && !isCountQuery && !isImageQuestion) {
      val ignored = setOf(
        "show", "me", "find", "photos", "photo", "videos", "video", "my", "all", "the", "in", "of", "from",
        "दिखाओ", "दिखाइए", "खोजो", "ढूंढो", "मेरी", "मेरे", "की", "के", "वाली", "वाले", "का", "हैं", "है",
        "tell", "how", "many", "give", "list", "bala", "wali", "wala", "wale", "bali", "bho", "woh", "wo",
        "do", "chahiye", "search", "batao", "colour", "color", "rang", "wali", "photo do"
      )
      val tokens = lower.split(Regex("""[\s,?.!]+""")).filter { it.isNotBlank() && !ignored.contains(it) }
      semanticKeywords.addAll(tokens.take(3))
    }

    return ParsedAiQuery(
      originalQuery = raw,
      mediaType = mediaType,
      dateRange = dateRange,
      specificMonth = specificMonth,
      specificMonthName = specificMonthName,
      specificYear = specificYear,
      onlyFavorites = onlyFavorites,
      onlyEdited = onlyEdited,
      onlyScreenshots = onlyScreenshots,
      onlyDocuments = onlyDocuments,
      onlyLargeFiles = onlyLargeFiles,
      onlySmallFiles = onlySmallFiles,
      orientation = orientation,
      targetAlbum = targetAlbum,
      semanticKeywords = semanticKeywords,
      primarySubject = primarySubject,
      primaryColor = primaryColor,
      exactCompoundPhrase = exactCompoundPhrase,
      sortOption = sortOption,
      isCountQuery = isCountQuery,
      isImageQuestion = isImageQuestion
    )
  }
}
