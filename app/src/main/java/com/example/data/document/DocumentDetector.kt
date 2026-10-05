package com.example.data.document

import com.example.model.DocumentCategories
import com.example.model.MediaItem
import com.example.model.MediaType
import java.util.Locale

data class DocumentDetectionResult(
  val isDocument: Boolean,
  val detectedCategory: String,
  val confidence: Float,
  val suggestedTitle: String,
  val reasoning: String
)

object DocumentDetector {

  private val NON_DOCUMENT_CATEGORIES = setOf(
    "portrait", "landscape", "nature", "flora", "astro", "night", "beach", "ocean",
    "aerial", "drone", "lifestyle", "food", "animal", "pet", "sunset"
  )

  fun detectFromMedia(media: MediaItem): DocumentDetectionResult {
    // Videos are never documents
    if (media.mediaType == MediaType.VIDEO) {
      return DocumentDetectionResult(
        isDocument = false,
        detectedCategory = DocumentCategories.OTHER,
        confidence = 0f,
        suggestedTitle = media.title,
        reasoning = "Video file"
      )
    }

    val lowerTitle = media.title.lowercase(Locale.getDefault())
    val lowerAlbum = media.albumName.lowercase(Locale.getDefault())
    val lowerCategory = media.categoryTag.lowercase(Locale.getDefault())
    val lowerText = media.detectedText.lowercase(Locale.getDefault())
    val allTags = (media.semanticTags + media.detectedObjects).map { it.lowercase(Locale.getDefault()) }

    // Check false positive guard: If marked as general photography without text
    val isExplicitPhoto = NON_DOCUMENT_CATEGORIES.contains(lowerCategory)
    val hasDenseText = media.detectedText.length > 30 || lowerText.contains("name:") || lowerText.contains("total") || lowerText.contains("tax")

    if (isExplicitPhoto && !hasDenseText && !lowerTitle.contains("doc") && !lowerAlbum.contains("doc")) {
      return DocumentDetectionResult(
        isDocument = false,
        detectedCategory = DocumentCategories.OTHER,
        confidence = 0.05f,
        suggestedTitle = media.title,
        reasoning = "Natural scene or portrait without document typography"
      )
    }

    // 1. ID Documents (Aadhaar, PAN, Passport, Driving License, Voter ID, Identity)
    val isIdDoc = lowerTitle.contains("aadhaar") || lowerTitle.contains("passport") ||
      lowerTitle.contains("pan card") || lowerTitle.contains("id card") || lowerTitle.contains("identity") ||
      lowerTitle.contains("license") || lowerTitle.contains("voter") ||
      lowerText.contains("unique identification authority") || lowerText.contains("aadhaar") ||
      lowerText.contains("republic of india") || lowerText.contains("passport") ||
      allTags.any { it.contains("aadhaar") || it.contains("id card") || it.contains("identity") }

    if (isIdDoc) {
      val title = when {
        lowerTitle.contains("aadhaar") || lowerText.contains("aadhaar") -> "Aadhaar Card Scan"
        lowerTitle.contains("passport") || lowerText.contains("passport") -> "Passport Document"
        lowerTitle.contains("pan") -> "PAN Card Scan"
        else -> "Government ID Card"
      }
      return DocumentDetectionResult(
        isDocument = true,
        detectedCategory = DocumentCategories.ID_DOCUMENTS,
        confidence = 0.96f,
        suggestedTitle = title,
        reasoning = "High confidence government identity document with photo/emblem"
      )
    }

    // 2. Receipts & Invoices
    val isReceipt = lowerTitle.contains("invoice") || lowerTitle.contains("receipt") ||
      lowerTitle.contains("bill") || lowerTitle.contains("tax") ||
      lowerText.contains("tax invoice") || lowerText.contains("subtotal") ||
      lowerText.contains("gst") || lowerText.contains("total amount") || lowerText.contains("paid via") ||
      allTags.any { it.contains("receipt") || it.contains("invoice") || it.contains("bill") }

    if (isReceipt) {
      val title = when {
        lowerTitle.contains("apple") || lowerText.contains("apple") -> "Apple Store Invoice"
        lowerTitle.contains("invoice") || lowerText.contains("invoice") -> "Purchase Tax Invoice"
        else -> "Payment Receipt"
      }
      return DocumentDetectionResult(
        isDocument = true,
        detectedCategory = DocumentCategories.RECEIPTS,
        confidence = 0.94f,
        suggestedTitle = title,
        reasoning = "Financial invoice layout with line items, tax numbers and totals"
      )
    }

    // 3. Certificates
    val isCertificate = lowerTitle.contains("cert") || lowerTitle.contains("degree") ||
      lowerTitle.contains("diploma") || lowerTitle.contains("award") ||
      lowerText.contains("this is to certify") || lowerText.contains("degree of") ||
      allTags.any { it.contains("certificate") || it.contains("award") }

    if (isCertificate) {
      return DocumentDetectionResult(
        isDocument = true,
        detectedCategory = DocumentCategories.CERTIFICATES,
        confidence = 0.92f,
        suggestedTitle = "Academic / Honor Certificate",
        reasoning = "Formal ornate border and certification typography"
      )
    }

    // 4. Forms & Applications
    val isForm = lowerTitle.contains("form") || lowerTitle.contains("application") ||
      lowerTitle.contains("insurance") || lowerTitle.contains("policy") ||
      lowerText.contains("policy number") || lowerText.contains("registration no") ||
      lowerText.contains("terms and conditions") || lowerText.contains("applicant")

    if (isForm) {
      return DocumentDetectionResult(
        isDocument = true,
        detectedCategory = DocumentCategories.FORMS,
        confidence = 0.89f,
        suggestedTitle = "Official Policy / Form",
        reasoning = "Structured form fields, policy schedules and tabular layouts"
      )
    }

    // 5. Notes & Medical / Written reports
    val isNotes = lowerTitle.contains("note") || lowerTitle.contains("memo") ||
      lowerTitle.contains("report") || lowerTitle.contains("lab") || lowerTitle.contains("clinic") ||
      lowerText.contains("clinical notes") || lowerText.contains("patient name") ||
      allTags.any { it.contains("notes") || it.contains("memo") }

    if (isNotes) {
      return DocumentDetectionResult(
        isDocument = true,
        detectedCategory = DocumentCategories.NOTES,
        confidence = 0.88f,
        suggestedTitle = "Diagnostic & Clinical Notes",
        reasoning = "Dense clinical/diagnostic text and observations"
      )
    }

    // 6. Generic Scanned documents or screenshots of documents
    val isGenericDoc = lowerAlbum.contains("doc") || lowerCategory == "document" ||
      lowerTitle.contains("doc") || lowerTitle.contains("scan") ||
      (lowerCategory == "screenshot" && hasDenseText)

    if (isGenericDoc) {
      return DocumentDetectionResult(
        isDocument = true,
        detectedCategory = DocumentCategories.SCANNED,
        confidence = 0.82f,
        suggestedTitle = "Scanned Page",
        reasoning = "Printed text on paper canvas detected"
      )
    }

    // Default: Not a document
    return DocumentDetectionResult(
      isDocument = false,
      detectedCategory = DocumentCategories.OTHER,
      confidence = 0.1f,
      suggestedTitle = media.title,
      reasoning = "General photographic media without high-density document content"
    )
  }
}
