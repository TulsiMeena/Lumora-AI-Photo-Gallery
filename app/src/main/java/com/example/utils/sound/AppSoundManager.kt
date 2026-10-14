package com.example.utils.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.io.File

/**
 * High-performance, zero-latency Sound & Haptic Feedback Engine.
 *
 * Uses native [AudioManager] system effects and [ToneGenerator] for instant auditory
 * feedback without disk I/O, background decoders, or hardware codec allocation conflicts.
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

  @Volatile
  private var isInitialized = false

  private var audioManager: AudioManager? = null
  private var toneGenerator: ToneGenerator? = null
  private var vibrator: Vibrator? = null

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

      try {
        toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 65)
      } catch (e: Exception) {
        Log.w(TAG, "ToneGenerator not available on this device", e)
      }

      // Clean up any legacy disk cache from previous audio engine versions
      try {
        val legacyDir = File(appContext.cacheDir, "audio_fx")
        if (legacyDir.exists()) {
          legacyDir.deleteRecursively()
        }
      } catch (_: Exception) {}

      isInitialized = true
    }
  }

  fun playSound(type: SoundEffectType, volumeFactor: Float = 1.0f) {
    if (!soundEnabled) return

    val am = audioManager
    if (am != null && am.ringerMode == AudioManager.RINGER_MODE_SILENT) return

    try {
      when (type) {
        SoundEffectType.KEY_CLICK -> {
          playSystemEffect(AudioManager.FX_KEY_CLICK, volumeFactor)
        }
        SoundEffectType.TOGGLE_ON -> {
          if (toneGenerator != null) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
          } else {
            playSystemEffect(AudioManager.FX_KEYPRESS_SPACEBAR, volumeFactor)
          }
        }
        SoundEffectType.TOGGLE_OFF -> {
          if (toneGenerator != null) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
          } else {
            playSystemEffect(AudioManager.FX_KEYPRESS_DELETE, volumeFactor)
          }
        }
        SoundEffectType.UNLOCK_SUCCESS -> {
          if (toneGenerator != null) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
          } else {
            playSystemEffect(AudioManager.FX_KEYPRESS_STANDARD, volumeFactor)
          }
        }
        SoundEffectType.ERROR_BUZZ -> {
          if (toneGenerator != null) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 150)
          } else {
            playSystemEffect(AudioManager.FX_KEYPRESS_INVALID, volumeFactor)
          }
        }
        SoundEffectType.TRASH -> {
          if (toneGenerator != null) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
          } else {
            playSystemEffect(AudioManager.FX_KEYPRESS_DELETE, volumeFactor)
          }
        }
        SoundEffectType.SHUTTER -> {
          playSystemEffect(AudioManager.FX_KEY_CLICK, volumeFactor)
        }
        SoundEffectType.COMPLETE_CHIME -> {
          if (toneGenerator != null) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
          } else {
            playSystemEffect(AudioManager.FX_KEYPRESS_STANDARD, volumeFactor)
          }
        }
      }
    } catch (_: Exception) {
      try {
        am?.playSoundEffect(AudioManager.FX_KEY_CLICK)
      } catch (_: Exception) {}
    }
  }

  private fun playSystemEffect(effectType: Int, volumeFactor: Float) {
    val am = audioManager ?: return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        am.playSoundEffect(effectType, volumeFactor)
      } else {
        am.playSoundEffect(effectType)
      }
    } catch (_: Exception) {}
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
}
