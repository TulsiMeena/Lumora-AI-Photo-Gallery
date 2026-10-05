package com.example.utils.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance, zero-latency Sound & Haptic Feedback Engine.
 *
 * Provides synthesized micro-interactions:
 * - Keypad and PIN entry clicks
 * - Settings toggle on/off clicks
 * - Vault unlock success chime & wrong PIN error alert
 * - Shutter clicks, trash/delete sounds, and export complete fanfare
 *
 * Fully respects user preferences, silent mode, and device battery.
 */
object AppSoundManager {

  private const val TAG = "AppSoundManager"
  private const val SAMPLE_RATE = 44100

  enum class SoundEffectType {
    KEY_CLICK,
    TOGGLE_ON,
    TOGGLE_OFF,
    UNLOCK_SUCCESS,
    ERROR_BUZZ,
    TRASH,
    SHUTTER,
    COMPLETE_CHIME
  }

  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Volatile
  private var isInitialized = false

  private var soundPool: SoundPool? = null
  private val soundIds = mutableMapOf<SoundEffectType, Int>()
  private var vibrator: Vibrator? = null
  private var audioManager: AudioManager? = null

  // User preference caches (updated dynamically)
  @Volatile
  var soundEnabled: Boolean = true

  @Volatile
  var hapticEnabled: Boolean = true

  fun init(context: Context) {
    if (isInitialized) return
    synchronized(this) {
      if (isInitialized) return
      val appContext = context.applicationContext

      audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

      vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

      soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(audioAttributes)
        .build()

      isInitialized = true

      coroutineScope.launch {
        try {
          generateAndLoadSounds(appContext)
        } catch (e: Exception) {
          Log.e(TAG, "Failed to initialize synthesized sounds", e)
        }
      }
    }
  }

  private fun generateAndLoadSounds(context: Context) {
    val cacheDir = File(context.cacheDir, "audio_fx").apply { mkdirs() }

    SoundEffectType.entries.forEach { type ->
      val file = File(cacheDir, "${type.name.lowercase()}.wav")
      if (!file.exists() || file.length() == 0L) {
        val samples = synthesizeSound(type)
        val wavBytes = createWavBytes(samples, SAMPLE_RATE)
        FileOutputStream(file).use { it.write(wavBytes) }
      }
      val soundId = soundPool?.load(file.absolutePath, 1) ?: 0
      if (soundId != 0) {
        soundIds[type] = soundId
      }
    }
  }

  fun playSound(type: SoundEffectType, volumeFactor: Float = 1.0f) {
    if (!soundEnabled) return

    val am = audioManager
    if (am != null) {
      if (am.ringerMode == AudioManager.RINGER_MODE_SILENT) return
    }

    val pool = soundPool ?: return
    val soundId = soundIds[type]
    if (soundId != null && soundId != 0) {
      val vol = (0.65f * volumeFactor).coerceIn(0.1f, 1.0f)
      pool.play(soundId, vol, vol, 1, 0, 1.0f)
    } else {
      // Fallback to system click if not yet loaded
      try {
        am?.playSoundEffect(AudioManager.FX_KEY_CLICK)
      } catch (_: Exception) {}
    }
  }

  fun playKeyClick() {
    playSound(SoundEffectType.KEY_CLICK, 0.5f)
    vibrateTick()
  }

  fun playToggle(enabled: Boolean) {
    if (enabled) {
      playSound(SoundEffectType.TOGGLE_ON, 0.7f)
    } else {
      playSound(SoundEffectType.TOGGLE_OFF, 0.6f)
    }
    vibrateClick()
  }

  fun playUnlockSuccess() {
    playSound(SoundEffectType.UNLOCK_SUCCESS, 0.9f)
    vibrateSuccess()
  }

  fun playError() {
    playSound(SoundEffectType.ERROR_BUZZ, 0.8f)
    vibrateError()
  }

  fun playTrash() {
    playSound(SoundEffectType.TRASH, 0.7f)
    vibrateClick()
  }

  fun playShutter() {
    playSound(SoundEffectType.SHUTTER, 0.85f)
    vibrateClick()
  }

  fun playComplete() {
    playSound(SoundEffectType.COMPLETE_CHIME, 0.85f)
    vibrateSuccess()
  }

  // --- HAPTIC FEEDBACK ---

  fun vibrateTick() {
    if (!hapticEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(10)
      }
    } catch (_: Exception) {}
  }

  fun vibrateClick() {
    if (!hapticEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(20)
      }
    } catch (_: Exception) {}
  }

  fun vibrateSuccess() {
    if (!hapticEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 15, 60, 25)
        val amplitudes = intArrayOf(0, 120, 0, 180)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(longArrayOf(0, 20, 40, 30), -1)
      }
    } catch (_: Exception) {}
  }

  fun vibrateError() {
    if (!hapticEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 40, 50, 40)
        val amplitudes = intArrayOf(0, 200, 0, 200)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(longArrayOf(0, 40, 50, 40), -1)
      }
    } catch (_: Exception) {}
  }

  // --- AUDIO SYNTHESIS ENGINE ---

  private fun synthesizeSound(type: SoundEffectType): ShortArray {
    return when (type) {
      SoundEffectType.KEY_CLICK -> {
        // 18ms soft click, 1400Hz frequency with steep exponential decay
        val durationMs = 18
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        ShortArray(sampleCount) { i ->
          val t = i.toDouble() / SAMPLE_RATE
          val envelope = exp(-i.toDouble() / (sampleCount * 0.25))
          val wave = sin(2.0 * PI * 1400.0 * t) + 0.3 * sin(2.0 * PI * 2800.0 * t)
          (wave * envelope * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.TOGGLE_ON -> {
        // 45ms ascending cheerful chirp (780Hz -> 1300Hz)
        val durationMs = 45
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        ShortArray(sampleCount) { i ->
          val progress = i.toDouble() / sampleCount
          val freq = 780.0 + (1300.0 - 780.0) * progress
          val t = i.toDouble() / SAMPLE_RATE
          val envelope = sin(PI * progress) // smooth rise and fall
          val wave = sin(2.0 * PI * freq * t)
          (wave * envelope * 26000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.TOGGLE_OFF -> {
        // 40ms descending crisp click (1150Hz -> 620Hz)
        val durationMs = 40
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        ShortArray(sampleCount) { i ->
          val progress = i.toDouble() / sampleCount
          val freq = 1150.0 - (1150.0 - 620.0) * progress
          val t = i.toDouble() / SAMPLE_RATE
          val envelope = sin(PI * progress)
          val wave = sin(2.0 * PI * freq * t)
          (wave * envelope * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.UNLOCK_SUCCESS -> {
        // 180ms two-note ascending chime (E5: 659Hz -> B5: 987Hz)
        val durationMs = 180
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        val split = sampleCount / 2
        ShortArray(sampleCount) { i ->
          val t = i.toDouble() / SAMPLE_RATE
          val (freq, decayProgress) = if (i < split) {
            Pair(659.25, i.toDouble() / split)
          } else {
            Pair(987.77, (i - split).toDouble() / (sampleCount - split))
          }
          val envelope = exp(-decayProgress * 3.5)
          val wave = sin(2.0 * PI * freq * t) + 0.35 * sin(2.0 * PI * (freq * 2.0) * t)
          (wave * envelope * 28000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.ERROR_BUZZ -> {
        // 140ms alert double-pulse (200Hz low buzz)
        val durationMs = 140
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        ShortArray(sampleCount) { i ->
          val t = i.toDouble() / SAMPLE_RATE
          val pulseGate = if ((i % (sampleCount / 2)) < (sampleCount / 3)) 1.0 else 0.1
          val envelope = exp(-((i % (sampleCount / 2)).toDouble() / (sampleCount / 3)) * 2.0) * pulseGate
          val wave = sin(2.0 * PI * 220.0 * t) + 0.5 * sin(2.0 * PI * 440.0 * t)
          (wave * envelope * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.TRASH -> {
        // 60ms downward sweep (800Hz -> 180Hz)
        val durationMs = 60
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        ShortArray(sampleCount) { i ->
          val progress = i.toDouble() / sampleCount
          val freq = 800.0 - (800.0 - 180.0) * progress
          val t = i.toDouble() / SAMPLE_RATE
          val envelope = exp(-progress * 3.0)
          val wave = sin(2.0 * PI * freq * t)
          (wave * envelope * 22000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.SHUTTER -> {
        // 75ms double click mechanical shutter
        val durationMs = 75
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        val firstClick = sampleCount / 3
        val secondClick = (sampleCount * 2) / 3
        ShortArray(sampleCount) { i ->
          val t = i.toDouble() / SAMPLE_RATE
          val env1 = if (i in 0 until firstClick) exp(-(i.toDouble() / firstClick) * 5.0) else 0.0
          val env2 = if (i >= secondClick) exp(-((i - secondClick).toDouble() / (sampleCount - secondClick)) * 4.0) else 0.0
          val wave = sin(2.0 * PI * 1800.0 * t) + 0.4 * sin(2.0 * PI * 3200.0 * t)
          ((env1 + env2) * wave * 25000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }

      SoundEffectType.COMPLETE_CHIME -> {
        // 220ms major triad chime (C5 523Hz -> E5 659Hz -> G5 784Hz)
        val durationMs = 220
        val sampleCount = (SAMPLE_RATE * durationMs / 1000)
        val third = sampleCount / 3
        ShortArray(sampleCount) { i ->
          val t = i.toDouble() / SAMPLE_RATE
          val (freq, segmentProgress) = when {
            i < third -> Pair(523.25, i.toDouble() / third)
            i < third * 2 -> Pair(659.25, (i - third).toDouble() / third)
            else -> Pair(783.99, (i - third * 2).toDouble() / (sampleCount - third * 2))
          }
          val envelope = exp(-segmentProgress * 2.8)
          val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(2.0 * PI * (freq * 2.0) * t)
          (wave * envelope * 27000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }
    }
  }

  private fun createWavBytes(samples: ShortArray, sampleRate: Int): ByteArray {
    val dataSize = samples.size * 2
    val totalSize = 36 + dataSize
    val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

    // RIFF header
    buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
    buffer.putInt(totalSize)
    buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

    // "fmt " subchunk
    buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
    buffer.putInt(16) // Subchunk1Size (16 for PCM)
    buffer.putShort(1) // AudioFormat (1 for PCM)
    buffer.putShort(1) // NumChannels (1 for Mono)
    buffer.putInt(sampleRate)
    buffer.putInt(sampleRate * 2) // ByteRate = sampleRate * 1 * 16/8
    buffer.putShort(2) // BlockAlign = 1 * 16/8
    buffer.putShort(16) // BitsPerSample

    // "data" subchunk
    buffer.put("data".toByteArray(Charsets.US_ASCII))
    buffer.putInt(dataSize)

    for (sample in samples) {
      buffer.putShort(sample)
    }

    return buffer.array()
  }
}
