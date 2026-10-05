package com.example.utils.advanced

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.model.MediaItem
import com.example.model.advanced.VideoGifExportConfig
import com.example.model.advanced.VideoTrimConfig
import com.example.utils.creative.GifEncoder
import com.example.utils.storage.SafeTempFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Mini Video Studio: Frame sampling, trimming, audio extraction, and Video-to-GIF conversion.
 */
object VideoStudioEngine {

  suspend fun extractKeyframes(
    context: Context,
    videoUri: Uri,
    frameCount: Int = 8
  ): List<Bitmap> = withContext(Dispatchers.IO) {
    val frames = mutableListOf<Bitmap>()
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, videoUri)
      val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L
      val interval = durationMs / (frameCount + 1)

      for (i in 1..frameCount) {
        val timeUs = (i * interval) * 1000L
        val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        if (frame != null) {
          // Scale down for preview strip
          val scaled = Bitmap.createScaledBitmap(frame, 160, 120, true)
          frames.add(scaled)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      try { retriever.release() } catch (_: Exception) {}
    }
    frames
  }

  suspend fun convertVideoToGif(
    context: Context,
    videoUri: Uri,
    config: VideoGifExportConfig = VideoGifExportConfig(),
    onProgress: (Float) -> Unit = {}
  ): File? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, videoUri)
      val frames = mutableListOf<Bitmap>()
      val frameStepMs = 1000L / config.frameRate
      val totalFrames = (config.durationMs / frameStepMs).toInt().coerceIn(6, 40)

      for (i in 0 until totalFrames) {
        val timeUs = (config.startMs + (i * frameStepMs)) * 1000L
        val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        if (frame != null) {
          val aspect = frame.height.toFloat() / frame.width.toFloat()
          val targetHeight = (config.widthPx * aspect).toInt()
          val scaled = Bitmap.createScaledBitmap(frame, config.widthPx, targetHeight, true)
          frames.add(scaled)
        }
        onProgress((i + 1).toFloat() / totalFrames * 0.5f)
      }

      if (frames.isEmpty()) return@withContext null

      val outputFile = SafeTempFileManager.createTempFile(context, prefix = "video_gif", extension = ".gif")
      val delayMs = (1000 / config.frameRate).coerceAtLeast(20)
      val gifConfig = com.example.model.creative.GifConfig(
        frameDelayMs = delayMs,
        maxDimension = config.widthPx,
        loopCount = 0
      )
      FileOutputStream(outputFile).use { fos ->
        GifEncoder.encodeGifStream(frames, gifConfig, fos)
        fos.flush()
      }
      onProgress(1.0f)

      frames.forEach { it.recycle() }
      outputFile
    } catch (e: Exception) {
      e.printStackTrace()
      null
    } finally {
      try { retriever.release() } catch (_: Exception) {}
    }
  }
}
