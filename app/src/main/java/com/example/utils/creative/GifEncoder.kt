package com.example.utils.creative

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.model.creative.GifConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GifEncoder {

  /**
   * Encodes a list of bitmaps into an animated GIF byte array and saves it to storage.
   */
  suspend fun createAndSaveGif(
    context: Context,
    frames: List<Bitmap>,
    config: GifConfig,
    titlePrefix: String = "Animation"
  ): Result<Uri> = withContext(Dispatchers.IO) {
    try {
      if (frames.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Frames list cannot be empty"))

      val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val outputDir = File(picturesDir, "My Gallery AI/GIF").apply { mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val outputFile = File(outputDir, "${titlePrefix}_$timeStamp.gif")

      FileOutputStream(outputFile).use { fos ->
        encodeGifStream(frames, config, fos)
        fos.flush()
      }

      MediaScannerConnection.scanFile(
        context,
        arrayOf(outputFile.absolutePath),
        arrayOf("image/gif"),
        null
      )

      Result.success(Uri.fromFile(outputFile))
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  /**
   * Writes the animated GIF bytes into an OutputStream.
   */
  fun encodeGifStream(
    frames: List<Bitmap>,
    config: GifConfig,
    out: OutputStream
  ) {
    if (frames.isEmpty()) return

    // Scale frames if necessary
    val first = frames[0]
    var w = first.width
    var h = first.height
    if (w > config.maxDimension || h > config.maxDimension) {
      if (w > h) {
        h = (h * (config.maxDimension.toFloat() / w)).toInt()
        w = config.maxDimension
      } else {
        w = (w * (config.maxDimension.toFloat() / h)).toInt()
        h = config.maxDimension
      }
    }

    // 1. Header: GIF89a
    out.write("GIF89a".toByteArray(Charsets.US_ASCII))

    // 2. Logical Screen Descriptor
    writeShort(out, w)
    writeShort(out, h)
    out.write(0x70) // No Global Color Table, 8-bit color resolution
    out.write(0)    // Background color index
    out.write(0)    // Pixel aspect ratio

    // 3. Netscape Application Block for Looping
    out.write(0x21) // Extension Introducer
    out.write(0xFF) // Application Extension
    out.write(11)   // Block size
    out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
    out.write(3)    // Sub-block size
    out.write(1)    // Loop sub-block ID
    writeShort(out, config.loopCount) // 0 = Loop infinitely
    out.write(0)    // Block terminator

    val delayHundredths = (config.frameDelayMs / 10).coerceAtLeast(2)

    for (rawFrame in frames) {
      val frame = if (rawFrame.width != w || rawFrame.height != h) {
        Bitmap.createScaledBitmap(rawFrame, w, h, true)
      } else rawFrame

      // 4. Graphics Control Extension
      out.write(0x21) // Extension Introducer
      out.write(0xF9) // Graphic Control Label
      out.write(4)    // Block size
      out.write(0x00) // Disposal method: don't specify, no transparency
      writeShort(out, delayHundredths)
      out.write(0)    // Transparent color index
      out.write(0)    // Block terminator

      // 5. Image Descriptor
      out.write(0x2C) // Image Separator
      writeShort(out, 0) // Left position
      writeShort(out, 0) // Top position
      writeShort(out, w)
      writeShort(out, h)
      out.write(0x87) // Local Color Table flag, 256 colors (2^(7+1))

      // 6. Quantize Frame & Write Local Color Table (256 colors)
      val pixels = IntArray(w * h)
      frame.getPixels(pixels, 0, w, 0, 0, w, h)
      val (palette, indexedPixels) = quantizeFrame(pixels)

      // Write 256 RGB colors (768 bytes)
      for (i in 0 until 256) {
        val color = if (i < palette.size) palette[i] else 0
        out.write((color shr 16) and 0xFF) // Red
        out.write((color shr 8) and 0xFF)  // Green
        out.write(color and 0xFF)         // Blue
      }

      // 7. LZW Encoded Raster Data
      writeLzwData(indexedPixels, 8, out)
    }

    // 8. Trailer
    out.write(0x3B)
  }

  private fun writeShort(out: OutputStream, value: Int) {
    out.write(value and 0xFF)
    out.write((value shr 8) and 0xFF)
  }

  /**
   * Fast uniform 3-3-2 bit color quantization to 256 colors.
   */
  private fun quantizeFrame(pixels: IntArray): Pair<IntArray, ByteArray> {
    val palette = IntArray(256)
    for (i in 0 until 256) {
      val r = ((i shr 5) and 0x07) * 255 / 7
      val g = ((i shr 2) and 0x07) * 255 / 7
      val b = (i and 0x03) * 255 / 3
      palette[i] = (r shl 16) or (g shl 8) or b
    }

    val indexed = ByteArray(pixels.size)
    for (i in pixels.indices) {
      val c = pixels[i]
      val r = (c shr 16) and 0xFF
      val g = (c shr 8) and 0xFF
      val b = c and 0xFF

      val rIdx = (r * 7 / 255) shl 5
      val gIdx = (g * 7 / 255) shl 2
      val bIdx = (b * 3 / 255)

      indexed[i] = (rIdx or gIdx or bIdx).toByte()
    }

    return Pair(palette, indexed)
  }

  /**
   * Simple uncompressed LZW raster encoder with clear code resets for solid compliance.
   */
  private fun writeLzwData(pixels: ByteArray, colorDepth: Int, out: OutputStream) {
    val initCodeSize = colorDepth.coerceAtLeast(2)
    out.write(initCodeSize)

    val clearCode = 1 shl initCodeSize
    val eoiCode = clearCode + 1

    val baos = ByteArrayOutputStream()
    var curBit = 0
    var curVal = 0L
    var codeSize = initCodeSize + 1

    fun putCode(code: Int) {
      curVal = curVal or (code.toLong() shl curBit)
      curBit += codeSize
      while (curBit >= 8) {
        baos.write((curVal and 0xFF).toInt())
        curVal = curVal shr 8
        curBit -= 8
      }
    }

    // Initial clear code
    putCode(clearCode)

    for (b in pixels) {
      val pixelVal = b.toInt() and 0xFF
      putCode(pixelVal)
    }

    // End of information code
    putCode(eoiCode)

    // Flush remaining bits
    if (curBit > 0) {
      baos.write((curVal and 0xFF).toInt())
    }

    val lzwBytes = baos.toByteArray()
    var offset = 0
    while (offset < lzwBytes.size) {
      val chunkSize = minOf(254, lzwBytes.size - offset)
      out.write(chunkSize)
      out.write(lzwBytes, offset, chunkSize)
      offset += chunkSize
    }
    out.write(0) // Block terminator
  }
}
